use anyhow::{anyhow, Result};
use serde::{Deserialize, Serialize};
use std::time::{Instant, SystemTime, UNIX_EPOCH};

use crate::ascii_art::{extract_from_parsed_ascii_art, parse_package, replicate_to_ascii_art, Particle};
use crate::kdf::{header_seed, master_seed, random_nonce, Nonce, Seeds, MASTER_SEED_LEN};
use crate::route::{apply_permutation, constant_time_eq, invert_permutation, RouteContext};

const ROUNDS: usize = 30;
const FRAME_IMAGE_WIDTH: usize = 32;
const FRAME_BYTES_LIMIT: usize = 1024;
const TAG_LEN: usize = 32;

#[derive(Clone, Debug, Serialize)]
pub struct SelfTestCase {
    pub name: String,
    pub ok: bool,
    pub detail: String,
}

#[derive(Clone, Debug, Serialize)]
pub struct DiagnosticVar {
    pub name: String,
    pub value: String,
    pub note: String,
}

#[derive(Clone, Debug, Serialize)]
pub struct DiagnosticFrame {
    pub index: usize,
    pub stage: String,
    pub round: Option<usize>,
    pub len: usize,
    pub checksum: String,
    pub preview_hex: String,
    pub variables: Vec<DiagnosticVar>,
    pub ascii_image: String,
    pub download_name: String,
}

#[derive(Clone, Debug, Serialize)]
pub struct DiagnosticReport {
    pub ok: bool,
    pub elapsed_ms: u128,
    pub input_len: usize,
    pub encrypted_core_len: usize,
    pub ascii_art_chars: usize,
    pub frame_count: usize,
    pub guide: Vec<String>,
    pub variables: Vec<DiagnosticVar>,
    pub frames: Vec<DiagnosticFrame>,
    pub tests: Vec<SelfTestCase>,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct QcsParams {
    pub password: String,
    pub seed1: String,
    pub seed2: String,
    pub seed3: String,
    pub seed4: String,
    pub particle: Particle,
}

impl QcsParams {
    pub fn seeds(&self) -> Seeds {
        Seeds {
            seed1: self.seed1.clone(),
            seed2: self.seed2.clone(),
            seed3: self.seed3.clone(),
            seed4: self.seed4.clone(),
        }
    }

    pub fn header_seed(&self) -> Result<[u8; MASTER_SEED_LEN]> {
        header_seed(&self.password, &self.seeds())
    }

    pub fn route_context_for_nonce(&self, nonce: Nonce) -> Result<RouteContext> {
        let master = master_seed(&self.password, &self.seeds(), &nonce)?;
        Ok(RouteContext::new(master, nonce))
    }

    pub fn fresh_route_context(&self) -> Result<RouteContext> {
        self.route_context_for_nonce(random_nonce()?)
    }

