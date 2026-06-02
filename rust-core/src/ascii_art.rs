use anyhow::{anyhow, Result};
use base64::{engine::general_purpose::URL_SAFE_NO_PAD as B64URL, Engine as _};
use serde::{Deserialize, Serialize};

use crate::kdf::{Nonce, MASTER_SEED_LEN};
use crate::route::{constant_time_eq, make_permutation, RouteContext};

const BEGIN: &str = "-----BEGIN QCS ASCII ART-----";
const END: &str = "-----END QCS ASCII ART-----";
const COMPACT_VERSION: &str = "ZEROQCS6";
const TAG_LEN: usize = 32;

// 16 znaků = 4 bity na znak. Každý bajt šifrovaného jádra se v ASCII nosiči
// uloží jako 2 znaky. Šifrované jádro je 1:1; ASCII art je textový nosič.
const DATA_PALETTE: &[u8; 16] = b".:-=+*#%@XO0&$?A";
const ART_PALETTE: &[u8] = b".:;-~=+*#%@XO0&$?A/\\|()[]{}<>";

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct Particle {
    pub glyph: String,
    pub value: u8,
    pub vector: String,
    pub phase: u64,
    pub amplitude: u8,
}

impl Particle {
    pub fn material(&self) -> Vec<u8> {
        let mut out = Vec::new();
        out.extend_from_slice(b"QCS-PARTICLE-v1|");
        out.extend_from_slice(self.glyph.as_bytes());
        out.extend_from_slice(b"|");
        out.push(self.value);
        out.extend_from_slice(b"|");
        out.extend_from_slice(self.vector.as_bytes());
        out.extend_from_slice(b"|");
        out.extend_from_slice(&self.phase.to_le_bytes());
        out.extend_from_slice(b"|");
        out.push(self.amplitude);
        out
    }

    pub fn short_hash_hex(&self) -> String {
        let h = blake3::hash(&self.material());
        to_hex(&h.as_bytes()[..8])
    }
}

#[derive(Clone, Debug)]
pub struct HiddenHeader {
    pub len: usize,
    pub core_len: usize,
    pub width: usize,
    pub height: usize,
    pub particle_hash: String,
    pub nonce: Nonce,
    pub encrypted_metadata: Vec<u8>,
    pub tag1_core: Vec<u8>,
    pub tag2_carrier: Vec<u8>,
}

impl HiddenHeader {
    fn pre_string(&self) -> String {
        format!(
            "{}|{}|{}|{}|{}|{}|{}|{}",
            COMPACT_VERSION,
            self.len,
            self.core_len,
            self.width,
            self.height,
            self.particle_hash,
            to_hex(&self.nonce),
            B64URL.encode(&self.encrypted_metadata)
        )
    }

    fn full_string(&self) -> String {
        format!(
            "{}|{}|{}",
            self.pre_string(),
            to_hex(&self.tag1_core),
            to_hex(&self.tag2_carrier)
        )
    }
}

#[derive(Clone, Debug)]
pub struct ParsedPackage {
    pub header: HiddenHeader,
    pub grid: Vec<char>,
    pub grid_text: String,
    pub body_for_seal: String,
    pub zero_seal: Vec<u8>,
}

