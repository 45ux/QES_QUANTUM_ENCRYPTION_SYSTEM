use qes_quantum_encryption_system::ascii_art::Particle;
use qes_quantum_encryption_system::capsule::{create_capsule, verify_capsule_b64, QesMode, QES_CAPSULE_LEN};
use qes_quantum_encryption_system::cipher::{encrypt_to_ascii_art, QcsParams};

fn params() -> QcsParams {
    QcsParams {
        password: "tajne-heslo".to_string(),
        seed1: "seed-111".to_string(),
        seed2: "seed-222".to_string(),
        seed3: "seed-333".to_string(),
        seed4: "seed-444".to_string(),
        particle: Particle { glyph: "X".to_string(), value: 77, vector: "vektor".to_string(), phase: 13, amplitude: 9 },
    }
}

#[test]
fn qes_capsule_is_exactly_128_bytes_after_base64_decode() {
    let p = params();
    let qes = encrypt_to_ascii_art(b"test", &p).unwrap();
    let cap = create_capsule(qes.as_bytes(), QesMode::NormalAscii, 4, &p).unwrap();
    let raw = base64::Engine::decode(&base64::engine::general_purpose::STANDARD, cap.capsule_b64.as_bytes()).unwrap();
    assert_eq!(raw.len(), QES_CAPSULE_LEN);
}

#[test]
fn qes_capsule_verifies_correct_file_and_key() {
    let p = params();
    let qes = encrypt_to_ascii_art(b"test", &p).unwrap();
    let cap = create_capsule(qes.as_bytes(), QesMode::NormalAscii, 4, &p).unwrap();
    let verify = verify_capsule_b64(qes.as_bytes(), &cap.capsule_b64, QesMode::NormalAscii, &p).unwrap();
    assert!(verify.ok);
    assert_eq!(verify.payload_len, 4);
}

#[test]
fn qes_capsule_rejects_tampered_file() {
    let p = params();
    let mut qes = encrypt_to_ascii_art(b"test", &p).unwrap().into_bytes();
    let cap = create_capsule(&qes, QesMode::NormalAscii, 4, &p).unwrap();
    qes[10] ^= 1;
    assert!(verify_capsule_b64(&qes, &cap.capsule_b64, QesMode::NormalAscii, &p).is_err());
}

#[test]
fn qes_capsule_rejects_wrong_key() {
    let p = params();
    let qes = encrypt_to_ascii_art(b"test", &p).unwrap();
    let cap = create_capsule(qes.as_bytes(), QesMode::NormalAscii, 4, &p).unwrap();
    let mut wrong = p.clone();
    wrong.seed1.push_str("-wrong");
    assert!(verify_capsule_b64(qes.as_bytes(), &cap.capsule_b64, QesMode::NormalAscii, &wrong).is_err());
}