    /// Deterministický kontext jen pro interní testy/diagnostiku kroků.
    pub fn test_route_context(&self) -> Result<RouteContext> {
        let mut nonce = [0u8; 32];
        let h = blake3::hash(b"QCS deterministic test nonce v6");
        nonce.copy_from_slice(h.as_bytes());
        self.route_context_for_nonce(nonce)
    }
}

pub fn encrypt_to_ascii_art(data: &[u8], params: &QcsParams) -> Result<String> {
    let header = params.header_seed()?;
    let ctx = params.fresh_route_context()?;
    let encrypted_core = encrypt_core(data, &ctx);
    let metadata_plain = build_metadata(data, &encrypted_core);
    let encrypted_metadata = ctx.mask_metadata(&metadata_plain);
    replicate_to_ascii_art(&encrypted_core, &ctx, &params.particle, &header, &encrypted_metadata)
}

pub fn decrypt_from_ascii_art(ascii_art_package: &str, params: &QcsParams) -> Result<Vec<u8>> {
    let header = params.header_seed()?;
    let parsed = parse_package(ascii_art_package, &header)?;
    let ctx = params.route_context_for_nonce(parsed.header.nonce)?;

    let encrypted_core = extract_from_parsed_ascii_art(&parsed, &ctx, &params.particle)?;
    let metadata_plain = ctx.mask_metadata(&parsed.header.encrypted_metadata);
    let plaintext = decrypt_core(&encrypted_core, &ctx);
    verify_metadata(&metadata_plain, &plaintext, &encrypted_core)?;
    Ok(plaintext)
}

pub fn encrypt_core(data: &[u8], ctx: &RouteContext) -> Vec<u8> {
    let mut out = data.to_vec();

    // 1) PRE-CORE XOR OTP-like pad:
    // vstup nejde do difuze/permutace/superpozice nahý.
    ctx.apply_pre_core_otp(&mut out);

    // 2) QES core kola.
    for round in 0..ROUNDS {
        apply_round_forward(&mut out, ctx, round);
    }

    // 3) TAG1 a TAG2 jako skryté XOF XOR masky.
    // Nejsou viditelné v souboru jako řádky; jsou to reverzibilní maskovací vrstvy.
    ctx.apply_tag1_hidden_mask(&mut out);
    ctx.apply_tag2_hidden_mask(&mut out);

    out
}

pub fn decrypt_core(data: &[u8], ctx: &RouteContext) -> Vec<u8> {
    let mut out = data.to_vec();

    // 1) Odstranit TAG2 a TAG1 v opačném pořadí.
    ctx.apply_tag2_hidden_mask(&mut out);
    ctx.apply_tag1_hidden_mask(&mut out);

    // 2) Vrátit QES core kola.
    for round in (0..ROUNDS).rev() {
        apply_round_inverse(&mut out, ctx, round);
    }

    // 3) PRE-CORE pad se odstraňuje až úplně nakonec.
    ctx.apply_pre_core_otp(&mut out);

    out
}

pub fn diagnose_pipeline(data: &[u8], params: &QcsParams, include_frames: bool) -> Result<DiagnosticReport> {
    let started = Instant::now();
    let header = params.header_seed()?;
    let ctx = params.fresh_route_context()?;
    let mut frames = Vec::new();
    let mut working = data.to_vec();

    let variables = vec![
        DiagnosticVar { name: "input_len".to_string(), value: format!("{} B", data.len()), note: "Délka původního vstupu.".to_string() },
        DiagnosticVar { name: "header_key".to_string(), value: fingerprint(&header), note: "Otisk klíče pro skrytou ZERO hlavičku; samotný klíč se nezobrazuje.".to_string() },
        DiagnosticVar { name: "nonce".to_string(), value: fingerprint(&ctx.nonce), note: "Nonce je v balíčku schované; diagnostika ukazuje jen otisk.".to_string() },
        DiagnosticVar { name: "hidden_iv".to_string(), value: fingerprint(&ctx.hidden_iv), note: "Otisk tajného hidden_iv odvozeného z hesla, seedů a nonce.".to_string() },
        DiagnosticVar { name: "route_seed".to_string(), value: fingerprint(&ctx.route_seed), note: "Otisk tajného základu pro permutace, proudy a skoky.".to_string() },
        DiagnosticVar { name: "particle_hash".to_string(), value: params.particle.short_hash_hex(), note: "Krátký otisk particle: glyph + value + vector + phase + amplitude.".to_string() },
        DiagnosticVar { name: "rounds".to_string(), value: ROUNDS.to_string(), note: "Počet kol QCS core.".to_string() },
    ];

    if include_frames {
        push_frame(&mut frames, "00_INPUT", None, &working, vec![
            var("meaning", "původní data", "Toto je stav před prvním skokem."),
        ]);
    }

    ctx.apply_pre_core_otp(&mut working);
    if include_frames {
        push_frame(&mut frames, "01_PRE_CORE_XOR_OTP_PAD", None, &working, vec![
            var("meaning", "vstup zamaskovaný OTP-like XOF padem", "Data už před difuzí/permutací/superpozicí nejsou nahá."),
            var("pad_source", "QES.V3.PRE_CORE_XOR_OTP_PAD", "Pad je dopočítaný z odděleného subklíče, hidden_iv a nonce."),
        ]);
    }

    for round in 0..ROUNDS {
        let len = working.len();
        let p = ctx.round_permutation(len, round, "perm");
        let perm_preview = p.iter().take(12).map(|v| v.to_string()).collect::<Vec<_>>().join(",");
        apply_permutation(&mut working, &p);
        if include_frames { push_frame(&mut frames, "PERMUTACE", Some(round), &working, vec![var("permutation_first", &perm_preview, "Prvních několik indexů tajného přeházení.")]); }

        let len = working.len();
        let diff_a = ctx.round_bytes(round, "diff-a", len.max(1));
        diffuse_forward(&mut working, &diff_a);
        if include_frames { push_frame(&mut frames, "DIFUZE_A", Some(round), &working, vec![var("stream_fingerprint", &fingerprint(&diff_a), "Otisk proudu pro první difuzi kola; proud se nezobrazuje.")]); }

        let len = working.len();
        let super_stream = ctx.round_bytes(round, "super", len.saturating_mul(2).max(2));
        superpose_forward(&mut working, &super_stream);
        if include_frames { push_frame(&mut frames, "SUPERPOZICE", Some(round), &working, vec![var("stream_fingerprint", &fingerprint(&super_stream), "Otisk proudu pro přičtení a rotaci bitů; proud se nezobrazuje.")]); }

        let len = working.len();
        let diff_b = ctx.round_bytes(round, "diff-b", len.max(1));
        diffuse_forward(&mut working, &diff_b);
        if include_frames { push_frame(&mut frames, "DIFUZE_B", Some(round), &working, vec![var("stream_fingerprint", &fingerprint(&diff_b), "Otisk proudu pro druhou difuzi kola; proud se nezobrazuje.")]); }
    }

    ctx.apply_tag1_hidden_mask(&mut working);
    if include_frames {
        push_frame(&mut frames, "TAG1_HIDDEN_XOF_MASK", None, &working, vec![
            var("meaning", "TAG1 skrytá XOF XOR maska", "První interní tagová maska není viditelný řádek v souboru."),
            var("zero_model", "ZERO_UNDER_TAG1", "ZERO bude navázané pod TAG1 jako skrytá vnitřní visačka."),
        ]);
    }

    ctx.apply_tag2_hidden_mask(&mut working);
    if include_frames {
        push_frame(&mut frames, "TAG2_HIDDEN_XOF_MASK", None, &working, vec![
            var("meaning", "TAG2 skrytá XOF XOR maska", "Druhá interní tagová maska leží nad TAG1 vrstvou."),
            var("tag3_model", "ASCII_WAVE_SEAL", "TAG3 je vrstva nad ASCII artem, ne další změna core dat."),
        ]);
    }

    let encrypted_core = working;
    let metadata_plain = build_metadata(data, &encrypted_core);
    let encrypted_metadata = ctx.mask_metadata(&metadata_plain);
    let ascii_art = replicate_to_ascii_art(&encrypted_core, &ctx, &params.particle, &header, &encrypted_metadata)?;

    if include_frames {
        let tag1 = ctx.mac("TAG1_CORE_ENCRYPTED_DATA", &[encrypted_core.as_slice()], TAG_LEN);
        let tag2_note = "Nový model: TAG1/TAG2 jsou skryté XOF XOR masky; ZERO_UNDER_TAG1 je maskovaná metadata visačka.";
        push_frame(&mut frames, "ENCRYPTED_CORE_1_1", None, &encrypted_core, vec![
            var("core_len", &format!("{} B", encrypted_core.len()), "Tady se ověřuje 1:1 délka vůči vstupu."),
            var("TAG1_CORE", &fingerprint(&tag1), "Otisk skutečné MAC pečetě přes encrypted_core."),
            var("TAG2_CARRIER", "XOF maska + metadata bind", tag2_note),
        ]);
        frames.push(DiagnosticFrame {
            index: frames.len(),
            stage: "ASCII_ART_CARRIER_TAG3_SEAL".to_string(),
            round: None,
            len: ascii_art.len(),
            checksum: blake3_hex(ascii_art.as_bytes()),
            preview_hex: preview_hex(ascii_art.as_bytes(), 64),
            variables: vec![
                var("ascii_art_chars", &ascii_art.chars().count().to_string(), "Počet znaků textového nosiče. Nosič je delší než binární 1:1 core."),
                var("TAG3_ASCII_SEAL", "legacy seal / budoucí hidden art slot", "TAG3 chrání ASCII art; další formátový krok ho schová bez viditelného řádku."),
                var("particle_hash", &params.particle.short_hash_hex(), "Particle řídí trasu nosiče."),
            ],
            ascii_image: ascii_art.clone(),
            download_name: format!("qcs_jump_{:03}_ascii_art_nosic.txt", frames.len()),
        });
    }

    let decrypted = decrypt_core(&encrypted_core, &ctx);
    let full_roundtrip_ok = decrypted.as_slice() == data;
    let ascii_back = decrypt_from_ascii_art(&ascii_art, params)?;
    let ascii_roundtrip_ok = ascii_back.as_slice() == data;
    let encrypted_core_len = encrypted_core.len();
    let ascii_art_chars = ascii_art.chars().count();

    let mut tests = run_self_tests();
    tests.insert(0, SelfTestCase {
        name: "diagnostika aktuálního vstupu: core 1:1".to_string(),
        ok: encrypted_core_len == data.len(),
        detail: format!("input={}B encrypted_core={}B", data.len(), encrypted_core_len),
    });
    tests.insert(1, SelfTestCase {
        name: "diagnostika aktuálního vstupu: core roundtrip".to_string(),
        ok: full_roundtrip_ok,
        detail: format!("decrypt_core(encrypt_core(data)) == data → {}", full_roundtrip_ok),
    });
    tests.insert(2, SelfTestCase {
        name: "diagnostika aktuálního vstupu: ASCII roundtrip + TAG1/TAG2/TAG3".to_string(),
        ok: ascii_roundtrip_ok,
        detail: format!("ASCII nosič → ověřit pečetě → core → plaintext; ascii_art_chars={}", ascii_art_chars),
    });

    let ok = tests.iter().all(|t| t.ok);
    Ok(DiagnosticReport {
        ok,
        elapsed_ms: started.elapsed().as_millis(),
        input_len: data.len(),
        encrypted_core_len,
        ascii_art_chars,
        frame_count: frames.len(),
        guide: diagnostic_guide(),
        variables,
        frames,
        tests,
    })
}

fn apply_round_forward(out: &mut Vec<u8>, ctx: &RouteContext, round: usize) {
    let len = out.len();
    let p = ctx.round_permutation(len, round, "perm");
    apply_permutation(out, &p);

    let len = out.len();
    let diff_a = ctx.round_bytes(round, "diff-a", len.max(1));
    diffuse_forward(out, &diff_a);

    let len = out.len();
    let super_stream = ctx.round_bytes(round, "super", len.saturating_mul(2).max(2));
    superpose_forward(out, &super_stream);

    let len = out.len();
    let diff_b = ctx.round_bytes(round, "diff-b", len.max(1));
    diffuse_forward(out, &diff_b);
}

fn apply_round_inverse(out: &mut Vec<u8>, ctx: &RouteContext, round: usize) {
    let len = out.len();
    let diff_b = ctx.round_bytes(round, "diff-b", len.max(1));
    diffuse_inverse(out, &diff_b);

    let len = out.len();
    let super_stream = ctx.round_bytes(round, "super", len.saturating_mul(2).max(2));
    superpose_inverse(out, &super_stream);

    let len = out.len();
    let diff_a = ctx.round_bytes(round, "diff-a", len.max(1));
    diffuse_inverse(out, &diff_a);

    let len = out.len();
    let p = ctx.round_permutation(len, round, "perm");
    let inv = invert_permutation(&p);
    apply_permutation(out, &inv);
}

fn diffuse_forward(data: &mut [u8], stream: &[u8]) {
    if data.is_empty() || stream.is_empty() {
        return;
    }
    let mut prev = stream[0];
    for i in 0..data.len() {
        let c = data[i].wrapping_add(prev).wrapping_add(stream[i % stream.len()]);
        data[i] = c;
        prev = c;
    }
}

fn diffuse_inverse(data: &mut [u8], stream: &[u8]) {
    if data.is_empty() || stream.is_empty() {
        return;
    }
    let mut prev_cipher = stream[0];
    for i in 0..data.len() {
        let c = data[i];
        let p = c.wrapping_sub(prev_cipher).wrapping_sub(stream[i % stream.len()]);
        data[i] = p;
        prev_cipher = c;
    }
}

fn superpose_forward(data: &mut [u8], stream: &[u8]) {
    if data.is_empty() || stream.is_empty() {
        return;
    }
    for i in 0..data.len() {
        let k = stream[(i * 2) % stream.len()];
        let r = stream[(i * 2 + 1) % stream.len()] % 8;
        data[i] = data[i].wrapping_add(k).rotate_left(r as u32);
    }
}

fn superpose_inverse(data: &mut [u8], stream: &[u8]) {
    if data.is_empty() || stream.is_empty() {
        return;
    }
    for i in 0..data.len() {
        let k = stream[(i * 2) % stream.len()];
        let r = stream[(i * 2 + 1) % stream.len()] % 8;
        data[i] = data[i].rotate_right(r as u32).wrapping_sub(k);
    }
}

fn build_metadata(plaintext: &[u8], encrypted_core: &[u8]) -> Vec<u8> {
    let created = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_secs())
        .unwrap_or(0);