pub fn replicate_to_ascii_art(
    core: &[u8],
    ctx: &RouteContext,
    particle: &Particle,
    header_seed: &[u8; MASTER_SEED_LEN],
    encrypted_metadata: &[u8],
) -> Result<String> {
    let data_symbols = core.len().saturating_mul(2);
    let width = choose_width(data_symbols);
    let min_height = choose_min_height(data_symbols);
    let needed_height = ceil_div(data_symbols.saturating_mul(2).saturating_add(width * 4), width);
    let height = min_height.max(needed_height).max(8);

    let mut grid = make_wave_grid(width, height, ctx, particle);
    let positions = carrier_positions(width, height, data_symbols, ctx, particle)?;
    let particle_stream = particle_xor_stream(core.len().max(1), ctx, particle);

    for (i, &byte) in core.iter().enumerate() {
        // Particle maska tady slouží jen pro vložení jádra do ASCII nosiče.
        // Není to bezpečnostní MAC tag.
        let hidden = byte ^ particle_stream[i % particle_stream.len()];
        let hi = (hidden >> 4) as usize;
        let lo = (hidden & 0x0f) as usize;
        grid[positions[i * 2]] = DATA_PALETTE[hi] as char;
        grid[positions[i * 2 + 1]] = DATA_PALETTE[lo] as char;
    }

    let grid_text = grid_to_text(&grid, width, height);
    let tag1_core = ctx.mac("TAG1_CORE_ENCRYPTED_DATA", &[core], TAG_LEN);

    let mut header = HiddenHeader {
        len: core.len(),
        core_len: core.len(),
        width,
        height,
        particle_hash: particle.short_hash_hex(),
        nonce: ctx.nonce,
        encrypted_metadata: encrypted_metadata.to_vec(),
        tag1_core,
        tag2_carrier: Vec::new(),
    };

    let header_pre = header.pre_string();
    header.tag2_carrier = ctx.mac(
        "TAG2_HIDDEN_HEADER_METADATA_CARRIER",
        &[header_pre.as_bytes(), grid_text.as_bytes()],
        TAG_LEN,
    );

    let hidden_header = mask_header(header_seed, header.full_string().as_bytes());
    let header_line = encode_art_bytes(&hidden_header);

    let mut body_for_seal = String::new();
    body_for_seal.push_str(BEGIN);
    body_for_seal.push('\n');
    body_for_seal.push_str(&header_line);
    body_for_seal.push('\n');
    body_for_seal.push_str(&grid_text);

    let zero_seal = ctx.mac("TAG3_ZERO_SEAL_WHOLE_ASCII_PACKAGE", &[body_for_seal.as_bytes()], TAG_LEN);

    let mut out = body_for_seal;
    out.push_str(&encode_art_bytes(&zero_seal));
    out.push('\n');
    out.push_str(END);
    out.push('\n');
    Ok(out)
}

pub fn parse_package(package: &str, header_seed: &[u8; MASTER_SEED_LEN]) -> Result<ParsedPackage> {
    let normalized = package.replace("\r\n", "\n").replace('\r', "\n");
    let mut lines = normalized.lines();

    let begin = lines
        .by_ref()
        .find(|line| line.trim() == BEGIN)
        .ok_or_else(|| anyhow!("Chybí začátek QCS ASCII ART balíčku"))?;
    if begin.trim() != BEGIN {
        return Err(anyhow!("Neplatný začátek balíčku"));
    }

    let header_line = lines
        .next()
        .ok_or_else(|| anyhow!("Chybí skrytá ZERO hlavička"))?
        .trim()
        .to_string();
    let header = parse_hidden_header(&header_line, header_seed)?;

    let mut grid_lines = Vec::new();
    let mut seal_line: Option<String> = None;
    let mut saw_end = false;

    for line in lines {
        let trimmed = line.trim();
        if trimmed == END {
            saw_end = true;
            break;
        }
        if trimmed.starts_with("SEAL:") {
            seal_line = Some(trimmed.to_string());
            continue;
        }
        if seal_line.is_none() {
            grid_lines.push(line.to_string());
        }
    }

    if !saw_end {
        return Err(anyhow!("Chybí konec QCS ASCII ART balíčku"));
    }

    let seal_line = if let Some(seal_line) = seal_line {
        seal_line
    } else {
        grid_lines
            .pop()
            .ok_or_else(|| anyhow!("Chybí skrytý TAG3 art seal"))?
            .trim()
            .to_string()
    };
    let zero_seal = parse_seal(&seal_line)?;

    let mut grid_text = String::new();
    let mut grid = Vec::new();
    for line in &grid_lines {
        grid_text.push_str(line);
        grid_text.push('\n');
        for ch in line.chars() {
            grid.push(ch);
        }
    }

    let mut body_for_seal = String::new();
    body_for_seal.push_str(BEGIN);
    body_for_seal.push('\n');
    body_for_seal.push_str(&header_line);
    body_for_seal.push('\n');
    body_for_seal.push_str(&grid_text);

    Ok(ParsedPackage {
        header,
        grid,
        grid_text,
        body_for_seal,
        zero_seal,
    })
}

