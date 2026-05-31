use anyhow::{anyhow, Result};
use base64::{engine::general_purpose::STANDARD as B64, Engine as _};
use serde::Serialize;

use crate::cipher::QcsParams;
use crate::kdf::MASTER_SEED_LEN;
use crate::route::constant_time_eq;

pub const QES_CAPSULE_LEN: usize = 128;
const MAGIC: &[u8; 4] = b"QES1";
const VERSION: u8 = 1;

#[derive(Clone, Copy, Debug, Serialize, PartialEq, Eq)]
pub enum QesMode {
    NormalAscii = 1,
    CarrierAppend = 2,
    AdaptiveLabyrinthPlanned = 3,
}

impl QesMode {
    pub fn from_byte(v: u8) -> Result<Self> {
        match v {
            1 => Ok(Self::NormalAscii),
            2 => Ok(Self::CarrierAppend),
            3 => Ok(Self::AdaptiveLabyrinthPlanned),
            _ => Err(anyhow!("Neznámý QES režim v kapsli: {v}")),
        }
    }

    pub fn label(self) -> &'static str {
        match self {
            Self::NormalAscii => "normal-ascii-core",
            Self::CarrierAppend => "carrier-compat-v7",
            Self::AdaptiveLabyrinthPlanned => "adaptive-labyrinth-cover-planned",
        }
    }
}

#[derive(Clone, Debug, Serialize)]
pub struct QesCapsuleReport {
    pub ok: bool,
    pub version: u8,
    pub mode: String,
    pub capsule_len: usize,
    pub capsule_b64: String,
    pub capsule_hex: String,
    pub public_hash_hex: String,
    pub keyed_mac_hex: String,
    pub payload_len: usize,
    pub note: String,
}

#[derive(Clone, Debug, Serialize)]
pub struct QesCapsuleVerifyReport {
    pub ok: bool,
    pub version: u8,
    pub mode: String,
    pub capsule_len: usize,
    pub payload_len: usize,
    pub public_hash_hex: String,
    pub keyed_mac_hex: String,
    pub note: String,
}

pub fn create_capsule(final_data: &[u8], mode: QesMode, payload_len: usize, params: &QcsParams) -> Result<QesCapsuleReport> {
    let seed = params.header_seed()?;
    let public_hash_bytes = blake3::hash(final_data);
    let file_commitment = commitment_bytes(final_data);

    let mut capsule = [0u8; QES_CAPSULE_LEN];
    capsule[0..4].copy_from_slice(MAGIC);
    capsule[4] = VERSION;
    capsule[5] = mode as u8;
    capsule[6] = 0; // flags: reserved for future capacity modes
    capsule[7] = QES_CAPSULE_LEN as u8;

    let public_anchor = keyed_xof(&seed, b"public-anchor", final_data, 16);
    let curve_anchor = keyed_xof(&seed, b"curve-anchor", final_data, 16);
    let final_phase = keyed_xof(&seed, b"final-phase", final_data, 16);
    capsule[8..24].copy_from_slice(&public_anchor);
    capsule[24..40].copy_from_slice(&curve_anchor);
    capsule[40..56].copy_from_slice(&final_phase);

    let mask = keyed_xof(&seed, b"payload-len-mask", public_hash_bytes.as_bytes(), 8);
    let mut len_bytes = (payload_len as u64).to_le_bytes();
    for i in 0..8 { len_bytes[i] ^= mask[i]; }
    capsule[56..64].copy_from_slice(&len_bytes);

    capsule[64..96].copy_from_slice(&file_commitment);

    let mac = capsule_mac(&seed, &capsule[..96], final_data, mode);
    capsule[96..128].copy_from_slice(&mac);

    Ok(QesCapsuleReport {
        ok: true,
        version: VERSION,
        mode: mode.label().to_string(),
        capsule_len: QES_CAPSULE_LEN,
        capsule_b64: B64.encode(capsule),
        capsule_hex: to_hex(&capsule),
        public_hash_hex: to_hex(public_hash_bytes.as_bytes()),
        keyed_mac_hex: to_hex(&mac),
        payload_len,
        note: "QES-128 kapsle je samostatný artefakt pro uživatele. Neobsahuje heslo, master seed ani mapu bodů; jen kotvu, commitment a MAC svázaný se souborem.".to_string(),
    })
}