    // METAQES7 = nový vnitřní model ZERO_UNDER_TAG1.
    // ZERO není veřejný řádek s tajemstvím. Je to skrytá metadata visačka
    // maskovaná přes ctx.mask_metadata() a navázaná na TAG1/TAG2 v core toku.
    format!(
        "METAQES7|zero_model=ZERO_UNDER_TAG1|pre_core_otp=on|tag1_xof_mask=on|tag2_xof_mask=on|tag3_ascii_seal=on|plain_len={}|plain_hash={}|core_len={}|core_hash={}|counter=0|created_unix={}|carrier=ascii_art|nonce=hidden|visible_tags=legacy_compat",
        plaintext.len(),
        blake3_hex(plaintext),
        encrypted_core.len(),
        blake3_hex(encrypted_core),
        created
    ).into_bytes()
}

fn verify_metadata(metadata_plain: &[u8], plaintext: &[u8], encrypted_core: &[u8]) -> Result<()> {
    let s = String::from_utf8(metadata_plain.to_vec())
        .map_err(|_| anyhow!("Skrytá metadata nejdou přečíst: špatný klíč nebo poškozený soubor"))?;
    let is_metaqcs6 = s.starts_with("METAQCS6|");
    let is_metaqes7 = s.starts_with("METAQES7|");

    if !(is_metaqcs6 || is_metaqes7) {
        return Err(anyhow!("Skrytá metadata nemají správnou verzi"));
    }

    if is_metaqes7 {
        let zero_model = read_meta_value(&s, "zero_model")?;
        if zero_model != "ZERO_UNDER_TAG1" {
            return Err(anyhow!("Metadata: ZERO model nesedí"));
        }

        let pre_core_otp = read_meta_value(&s, "pre_core_otp")?;
        let tag1_xof_mask = read_meta_value(&s, "tag1_xof_mask")?;
        let tag2_xof_mask = read_meta_value(&s, "tag2_xof_mask")?;

        if pre_core_otp != "on" || tag1_xof_mask != "on" || tag2_xof_mask != "on" {
            return Err(anyhow!("Metadata: QES ochranné vrstvy nesedí"));
        }
    }

    let plain_len = read_meta_usize(&s, "plain_len")?;
    let core_len = read_meta_usize(&s, "core_len")?;
    let plain_hash = read_meta_value(&s, "plain_hash")?;
    let core_hash = read_meta_value(&s, "core_hash")?;
    if plain_len != plaintext.len() || core_len != encrypted_core.len() {
        return Err(anyhow!("Metadata nesedí: délka souboru byla změněna"));
    }
    if plain_hash != blake3_hex(plaintext) || core_hash != blake3_hex(encrypted_core) {
        return Err(anyhow!("Metadata nesedí: hash plaintextu nebo encrypted_core je jiný"));
    }
    Ok(())
}