pub fn extract_from_parsed_ascii_art(
    parsed: &ParsedPackage,
    ctx: &RouteContext,
    particle: &Particle,
) -> Result<Vec<u8>> {
    if parsed.header.particle_hash != particle.short_hash_hex() {
        return Err(anyhow!("Particle nesedí: ASCII balíček patří k jiné particle hodnotě"));
    }

    let expected_seal = ctx.mac(
        "TAG3_ZERO_SEAL_WHOLE_ASCII_PACKAGE",
        &[parsed.body_for_seal.as_bytes()],
        TAG_LEN,
    );
    if !constant_time_eq(&expected_seal, &parsed.zero_seal) {
        return Err(anyhow!("TAG3 ZERO-SEAL nesedí: soubor byl změněn, poškozen nebo je špatný klíč"));
    }

    if parsed.header.width * parsed.header.height != parsed.grid.len() {
        return Err(anyhow!("ASCII art má jinou velikost, než říká skrytá hlavička"));
    }

    let header_pre = parsed.header.pre_string();
    let expected_tag2 = ctx.mac(
        "TAG2_HIDDEN_HEADER_METADATA_CARRIER",
        &[header_pre.as_bytes(), parsed.grid_text.as_bytes()],
        TAG_LEN,
    );
    if !constant_time_eq(&expected_tag2, &parsed.header.tag2_carrier) {
        return Err(anyhow!("TAG2 nesedí: skrytá hlavička, metadata nebo ASCII nosič byly změněny"));
    }

    let data_symbols = parsed.header.len.saturating_mul(2);
    let positions = carrier_positions(parsed.header.width, parsed.header.height, data_symbols, ctx, particle)?;
    let particle_stream = particle_xor_stream(parsed.header.len.max(1), ctx, particle);

    let mut core = Vec::with_capacity(parsed.header.len);
    for i in 0..parsed.header.len {
        let hi_ch = parsed.grid[positions[i * 2]];
        let lo_ch = parsed.grid[positions[i * 2 + 1]];
        let hi = palette_index(hi_ch).ok_or_else(|| anyhow!("Neplatný znak v datové pozici ASCII artu: '{}'", hi_ch))?;
        let lo = palette_index(lo_ch).ok_or_else(|| anyhow!("Neplatný znak v datové pozici ASCII artu: '{}'", lo_ch))?;
        let hidden = (hi << 4) | lo;
        let byte = hidden ^ particle_stream[i % particle_stream.len()];
        core.push(byte);
    }

    let expected_tag1 = ctx.mac("TAG1_CORE_ENCRYPTED_DATA", &[core.as_slice()], TAG_LEN);
    if !constant_time_eq(&expected_tag1, &parsed.header.tag1_core) {
        return Err(anyhow!("TAG1 nesedí: encrypted_core byl změněn nebo poškozen"));
    }

    Ok(core)
}