pub fn verify_capsule_b64(final_data: &[u8], capsule_b64: &str, expected_mode: QesMode, params: &QcsParams) -> Result<QesCapsuleVerifyReport> {
    let capsule_vec = B64.decode(capsule_b64.trim().as_bytes())
        .map_err(|e| anyhow!("QES kapsle není platné Base64: {e}"))?;
    verify_capsule_bytes(final_data, &capsule_vec, expected_mode, params)
}

pub fn verify_capsule_bytes(final_data: &[u8], capsule: &[u8], expected_mode: QesMode, params: &QcsParams) -> Result<QesCapsuleVerifyReport> {
    if capsule.len() != QES_CAPSULE_LEN {
        return Err(anyhow!("QES kapsle musí mít přesně {QES_CAPSULE_LEN} bajtů, ale má {} bajtů", capsule.len()));
    }
    if &capsule[0..4] != MAGIC {
        return Err(anyhow!("QES kapsle nemá magic QES1"));
    }
    if capsule[4] != VERSION {
        return Err(anyhow!("Nepodporovaná verze QES kapsle: {}", capsule[4]));
    }
    if capsule[7] as usize != QES_CAPSULE_LEN {
        return Err(anyhow!("QES kapsle má chybnou délkovou značku"));
    }
    let mode = QesMode::from_byte(capsule[5])?;
    if mode != expected_mode {
        return Err(anyhow!("QES kapsle je pro režim {}, ale požadovaný režim je {}", mode.label(), expected_mode.label()));
    }

    let seed = params.header_seed()?;
    let expected_commitment = commitment_bytes(final_data);
    if !constant_time_eq(&capsule[64..96], &expected_commitment) {
        return Err(anyhow!("QES kapsle nesedí k tomuto souboru: file commitment je jiný"));
    }

    let expected_mac = capsule_mac(&seed, &capsule[..96], final_data, mode);
    if !constant_time_eq(&capsule[96..128], &expected_mac) {
        return Err(anyhow!("QES kapsle nebo soubor byly změněny, keyed MAC nesedí"));
    }

    let public_hash = blake3::hash(final_data);
    let mask = keyed_xof(&seed, b"payload-len-mask", public_hash.as_bytes(), 8);
    let mut len_bytes = [0u8; 8];
    len_bytes.copy_from_slice(&capsule[56..64]);
    for i in 0..8 { len_bytes[i] ^= mask[i]; }
    let payload_len = u64::from_le_bytes(len_bytes) as usize;

    Ok(QesCapsuleVerifyReport {
        ok: true,
        version: VERSION,
        mode: mode.label().to_string(),
        capsule_len: QES_CAPSULE_LEN,
        payload_len,
        public_hash_hex: to_hex(public_hash.as_bytes()),
        keyed_mac_hex: to_hex(&expected_mac),
        note: "Kapsle sedí k tomuto souboru, režimu i zadanému klíči.".to_string(),
    })
}

pub fn public_hash_hex(data: &[u8]) -> String {
    to_hex(blake3::hash(data).as_bytes())
}

pub fn keyed_mac_hex(data: &[u8], params: &QcsParams) -> Result<String> {
    let seed = params.header_seed()?;
    let mac = keyed_file_mac(&seed, data);
    Ok(to_hex(&mac))
}

pub fn check_text(file_name: &str, report: &QesCapsuleReport) -> String {
    format!(
        "QES – Quantum Encryption System\nVersion: QES-1\nFile: {file_name}\nMode: {}\nCapsule: QES-128 / {} B\nPublic hash: {}\nKeyed MAC: {}\nPayload length: {} B\n\nKapsli uchovej odděleně od souboru. Bez stejného passwordu, seedů a particle nelze MAC ověřit ani obnovit navigaci.\n",
        report.mode, report.capsule_len, report.public_hash_hex, report.keyed_mac_hex, report.payload_len
    )
}

fn commitment_bytes(data: &[u8]) -> [u8; 32] {
    let mut h = blake3::Hasher::new();
    h.update(b"QES-FILE-COMMITMENT-v1|");
    h.update(&(data.len() as u64).to_le_bytes());
    h.update(data);
    *h.finalize().as_bytes()
}