fn read_meta_usize(s: &str, key: &str) -> Result<usize> {
    read_meta_value(s, key)?
        .parse::<usize>()
        .map_err(|_| anyhow!("Metadata: neplatná hodnota {key}"))
}

fn read_meta_value<'a>(s: &'a str, key: &str) -> Result<&'a str> {
    let prefix = format!("{}=", key);
    for part in s.split('|') {
        if let Some(v) = part.strip_prefix(&prefix) {
            return Ok(v);
        }
    }
    Err(anyhow!("Metadata: chybí {key}"))
}

pub fn run_self_tests() -> Vec<SelfTestCase> {
    let params = sample_params();
    let mut results = Vec::new();

    let data_sets: Vec<Vec<u8>> = vec![
        Vec::new(),
        vec![0],
        b"Heslo".to_vec(),
        b"QCS ASCII art roundtrip test".to_vec(),
        (0..=255).map(|x| x as u8).collect(),
        (0..4096).map(|i| ((i * 31 + 7) % 256) as u8).collect(),
    ];

    match params.test_route_context() {
        Ok(ctx) => {
            results.push(test_route_seeds(&ctx));
            results.push(test_subkey_separation(&ctx));
            results.extend(test_permutation_steps(&ctx));
            results.extend(test_diffusion_steps(&ctx));
            results.extend(test_superposition_steps(&ctx));
            results.extend(test_single_round_steps(&ctx));
            results.extend(test_mac_steps(&ctx));

            for data in &data_sets {
                let enc = encrypt_core(data, &ctx);
                let dec = decrypt_core(&enc, &ctx);
                let ok = enc.len() == data.len() && dec == *data;
                results.push(SelfTestCase {
                    name: format!("1:1 encrypted core + roundtrip / {} B", data.len()),
                    ok,
                    detail: format!("input={}B encrypted_core={}B decrypted={}B checksum={}", data.len(), enc.len(), dec.len(), blake3_hex(&enc)),
                });
            }
        }
        Err(e) => results.push(SelfTestCase {
            name: "route context".to_string(),
            ok: false,
            detail: e.to_string(),
        }),
    }

    for data in &data_sets {
        match encrypt_to_ascii_art(data, &params).and_then(|art| {
            let art_chars = art.chars().count();
            decrypt_from_ascii_art(&art, &params).map(|dec| (art_chars, dec))
        }) {
            Ok((art_chars, dec)) => results.push(SelfTestCase {
                name: format!("ASCII art carrier roundtrip + 3 tagy / {} B", data.len()),
                ok: dec == *data,
                detail: format!("ascii_art_chars={} output={}B", art_chars, dec.len()),
            }),
            Err(e) => results.push(SelfTestCase {
                name: format!("ASCII art carrier roundtrip + 3 tagy / {} B", data.len()),
                ok: false,
                detail: e.to_string(),
            }),
        }
    }

    match encrypt_to_ascii_art(b"Heslo", &params) {
        Ok(art) => {
            let looks_like_art = art.contains("BEGIN QCS ASCII ART")
                && art.contains("END QCS ASCII ART")
                && art.lines().count() >= 10
                && !art.contains("ZERO:")
                && !art.contains("SEAL:")
                && !art.contains("QCSAA1 len=")
                && !art.contains("ZEROQCS6|");
            results.push(SelfTestCase {
                name: "ASCII art carrier má legacy metadata seal a QES V3 vrstvy".to_string(),
                ok: looks_like_art,
                detail: format!("lines={}", art.lines().count()),
            });
        }
        Err(e) => results.push(SelfTestCase {
            name: "ASCII art shape".to_string(),
            ok: false,
            detail: e.to_string(),
        }),
    }

    // Stejný plaintext se stejným heslem musí díky novému skrytému nonce dát jiný ASCII/cipher výstup.
    match (encrypt_to_ascii_art(b"same", &params), encrypt_to_ascii_art(b"same", &params)) {
        (Ok(a), Ok(b)) => results.push(SelfTestCase {
            name: "skryté nonce mění výstup při každém šifrování".to_string(),
            ok: a != b,
            detail: "stejný plaintext + stejné heslo/seedy → jiný QCS ASCII balíček".to_string(),
        }),
        (Err(e), _) | (_, Err(e)) => results.push(SelfTestCase {
            name: "skryté nonce mění výstup při každém šifrování".to_string(),
            ok: false,
            detail: e.to_string(),
        }),
    }

    results.extend(test_strength_indicators(&params));

    let mut wrong_particle = params.clone();
    wrong_particle.particle.value = wrong_particle.particle.value.wrapping_add(1);
    match encrypt_to_ascii_art(b"Kontrola spatne particle", &params) {
        Ok(art) => {
            let ok = decrypt_from_ascii_art(&art, &wrong_particle).is_err();
            results.push(SelfTestCase {
                name: "špatná particle zastaví TAG kontrola".to_string(),
                ok,
                detail: "Změna particle musí skončit chybou před vrácením plaintextu.".to_string(),
            });
        }
        Err(e) => results.push(SelfTestCase {
            name: "špatná particle zastaví TAG kontrola".to_string(),
            ok: false,
            detail: e.to_string(),
        }),
    }

    results
}