fn parse_hidden_header(line: &str, header_seed: &[u8; MASTER_SEED_LEN]) -> Result<HiddenHeader> {
    let masked = if let Some(encoded) = line.strip_prefix("ZERO:") {
        B64URL
            .decode(encoded.as_bytes())
            .map_err(|_| anyhow!("Neplatná skrytá ZERO hlavička"))?
    } else {
        decode_art_bytes(line)?
    };
    let plain = mask_header(header_seed, &masked);
    let plain = String::from_utf8(plain)
        .map_err(|_| anyhow!("Skrytá ZERO hlavička nejde odemknout: špatné heslo/seedy nebo poškozený soubor"))?;

    let parts: Vec<&str> = plain.split('|').collect();
    if parts.len() != 10 || parts[0] != COMPACT_VERSION {
        return Err(anyhow!("Neplatná nebo neodemčená ZERO hlavička"));
    }

    let len = parts[1].parse::<usize>().map_err(|_| anyhow!("Neplatná délka ve skryté hlavičce"))?;
    let core_len = parts[2].parse::<usize>().map_err(|_| anyhow!("Neplatná core délka ve skryté hlavičce"))?;
    let width = parts[3].parse::<usize>().map_err(|_| anyhow!("Neplatná šířka ve skryté hlavičce"))?;
    let height = parts[4].parse::<usize>().map_err(|_| anyhow!("Neplatná výška ve skryté hlavičce"))?;
    let particle_hash = parts[5].to_string();
    let nonce_vec = from_hex(parts[6])?;
    if nonce_vec.len() != 32 {
        return Err(anyhow!("Nonce ve skryté hlavičce nemá 32 bajtů"));
    }
    let mut nonce = [0u8; 32];
    nonce.copy_from_slice(&nonce_vec);

    let encrypted_metadata = B64URL
        .decode(parts[7].as_bytes())
        .map_err(|_| anyhow!("Neplatná skrytá metadata ve hlavičce"))?;
    let tag1_core = from_hex(parts[8])?;
    let tag2_carrier = from_hex(parts[9])?;
    if tag1_core.len() != TAG_LEN || tag2_carrier.len() != TAG_LEN {
        return Err(anyhow!("TAG1/TAG2 nemají správnou délku"));
    }

    Ok(HiddenHeader {
        len,
        core_len,
        width,
        height,
        particle_hash,
        nonce,
        encrypted_metadata,
        tag1_core,
        tag2_carrier,
    })
}

fn parse_seal(line: &str) -> Result<Vec<u8>> {
    let seal = if let Some(encoded) = line.strip_prefix("SEAL:") {
        B64URL
            .decode(encoded.as_bytes())
            .map_err(|_| anyhow!("Neplatný TAG3 ZERO-SEAL"))?
    } else {
        decode_art_bytes(line)?
    };
    if seal.len() != TAG_LEN {
        return Err(anyhow!("TAG3 ZERO-SEAL nemá správnou délku"));
    }
    Ok(seal)
}

fn encode_art_bytes(bytes: &[u8]) -> String {
    let mut out = String::with_capacity(bytes.len() * 2);
    for &b in bytes {
        out.push(DATA_PALETTE[(b >> 4) as usize] as char);
        out.push(DATA_PALETTE[(b & 0x0f) as usize] as char);
    }
    out
}

fn decode_art_bytes(s: &str) -> Result<Vec<u8>> {
    let chars: Vec<char> = s.trim().chars().collect();
    if chars.len() % 2 != 0 {
        return Err(anyhow!("Skrytý art blok má lichý počet znaků"));
    }

    let mut out = Vec::with_capacity(chars.len() / 2);
    for pair in chars.chunks_exact(2) {
        let hi = palette_index(pair[0])
            .ok_or_else(|| anyhow!("Neplatný znak ve skrytém art bloku: '{}'", pair[0]))?;
        let lo = palette_index(pair[1])
            .ok_or_else(|| anyhow!("Neplatný znak ve skrytém art bloku: '{}'", pair[1]))?;
        out.push((hi << 4) | lo);
    }
    Ok(out)
}

fn mask_header(header_seed: &[u8; MASTER_SEED_LEN], data: &[u8]) -> Vec<u8> {
    let mut key = [0u8; 32];
    key.copy_from_slice(&header_seed[..32]);
    let mut hasher = blake3::Hasher::new_keyed(&key);
    hasher.update(b"QCS-HIDDEN-ZERO-HEADER-MASK-v6|");
    hasher.update(&header_seed[32..]);
    let mut reader = hasher.finalize_xof();
    let mut stream = vec![0u8; data.len().max(1)];
    reader.fill(&mut stream);
    data.iter().enumerate().map(|(i, b)| b ^ stream[i % stream.len()]).collect()
}

