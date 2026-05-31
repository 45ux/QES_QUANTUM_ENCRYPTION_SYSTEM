use anyhow::{anyhow, Result};
use serde::Serialize;
use std::time::Instant;

use crate::cipher::{decrypt_from_ascii_art, encrypt_to_ascii_art, QcsParams, SelfTestCase};
use crate::kdf::MASTER_SEED_LEN;

pub const CARRIER_BEGIN: &[u8] = b"\n--BEGIN ZERO QCS CARRIER V1--\n";
pub const CARRIER_END: &[u8] = b"\n--END ZERO QCS CARRIER V1--\n";
const CARRIER_META_V2: &[u8] = b"QES-CARRIER-META-V2\n";

#[derive(Debug, Serialize)]
pub struct CarrierEncryptionResult {
    pub ok: bool,
    pub carrier_b64: String,
    pub cover_len: usize,
    pub payload_len: usize,
    pub carrier_len: usize,
    pub public_hash_hex: String,
    pub keyed_mac_hex: String,
    pub elapsed_ms: u128,
    pub speed_kib_s: f64,
    pub speed_mib_s: f64,
}

#[derive(Debug, Serialize)]
pub struct CarrierDecryptionInfo {
    pub cover_len: usize,
    pub payload_len: usize,
    pub carrier_len: usize,
    pub public_hash_hex: String,
    pub keyed_mac_hex: String,
}

/// V8.1 carrier už není jen „přilepený payload“ bez kontroly.
/// Do carrier bloku se uloží malé autentizační metadata V2 s keyed MAC.
/// MAC pokrývá cover část i ASCII payload, takže bit flip kdekoliv ve finálním
/// carrier souboru musí být odmítnut ještě před dešifrováním payloadu.
pub fn encrypt_to_carrier_file(plain: &[u8], cover: &[u8], params: &QcsParams) -> Result<(Vec<u8>, CarrierDecryptionInfo)> {
    let ascii_art = encrypt_to_ascii_art(plain, params)?;
    let mut payload = ascii_art.into_bytes();
    if !payload.ends_with(b"\n") { payload.push(b'\n'); }

    let carrier_mac = carrier_payload_mac_hex(cover, &payload, cover.len(), payload.len(), params)?;
    let meta = format!(
        "QES-CARRIER-META-V2\ncover_len={}\npayload_len={}\nmac={}\n\n",
        cover.len(),
        payload.len(),
        carrier_mac
    );

    let mut carrier = Vec::with_capacity(cover.len() + CARRIER_BEGIN.len() + meta.len() + payload.len() + CARRIER_END.len());
    carrier.extend_from_slice(cover);
    carrier.extend_from_slice(CARRIER_BEGIN);
    carrier.extend_from_slice(meta.as_bytes());
    carrier.extend_from_slice(&payload);
    carrier.extend_from_slice(CARRIER_END);

    let info = carrier_info(&carrier, cover.len(), payload.len(), params)?;
    Ok((carrier, info))
}

pub fn decrypt_from_carrier_file(carrier: &[u8], params: &QcsParams) -> Result<(Vec<u8>, CarrierDecryptionInfo)> {
    let parsed = extract_ascii_payload(carrier)?;
    let cover = &carrier[..parsed.cover_len];

    let expected_mac = carrier_payload_mac_hex(cover, parsed.payload, parsed.cover_len, parsed.payload.len(), params)?;
    if normalize_hex(&parsed.mac_hex) != expected_mac {
        return Err(anyhow!("QES carrier byl změněn nebo nesedí klíč: carrier MAC nesedí"));
    }

    let payload_str = std::str::from_utf8(parsed.payload)
        .map_err(|_| anyhow!("QCS payload v nosiči není platný UTF-8 ASCII art"))?;
    let data = decrypt_from_ascii_art(payload_str, params)?;
    let info = carrier_info(carrier, parsed.cover_len, parsed.payload.len(), params)?;
    Ok((data, info))
}

pub fn verify_hashes(data: &[u8], params: &QcsParams, expected_public_hash: Option<&str>, expected_keyed_mac: Option<&str>) -> Result<serde_json::Value> {
    let public_hash = public_hash_hex(data);
    let keyed_mac = keyed_mac_hex(data, params)?;
    let public_match = expected_public_hash.map(|v| normalize_hex(v) == public_hash);
    let mac_match = expected_keyed_mac.map(|v| normalize_hex(v) == keyed_mac);
    Ok(serde_json::json!({
        "ok": true,
        "public_hash_hex": public_hash,
        "keyed_mac_hex": keyed_mac,
        "public_hash_match": public_match,
        "keyed_mac_match": mac_match,
        "note": "Public hash ukáže změnu souboru bez klíče. Keyed MAC ověří změnu souboru s heslem a seedy."
    }))
}