fn test_strength_indicators(params: &QcsParams) -> Vec<SelfTestCase> {
    let mut out = Vec::new();
    let data: Vec<u8> = (0..2048).map(|i| ((i * 73 + 41) % 256) as u8).collect();
    let mut changed = data.clone();
    if let Some(first) = changed.first_mut() { *first ^= 1; }

    match params.test_route_context() {
        Ok(ctx) => {
            let enc_a = encrypt_core(&data, &ctx);
            let enc_b = encrypt_core(&changed, &ctx);
            let diff = bit_difference(&enc_a, &enc_b);
            let total = enc_a.len().saturating_mul(8).max(1);
            let ratio = diff as f64 / total as f64;
            out.push(SelfTestCase {
                name: "odolnostní indikátor: lavinový efekt po změně 1 bitu".to_string(),
                ok: ratio > 0.25,
                detail: format!("changed_bits={} total_bits={} ratio={:.2}%", diff, total, ratio * 100.0),
            });

            let mut wrong = params.clone();
            wrong.password.push_str("-wrong");
            match wrong.test_route_context() {
                Ok(ctx_wrong) => {
                    let enc_wrong = encrypt_core(&data, &ctx_wrong);
                    let key_diff = bit_difference(&enc_a, &enc_wrong);
                    let key_ratio = key_diff as f64 / total as f64;
                    out.push(SelfTestCase {
                        name: "odolnostní indikátor: změna klíče změní encrypted_core".to_string(),
                        ok: enc_a != enc_wrong && key_ratio > 0.25,
                        detail: format!("key_changed_bits={} total_bits={} ratio={:.2}%", key_diff, total, key_ratio * 100.0),
                    });
                }
                Err(e) => out.push(SelfTestCase { name: "odolnostní indikátor: změna klíče".to_string(), ok: false, detail: e.to_string() }),
            }

            let plaintext_visible = enc_a.windows(16).any(|w| data.windows(16).any(|p| p == w));
            out.push(SelfTestCase {
                name: "odolnostní indikátor: v encrypted_core není viditelný dlouhý plaintext blok".to_string(),
                ok: !plaintext_visible,
                detail: "Test hledá shodu 16 po sobě jdoucích bajtů mezi vstupem a encrypted_core.".to_string(),
            });
        }
        Err(e) => out.push(SelfTestCase { name: "odolnostní indikátory".to_string(), ok: false, detail: e.to_string() }),
    }

    match encrypt_to_ascii_art(&data, params) {
        Ok(art) => {
            let mut wrong = params.clone();
            wrong.password.push_str("-wrong");
            let rejected = decrypt_from_ascii_art(&art, &wrong).is_err();
            out.push(SelfTestCase {
                name: "odolnostní indikátor: špatné heslo nevrátí plaintext".to_string(),
                ok: rejected,
                detail: "ASCII balíček musí skončit chybou při špatném hesle/seedech.".to_string(),
            });
        }
        Err(e) => out.push(SelfTestCase { name: "odolnostní indikátor: špatné heslo".to_string(), ok: false, detail: e.to_string() }),
    }

    out
}