fn grid_to_text(grid: &[char], width: usize, height: usize) -> String {
    let mut out = String::new();
    for y in 0..height {
        let start = y * width;
        let end = start + width;
        for ch in &grid[start..end] {
            out.push(*ch);
        }
        out.push('\n');
    }
    out
}

fn choose_width(data_symbols: usize) -> usize {
    if data_symbols <= 160 {
        56
    } else if data_symbols <= 900 {
        72
    } else {
        96
    }
}

fn choose_min_height(data_symbols: usize) -> usize {
    if data_symbols <= 160 {
        16
    } else if data_symbols <= 900 {
        22
    } else {
        28
    }
}

fn ceil_div(a: usize, b: usize) -> usize {
    if b == 0 {
        0
    } else {
        (a + b - 1) / b
    }
}

fn make_wave_grid(width: usize, height: usize, ctx: &RouteContext, particle: &Particle) -> Vec<char> {
    let noise = ascii_stream(width.saturating_mul(height).max(1), "ascii-wave-background", ctx, particle);
    let mut grid = Vec::with_capacity(width * height);

    for y in 0..height {
        for x in 0..width {
            let c1 = wave_center_y(x, width, height, particle, 0.0);
            let c2 = (height as f64 - 1.0) - wave_center_y(x, width, height, particle, 1.7);
            let yf = y as f64;
            let d = (yf - c1).abs().min((yf - c2).abs());
            let n = noise[(y * width + x) % noise.len()] as usize;
            let idx = if d < 0.45 {
                20 + (n % 8)
            } else if d < 1.25 {
                14 + (n % 8)
            } else if d < 2.2 {
                8 + (n % 8)
            } else if d < 3.8 {
                3 + (n % 7)
            } else {
                n % 5
            } % ART_PALETTE.len();
            grid.push(ART_PALETTE[idx] as char);
        }
    }

    if width >= 4 && height >= 4 {
        for x in 0..width {
            grid[x] = if x % 2 == 0 { '-' } else { '~' };
            grid[(height - 1) * width + x] = if x % 2 == 0 { '~' } else { '-' };
        }
        for y in 0..height {
            grid[y * width] = '|';
            grid[y * width + width - 1] = '|';
        }
        grid[0] = '/';
        grid[width - 1] = '\\';
        grid[(height - 1) * width] = '\\';
        grid[(height - 1) * width + width - 1] = '/';
    }

    grid
}

fn wave_center_y(x: usize, width: usize, height: usize, particle: &Particle, offset: f64) -> f64 {
    let width = width.max(1) as f64;
    let height = height.max(1) as f64;
    let amp = (1.0 + (particle.amplitude as f64 / 255.0) * (height * 0.35)).min(height * 0.38);
    let phase = (particle.phase as f64 / width) * std::f64::consts::TAU;
    let value_phase = (particle.value as f64 / 255.0) * std::f64::consts::TAU;
    let t = (x as f64 / width) * std::f64::consts::TAU * 2.0 + phase + value_phase + offset;
    (height - 1.0) * 0.5 + t.sin() * amp
}