pub fn carrier_info(carrier: &[u8], cover_len: usize, payload_len: usize, params: &QcsParams) -> Result<CarrierDecryptionInfo> {
    Ok(CarrierDecryptionInfo {
        cover_len,
        payload_len,
        carrier_len: carrier.len(),
        public_hash_hex: public_hash_hex(carrier),
        keyed_mac_hex: keyed_mac_hex(carrier, params)?,
    })
}

#[derive(Debug)]
pub struct ParsedCarrierPayload<'a> {
    pub cover_len: usize,
    pub payload: &'a [u8],
    pub mac_hex: String,
}

pub fn extract_ascii_payload(carrier: &[u8]) -> Result<ParsedCarrierPayload<'_>> {
    let start = find_last(carrier, CARRIER_BEGIN)
        .ok_or_else(|| anyhow!("V souboru není ZERO QCS carrier marker"))?;
    let meta_start = start + CARRIER_BEGIN.len();
    let rel_end = find_first(&carrier[meta_start..], CARRIER_END)
        .ok_or_else(|| anyhow!("V souboru chybí konec ZERO QCS carrier markeru"))?;
    let body_end = meta_start + rel_end;
    let body = &carrier[meta_start..body_end];

    if !body.starts_with(CARRIER_META_V2) {
        return Err(anyhow!("Carrier nemá autentizační metadata QES-CARRIER-META-V2; starý neověřitelný carrier je odmítnut"));
    }

    let header_end = find_first(body, b"\n\n")
        .ok_or_else(|| anyhow!("Carrier metadata nejsou ukončena prázdným řádkem"))?;
    let header = std::str::from_utf8(&body[..header_end])
        .map_err(|_| anyhow!("Carrier metadata nejsou platné UTF-8"))?;
    let payload = &body[header_end + 2..];

    let mut cover_len_meta: Option<usize> = None;
    let mut payload_len_meta: Option<usize> = None;
    let mut mac_hex: Option<String> = None;

    for line in header.lines().skip(1) {
        if let Some(v) = line.strip_prefix("cover_len=") {
            cover_len_meta = Some(v.parse::<usize>().map_err(|_| anyhow!("Carrier metadata cover_len nejsou číslo"))?);
        } else if let Some(v) = line.strip_prefix("payload_len=") {
            payload_len_meta = Some(v.parse::<usize>().map_err(|_| anyhow!("Carrier metadata payload_len nejsou číslo"))?);
        } else if let Some(v) = line.strip_prefix("mac=") {
            mac_hex = Some(normalize_hex(v));
        }
    }

    let cover_len_meta = cover_len_meta.ok_or_else(|| anyhow!("Carrier metadata neobsahují cover_len"))?;
    let payload_len_meta = payload_len_meta.ok_or_else(|| anyhow!("Carrier metadata neobsahují payload_len"))?;
    let mac_hex = mac_hex.ok_or_else(|| anyhow!("Carrier metadata neobsahují MAC"))?;

    if cover_len_meta != start {
        return Err(anyhow!("Carrier cover_len nesedí: metadata={cover_len_meta}, skutečnost={start}"));
    }
    if payload_len_meta != payload.len() {
        return Err(anyhow!("Carrier payload_len nesedí: metadata={payload_len_meta}, skutečnost={}", payload.len()));
    }
    if mac_hex.len() != 64 || !mac_hex.chars().all(|c| c.is_ascii_hexdigit()) {
        return Err(anyhow!("Carrier MAC nemá platný 256bit HEX formát"));
    }

    Ok(ParsedCarrierPayload { cover_len: start, payload, mac_hex })
}

fn carrier_payload_mac_hex(cover: &[u8], payload: &[u8], cover_len: usize, payload_len: usize, params: &QcsParams) -> Result<String> {
    let seed = params.header_seed()?;
    let mut key = [0u8; 32];
    key.copy_from_slice(&seed[..32]);
    let mut hasher = blake3::Hasher::new_keyed(&key);
    hasher.update(b"QES-CARRIER-PAYLOAD-MAC-v2|");
    hasher.update(&seed[32..]);
    hasher.update(&(cover_len as u64).to_le_bytes());
    hasher.update(&(payload_len as u64).to_le_bytes());
    hasher.update(&(cover.len() as u64).to_le_bytes());
    hasher.update(cover);
    hasher.update(&(payload.len() as u64).to_le_bytes());
    hasher.update(payload);
    Ok(to_hex(hasher.finalize().as_bytes()))
}