fn bit_difference(a: &[u8], b: &[u8]) -> usize {
    a.iter().zip(b.iter()).map(|(x, y)| (x ^ y).count_ones() as usize).sum::<usize>()
        + a.len().abs_diff(b.len()).saturating_mul(8)
}

fn sample_params() -> QcsParams {
    QcsParams {
        password: "tajne-heslo".to_string(),
        seed1: "seed-111".to_string(),
        seed2: "seed-222".to_string(),
        seed3: "seed-333".to_string(),
        seed4: "seed-444".to_string(),
        particle: Particle {
            glyph: "X".to_string(),
            value: 77,
            vector: "vektor".to_string(),
            phase: 13,
            amplitude: 9,
        },
    }
}


fn test_subkey_separation(ctx: &RouteContext) -> SelfTestCase {
    let keys: Vec<(&str, &[u8; 32])> = vec![
        ("pre_otp_key", &ctx.pre_otp_key),
        ("enc_key", &ctx.enc_key),
        ("route_key", &ctx.route_key),
        ("replicator_key", &ctx.replicator_key),
        ("zero_key", &ctx.zero_key),
        ("tag1_mac_key", &ctx.tag1_mac_key),
        ("tag1_xof_key", &ctx.tag1_xof_key),
        ("tag2_mac_key", &ctx.tag2_mac_key),
        ("tag2_xof_key", &ctx.tag2_xof_key),
        ("tag3_mac_key", &ctx.tag3_mac_key),
        ("meta_bind_key", &ctx.meta_bind_key),
    ];

    let mut ok = true;
    let mut collision = String::new();

    for i in 0..keys.len() {
        for j in (i + 1)..keys.len() {
            if keys[i].1 == keys[j].1 {
                ok = false;
                collision = format!("{} == {}", keys[i].0, keys[j].0);
            }
        }
    }

    SelfTestCase {
        name: "QES V3: oddělené subklíče pro PRE/TAG1/TAG2/TAG3/ZERO".to_string(),
        ok,
        detail: if ok {
            format!(
                "{} subklíčů má odlišné hodnoty; tag1={} tag2={} tag3={}",
                keys.len(),
                fingerprint(ctx.tag1_mac_key.as_slice()),
                fingerprint(ctx.tag2_mac_key.as_slice()),
                fingerprint(ctx.tag3_mac_key.as_slice())
            )
        } else {
            format!("kolize subklíčů: {}", collision)
        },
    }
}