fn carrier_positions(width: usize, height: usize, data_symbols: usize, ctx: &RouteContext, particle: &Particle) -> Result<Vec<usize>> {
    let total_cells = width.saturating_mul(height);
    if data_symbols > total_cells {
        return Err(anyhow!("ASCII nosič je malý pro datové symboly"));
    }
    if data_symbols == 0 {
        return Ok(Vec::new());
    }

    let seed = ascii_stream(total_cells.saturating_mul(16).max(16), "ascii-carrier-positions", ctx, particle);
    let mut scored: Vec<(u64, usize)> = Vec::with_capacity(total_cells);

    for idx in 0..total_cells {
        let x = idx % width;
        let y = idx / width;
        let border_penalty = if x == 0 || y == 0 || x + 1 == width || y + 1 == height { 50_000 } else { 0 };
        let c1 = wave_center_y(x, width, height, particle, 0.0);
        let c2 = (height as f64 - 1.0) - wave_center_y(x, width, height, particle, 1.7);
        let d = ((y as f64 - c1).abs().min((y as f64 - c2).abs()) * 1000.0) as u64;
        let s0 = seed[(idx * 8) % seed.len()] as u64;
        let s1 = seed[(idx * 8 + 1) % seed.len()] as u64;
        let noise = ((s0 << 8) | s1) % 997;
        scored.push((border_penalty + d.saturating_mul(1000) + noise, idx));
    }

    scored.sort_by_key(|&(score, idx)| (score, idx));
    let mut chosen: Vec<usize> = scored.into_iter().take(data_symbols).map(|(_, idx)| idx).collect();

    let order_seed = ascii_stream(chosen.len().saturating_mul(8).max(8), "ascii-carrier-order", ctx, particle);
    let order = make_permutation(chosen.len(), &order_seed);
    let old = chosen.clone();
    for (new_pos, old_pos) in order.into_iter().enumerate() {
        chosen[new_pos] = old[old_pos];
    }

    Ok(chosen)
}

fn particle_xor_stream(len: usize, ctx: &RouteContext, particle: &Particle) -> Vec<u8> {
    ascii_stream(len.max(1), "particle-xor-hidden-glyph", ctx, particle)
}

fn ascii_stream(len: usize, label: &str, ctx: &RouteContext, particle: &Particle) -> Vec<u8> {
    let mut key = [0u8; 32];
    key.copy_from_slice(&ctx.route_seed);

    let mut hasher = blake3::Hasher::new_keyed(&key);
    hasher.update(b"QCS-ASCII-REPLICATOR-v6|");
    hasher.update(label.as_bytes());
    hasher.update(b"|");
    hasher.update(&ctx.nonce);
    hasher.update(b"|");
    hasher.update(&ctx.hidden_iv);
    hasher.update(b"|");
    hasher.update(&particle.material());
    hasher.update(b"|");
    hasher.update(&ctx.master_seed);

    let mut reader = hasher.finalize_xof();
    let mut out = vec![0u8; len];
    reader.fill(&mut out);
    out
}

fn palette_index(ch: char) -> Option<u8> {
    let b = ch as u32;
    if b > 127 {
        return None;
    }
    DATA_PALETTE
        .iter()
        .position(|&x| x == b as u8)
        .map(|x| x as u8)
}

fn to_hex(bytes: &[u8]) -> String {
    const HEX: &[u8; 16] = b"0123456789abcdef";
    let mut out = String::with_capacity(bytes.len() * 2);
    for &b in bytes {
        out.push(HEX[(b >> 4) as usize] as char);
        out.push(HEX[(b & 0x0f) as usize] as char);
    }
    out
}

fn from_hex(s: &str) -> Result<Vec<u8>> {
    if s.len() % 2 != 0 {
        return Err(anyhow!("Hex řetězec má lichou délku"));
    }
    let mut out = Vec::with_capacity(s.len() / 2);
    let bytes = s.as_bytes();
    for i in (0..bytes.len()).step_by(2) {
        let hi = hex_val(bytes[i])?;
        let lo = hex_val(bytes[i + 1])?;
        out.push((hi << 4) | lo);
    }
    Ok(out)
}

fn hex_val(b: u8) -> Result<u8> {
    match b {
        b'0'..=b'9' => Ok(b - b'0'),
        b'a'..=b'f' => Ok(b - b'a' + 10),
        b'A'..=b'F' => Ok(b - b'A' + 10),
        _ => Err(anyhow!("Neplatný hex znak")),
    }
}