fn keyed_file_mac(seed: &[u8; MASTER_SEED_LEN], data: &[u8]) -> [u8; 32] {
    let mut key = [0u8; 32];
    key.copy_from_slice(&seed[..32]);
    let mut h = blake3::Hasher::new_keyed(&key);
    h.update(b"QES-KEYED-FILE-MAC-v1|");
    h.update(&seed[32..]);
    h.update(&(data.len() as u64).to_le_bytes());
    h.update(data);
    *h.finalize().as_bytes()
}

fn capsule_mac(seed: &[u8; MASTER_SEED_LEN], capsule_body: &[u8], final_data: &[u8], mode: QesMode) -> [u8; 32] {
    let mut key = [0u8; 32];
    key.copy_from_slice(&seed[..32]);
    let mut h = blake3::Hasher::new_keyed(&key);
    h.update(b"QES-128-CAPSULE-MAC-v1|");
    h.update(&seed[32..]);
    h.update(&[mode as u8]);
    h.update(&(final_data.len() as u64).to_le_bytes());
    h.update(capsule_body);
    h.update(final_data);
    *h.finalize().as_bytes()
}

fn keyed_xof(seed: &[u8; MASTER_SEED_LEN], label: &[u8], context: &[u8], len: usize) -> Vec<u8> {
    let mut key = [0u8; 32];
    key.copy_from_slice(&seed[..32]);
    let mut h = blake3::Hasher::new_keyed(&key);
    h.update(b"QES-CAPSULE-XOF-v1|");
    h.update(label);
    h.update(b"|");
    h.update(&seed[32..]);
    h.update(b"|");
    h.update(context);
    let mut reader = h.finalize_xof();
    let mut out = vec![0u8; len];
    reader.fill(&mut out);
    out
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

pub fn run_capsule_tests() -> Vec<crate::cipher::SelfTestCase> {
    use crate::ascii_art::Particle;
    use crate::cipher::QcsParams;
    let params = QcsParams {
        password: "tajne-heslo".to_string(),
        seed1: "seed-111".to_string(),
        seed2: "seed-222".to_string(),
        seed3: "seed-333".to_string(),
        seed4: "seed-444".to_string(),
        particle: Particle { glyph: "X".to_string(), value: 77, vector: "vektor".to_string(), phase: 13, amplitude: 9 },
    };
    let mut tests = Vec::new();
    let final_data = b"QES final encrypted output bytes";
    match create_capsule(final_data, QesMode::NormalAscii, 1234, &params) {
        Ok(report) => {
            tests.push(crate::cipher::SelfTestCase { name: "QES-128 kapsle: vytvoreni".to_string(), ok: report.capsule_b64.len() > 100 && report.capsule_len == QES_CAPSULE_LEN, detail: format!("capsule={}B mode={}", report.capsule_len, report.mode) });
            match verify_capsule_b64(final_data, &report.capsule_b64, QesMode::NormalAscii, &params) {
                Ok(v) => tests.push(crate::cipher::SelfTestCase { name: "QES-128 kapsle: overeni spravnym klicem".to_string(), ok: v.ok && v.payload_len == 1234, detail: format!("payload_len={} mac={}", v.payload_len, &v.keyed_mac_hex[..16]) }),
                Err(e) => tests.push(crate::cipher::SelfTestCase { name: "QES-128 kapsle: overeni spravnym klicem".to_string(), ok: false, detail: e.to_string() }),
            }
            let mut tampered = final_data.to_vec();
            tampered[0] ^= 1;
            let rejected = verify_capsule_b64(&tampered, &report.capsule_b64, QesMode::NormalAscii, &params).is_err();
            tests.push(crate::cipher::SelfTestCase { name: "QES-128 kapsle: zmena souboru odmítnuta".to_string(), ok: rejected, detail: "Změna souboru musí změnit file commitment / MAC.".to_string() });

            let mut wrong = params.clone();
            wrong.seed4.push_str("x");
            let wrong_rejected = verify_capsule_b64(final_data, &report.capsule_b64, QesMode::NormalAscii, &wrong).is_err();
            tests.push(crate::cipher::SelfTestCase { name: "QES-128 kapsle: spatny klic odmitnut".to_string(), ok: wrong_rejected, detail: "Jiný seed musí vytvořit jiný MAC.".to_string() });
        }
        Err(e) => tests.push(crate::cipher::SelfTestCase { name: "QES-128 kapsle: vytvoreni".to_string(), ok: false, detail: e.to_string() }),
    }
    tests
}