fn test_route_seeds(ctx: &RouteContext) -> SelfTestCase {
    let ok = ctx.hidden_iv != [0u8; 32] && ctx.route_seed != [0u8; 32] && ctx.hidden_iv != ctx.route_seed;
    SelfTestCase {
        name: "skok 0: nonce vytvoří odlišný hidden_iv, route_seed a mac_key".to_string(),
        ok,
        detail: format!("hidden_iv={} route_seed={} mac_key={}", fingerprint(&ctx.hidden_iv), fingerprint(&ctx.route_seed), fingerprint(&ctx.mac_key)),
    }
}

fn test_permutation_steps(ctx: &RouteContext) -> Vec<SelfTestCase> {
    [1usize, 2, 5, 32, 257]
        .into_iter()
        .map(|len| {
            let mut data: Vec<u8> = (0..len).map(|i| (i % 251) as u8).collect();
            let original = data.clone();
            let p = ctx.round_permutation(len, 3, "perm");
            let inv = invert_permutation(&p);
            apply_permutation(&mut data, &p);
            apply_permutation(&mut data, &inv);
            SelfTestCase {
                name: format!("skok permutace: inverse funguje / {} B", len),
                ok: data == original,
                detail: format!("first_perm={}", p.iter().take(8).map(|v| v.to_string()).collect::<Vec<_>>().join(",")),
            }
        })
        .collect()
}

fn test_diffusion_steps(ctx: &RouteContext) -> Vec<SelfTestCase> {
    [0usize, 1, 5, 64, 511]
        .into_iter()
        .map(|len| {
            let mut data: Vec<u8> = (0..len).map(|i| ((i * 17 + 9) % 256) as u8).collect();
            let original = data.clone();
            let stream = ctx.round_bytes(4, "diff-a", len.max(1));
            diffuse_forward(&mut data, &stream);
            diffuse_inverse(&mut data, &stream);
            SelfTestCase {
                name: format!("skok difuze: forward + inverse / {} B", len),
                ok: data == original,
                detail: format!("stream_fingerprint={}", fingerprint(&stream)),
            }
        })
        .collect()
}

fn test_superposition_steps(ctx: &RouteContext) -> Vec<SelfTestCase> {
    [0usize, 1, 7, 128, 512]
        .into_iter()
        .map(|len| {
            let mut data: Vec<u8> = (0..len).map(|i| ((i * 13 + 3) % 256) as u8).collect();
            let original = data.clone();
            let stream = ctx.round_bytes(8, "super", len.saturating_mul(2).max(2));
            superpose_forward(&mut data, &stream);
            superpose_inverse(&mut data, &stream);
            SelfTestCase {
                name: format!("skok superpozice: add/rotate + inverse / {} B", len),
                ok: data == original,
                detail: format!("stream_fingerprint={}", fingerprint(&stream)),
            }
        })
        .collect()
}

fn test_single_round_steps(ctx: &RouteContext) -> Vec<SelfTestCase> {
    [1usize, 16, 255]
        .into_iter()
        .map(|len| {
            let mut data: Vec<u8> = (0..len).map(|i| ((i * 29 + 11) % 256) as u8).collect();
            let original = data.clone();
            apply_round_forward(&mut data, ctx, 12);
            let changed = data != original || len == 0;
            apply_round_inverse(&mut data, ctx, 12);
            SelfTestCase {
                name: format!("skok celé kolo: forward + inverse / {} B", len),
                ok: data == original && changed,
                detail: "permutace → difuze A → superpozice → difuze B → opačně".to_string(),
            }
        })
        .collect()
}

fn test_mac_steps(ctx: &RouteContext) -> Vec<SelfTestCase> {
    [0usize, 5, 1024]
        .into_iter()
        .map(|len| {
            let data: Vec<u8> = (0..len).map(|i| ((i * 7 + 1) % 256) as u8).collect();
            let tag = ctx.mac("TAG1_CORE_ENCRYPTED_DATA", &[data.as_slice()], TAG_LEN);
            let mut changed = data.clone();
            if let Some(first) = changed.first_mut() { *first ^= 1; }
            let changed_tag = ctx.mac("TAG1_CORE_ENCRYPTED_DATA", &[changed.as_slice()], TAG_LEN);
            let ok = tag.len() == TAG_LEN && changed_tag.len() == TAG_LEN && (len == 0 || !constant_time_eq(&tag, &changed_tag));
            SelfTestCase {
                name: format!("TAG/MAC: změna dat změní pečeť / {} B", len),
                ok,
                detail: format!("tag_fingerprint={}", fingerprint(&tag)),
            }
        })
        .collect()
}

fn diagnostic_guide() -> Vec<String> {
    vec![
        "1) HEADER_KEY slouží jen k odemčení skryté ZERO hlavičky, kde je nonce.".to_string(),
        "2) NONCE je v balíčku maskované; z hesla + seedů + nonce vzniká hlavní master_seed.".to_string(),
        "3) PERMUTACE mění pořadí bajtů. DIFUZE rozlévá změnu přes data. SUPERPOZICE přičítá proud a rotuje bity.".to_string(),
        "4) PRE-CORE XOR OTP-like pad maskuje vstup před skoky; TAG1/TAG2 mají oddělené subklíče a TAG3 chrání ASCII balíček.".to_string(),
        "5) Nejdřív se ověřují pečetě, až potom se dešifruje. Bit flip nebo změna znaků musí skončit chybou.".to_string(),
        "6) ASCII art je nosič. Jádro encrypted_core zůstává 1:1 vůči vstupu; ASCII text je delší.".to_string(),
    ]
}