pub fn public_hash_hex(data: &[u8]) -> String { blake3_hex(data) }

pub fn keyed_mac_hex(data: &[u8], params: &QcsParams) -> Result<String> {
    let seed = params.header_seed()?;
    Ok(keyed_mac_with_seed(data, &seed))
}

fn keyed_mac_with_seed(data: &[u8], seed: &[u8; MASTER_SEED_LEN]) -> String {
    let mut key = [0u8; 32];
    key.copy_from_slice(&seed[..32]);
    let mut hasher = blake3::Hasher::new_keyed(&key);
    hasher.update(b"ZERO-QCS-FILE-MAC-v1|");
    hasher.update(&seed[32..]);
    hasher.update(b"|");
    hasher.update(data);
    to_hex(hasher.finalize().as_bytes())
}

fn find_first(haystack: &[u8], needle: &[u8]) -> Option<usize> {
    if needle.is_empty() || haystack.len() < needle.len() { return None; }
    haystack.windows(needle.len()).position(|w| w == needle)
}

fn find_last(haystack: &[u8], needle: &[u8]) -> Option<usize> {
    if needle.is_empty() || haystack.len() < needle.len() { return None; }
    haystack.windows(needle.len()).rposition(|w| w == needle)
}

fn normalize_hex(s: &str) -> String {
    s.chars().filter(|c| !c.is_whitespace()).flat_map(|c| c.to_lowercase()).collect()
}

fn blake3_hex(data: &[u8]) -> String { to_hex(blake3::hash(data).as_bytes()) }

fn to_hex(bytes: &[u8]) -> String {
    const HEX: &[u8; 16] = b"0123456789abcdef";
    let mut out = String::with_capacity(bytes.len() * 2);
    for &b in bytes {
        out.push(HEX[(b >> 4) as usize] as char);
        out.push(HEX[(b & 0x0f) as usize] as char);
    }
    out
}

fn bit_flip_copy(data: &[u8]) -> Vec<u8> {
    let mut out = data.to_vec();
    if let Some(first) = out.first_mut() { *first ^= 1; }
    out
}

pub fn run_carrier_tests() -> Vec<SelfTestCase> {
    use crate::ascii_art::Particle;
    let params = QcsParams {
        password: "tajne-heslo".to_string(),
        seed1: "seed-111".to_string(),
        seed2: "seed-222".to_string(),
        seed3: "seed-333".to_string(),
        seed4: "seed-444".to_string(),
        particle: Particle { glyph: "X".to_string(), value: 77, vector: "vektor".to_string(), phase: 13, amplitude: 9 },
    };
    let mut tests = Vec::new();
    let data: Vec<u8> = (0..4096).map(|i| ((i * 37 + 19) % 256) as u8).collect();
    let cover = b"FAKE-PNG-OR-VIDEO-COVER-BYTES";

    let started = Instant::now();
    match encrypt_to_carrier_file(&data, cover, &params).and_then(|(carrier, _)| decrypt_from_carrier_file(&carrier, &params).map(|(dec, _)| (carrier, dec))) {
        Ok((carrier, dec)) => {
            tests.push(SelfTestCase { name: "experimentální carrier soubor: roundtrip".to_string(), ok: dec == data, detail: format!("carrier={}B output={}B time={}ms", carrier.len(), dec.len(), started.elapsed().as_millis()) });
            let changed = bit_flip_copy(&carrier);
            let tamper_rejected = decrypt_from_carrier_file(&changed, &params).is_err();
            tests.push(SelfTestCase { name: "experimentální carrier soubor: bit flip odmítnut MAC/TAG kontrolou".to_string(), ok: tamper_rejected, detail: "Změna jediného bajtu ve finálním souboru nesmí projít dešifrováním.".to_string() });
            let public_changed = public_hash_hex(&carrier) != public_hash_hex(&changed);
            let mac_changed = keyed_mac_hex(&carrier, &params).ok() != keyed_mac_hex(&changed, &params).ok();
            tests.push(SelfTestCase { name: "hash/MAC finálního souboru: změna souboru změní otisky".to_string(), ok: public_changed && mac_changed, detail: format!("public_changed={public_changed} keyed_mac_changed={mac_changed}") });
        }
        Err(e) => tests.push(SelfTestCase { name: "experimentální carrier soubor: roundtrip".to_string(), ok: false, detail: e.to_string() }),
    }
    tests
}