fn push_frame(frames: &mut Vec<DiagnosticFrame>, stage: &str, round: Option<usize>, data: &[u8], variables: Vec<DiagnosticVar>) {
    let index = frames.len();
    frames.push(DiagnosticFrame {
        index,
        stage: stage.to_string(),
        round,
        len: data.len(),
        checksum: blake3_hex(data),
        preview_hex: preview_hex(data, 64),
        variables,
        ascii_image: bytes_to_ascii_image(stage, round, data),
        download_name: format!("qcs_jump_{index:03}_{}.txt", slug(stage)),
    });
}

fn bytes_to_ascii_image(stage: &str, round: Option<usize>, data: &[u8]) -> String {
    let mut out = String::new();
    out.push_str("QCS SKOK / DIAGNOSTICKÝ OBRAZ\n");
    out.push_str(&format!("stage: {stage}\n"));
    if let Some(r) = round { out.push_str(&format!("round: {r}\n")); }
    out.push_str(&format!("len: {} B\n", data.len()));
    out.push_str(&format!("checksum: {}\n", blake3_hex(data)));
    out.push_str(&format!("preview_hex: {}\n", preview_hex(data, 64)));
    out.push_str("\n");

    if data.is_empty() {
        out.push_str("[prázdný stav]\n");
        return out;
    }

    let palette = b" .:-=+*#%@XO0&$?A";
    let take_len = data.len().min(FRAME_BYTES_LIMIT);
    for chunk in data[..take_len].chunks(FRAME_IMAGE_WIDTH) {
        out.push('|');
        for &b in chunk {
            let idx = (b as usize * (palette.len() - 1)) / 255;
            out.push(palette[idx] as char);
        }
        for _ in chunk.len()..FRAME_IMAGE_WIDTH {
            out.push(' ');
        }
        out.push('|');
        out.push('\n');
    }
    if data.len() > take_len {
        out.push_str(&format!("... zobrazeno prvních {} z {} B\n", take_len, data.len()));
    }
    out
}

fn var(name: &str, value: &str, note: &str) -> DiagnosticVar {
    DiagnosticVar { name: name.to_string(), value: value.to_string(), note: note.to_string() }
}

fn fingerprint(bytes: &[u8]) -> String {
    let h = blake3::hash(bytes);
    to_hex(&h.as_bytes()[..8])
}

fn preview_hex(bytes: &[u8], max: usize) -> String {
    if bytes.is_empty() { return "∅".to_string(); }
    let mut s = to_hex(&bytes[..bytes.len().min(max)]);
    if bytes.len() > max { s.push_str("…"); }
    s
}

fn blake3_hex(data: &[u8]) -> String {
    let h = blake3::hash(data);
    to_hex(h.as_bytes())
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

fn slug(s: &str) -> String {
    s.chars()
        .map(|c| if c.is_ascii_alphanumeric() { c.to_ascii_lowercase() } else { '_' })
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    fn params() -> QcsParams { sample_params() }

    #[test]
    fn core_roundtrip() {
        let ctx = params().test_route_context().unwrap();
        let data = b"Ahoj, tohle je test QCS ASCII art nosice.";
        let enc = encrypt_core(data, &ctx);
        assert_ne!(enc, data);
        let dec = decrypt_core(&enc, &ctx);
        assert_eq!(dec, data);
    }

    #[test]
    #[ignore = "slow on GitHub/Termux; run manually before release"]
    fn every_self_test_passes() {
        let results = run_self_tests();
        let failed: Vec<_> = results.iter().filter(|t| !t.ok).collect();
        assert!(failed.is_empty(), "failed tests: {failed:#?}");
    }

    #[test]
    #[ignore = "slow on GitHub/Termux; run manually before release"]
    fn diagnostic_frames_are_created() {
        let p = params();
        let report = diagnose_pipeline(b"diagnostika", &p, true).unwrap();
        assert!(report.ok);
        assert_eq!(report.input_len, report.encrypted_core_len);
        assert!(report.frame_count >= 123);
        assert!(report.frames.iter().any(|f| f.stage == "PERMUTACE"));
        assert!(report.frames.iter().any(|f| f.stage == "ASCII_ART_CARRIER_TAG3_SEAL"));
    }

    #[test]
    fn ascii_roundtrip() {
        let p = params();
        let data = b"Test pres ASCII art nosic.";
        let art = encrypt_to_ascii_art(data, &p).unwrap();
        assert!(!art.contains("ZERO:"), "new ASCII-only carrier must not expose ZERO: line");
        assert!(!art.contains("SEAL:"), "new ASCII-only carrier must not expose SEAL: line");
        let dec = decrypt_from_ascii_art(&art, &p).unwrap();
        assert_eq!(dec, data);
    }
}
