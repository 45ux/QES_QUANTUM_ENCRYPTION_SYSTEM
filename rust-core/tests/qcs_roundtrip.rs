use qes_quantum_encryption_system::ascii_art::Particle;
use qes_quantum_encryption_system::cipher::{decrypt_core, decrypt_from_ascii_art, diagnose_pipeline, encrypt_core, encrypt_to_ascii_art, run_self_tests, QcsParams};

fn params() -> QcsParams {
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

#[test]
fn encrypted_core_is_1_to_1_for_many_lengths() {
    let p = params();
    let ctx = p.test_route_context().unwrap();
    let lengths = [0usize, 1, 2, 5, 16, 64, 255, 1024, 4096];

    for len in lengths {
        let data: Vec<u8> = (0..len).map(|i| ((i * 31 + 17) % 256) as u8).collect();
        let enc = encrypt_core(&data, &ctx);
        assert_eq!(enc.len(), data.len(), "encrypted_core musi byt 1:1 pro delku {len}");
        let dec = decrypt_core(&enc, &ctx);
        assert_eq!(dec, data, "roundtrip encrypted_core selhal pro delku {len}");
    }
}

#[test]
fn ascii_art_roundtrip_for_text() {
    let p = params();
    let data = "Heslo".as_bytes();
    let art = encrypt_to_ascii_art(data, &p).unwrap();
    let dec = decrypt_from_ascii_art(&art, &p).unwrap();
    assert_eq!(dec, data);
}

#[test]
fn ascii_art_roundtrip_for_binary_data() {
    let p = params();
    let data: Vec<u8> = (0..2048).map(|i| ((i * 13 + 91) % 256) as u8).collect();
    let art = encrypt_to_ascii_art(&data, &p).unwrap();
    let dec = decrypt_from_ascii_art(&art, &p).unwrap();
    assert_eq!(dec, data);
}

#[test]
fn ascii_art_is_text_carrier_with_hidden_header_and_seal() {
    let p = params();
    let data = b"Heslo";
    let art = encrypt_to_ascii_art(data, &p).unwrap();
    assert!(art.contains("BEGIN QCS ASCII ART"));
    assert!(art.contains("END QCS ASCII ART"));
    assert!(art.contains("ZERO:"));
    assert!(art.contains("SEAL:"));
    assert!(!art.contains("QCSAA1 len="), "viditelna technicka hlavicka QCSAA1 len=... ma byt odstranena");
    assert!(!art.contains("ZEROQCS6|"), "ZEROQCS6 hlavička má být schovaná/maskovaná");
    assert!(art.lines().count() >= 10, "ASCII art ma mit vice radku a viditelny tvar");
    assert!(art.chars().count() > data.len(), "ASCII art je textovy nosic, proto je delsi nez 1:1 binarni jadro");
}

#[test]
fn same_plaintext_produces_different_art_because_nonce_is_hidden_and_random() {
    let p = params();
    let a = encrypt_to_ascii_art(b"same plaintext", &p).unwrap();
    let b = encrypt_to_ascii_art(b"same plaintext", &p).unwrap();
    assert_ne!(a, b, "skryte nonce musi udelat jiny vystup");
}

#[test]
fn bit_flip_is_rejected_by_mac() {
    let p = params();
    let data = b"Kontrola: bit flip nesmi projit.";
    let art = encrypt_to_ascii_art(data, &p).unwrap();
    let mut changed = art.clone().into_bytes();
    if let Some(pos) = changed.iter().position(|&b| b == b'.' || b == b':' || b == b'-') {
        changed[pos] ^= 1;
    }
    let changed_art = String::from_utf8_lossy(&changed).to_string();
    assert!(decrypt_from_ascii_art(&changed_art, &p).is_err());
}

#[test]
fn wrong_particle_is_rejected() {
    let p = params();
    let data = b"Kontrola: spatna particle nesmi vratit puvodni text.";
    let art = encrypt_to_ascii_art(data, &p).unwrap();

    let mut wrong = p.clone();
    wrong.particle.value = wrong.particle.value.wrapping_add(1);

    assert!(decrypt_from_ascii_art(&art, &wrong).is_err());
}

#[test]
fn self_tests_include_mac_steps_and_pass() {
    let tests = run_self_tests();
    assert!(tests.iter().any(|t| t.name.contains("permutace")), "chybi test permutace");
    assert!(tests.iter().any(|t| t.name.contains("difuze")), "chybi test difuze");
    assert!(tests.iter().any(|t| t.name.contains("superpozice")), "chybi test superpozice");
    assert!(tests.iter().any(|t| t.name.contains("TAG/MAC")), "chybi test MAC tagu");
    let failed: Vec<_> = tests.iter().filter(|t| !t.ok).collect();
    assert!(failed.is_empty(), "nektere testy selhaly: {failed:#?}");
}

#[test]
fn diagnostic_report_contains_frames_and_manual() {
    let p = params();
    let data = b"Manualni diagnostika QCS";
    let report = diagnose_pipeline(data, &p, true).unwrap();
    assert!(report.ok);
    assert_eq!(report.input_len, report.encrypted_core_len);
    assert!(!report.guide.is_empty(), "chybi manual orientace");
    assert!(report.frames.iter().any(|f| f.stage == "PERMUTACE"));
    assert!(report.frames.iter().any(|f| f.stage == "DIFUZE_A"));
    assert!(report.frames.iter().any(|f| f.stage == "SUPERPOZICE"));
    assert!(report.frames.iter().any(|f| f.stage == "ENCRYPTED_CORE_1_1"));
    assert!(report.frames.iter().any(|f| f.stage == "ASCII_ART_NOSIC_TAG3_ZERO_SEAL"));
    assert!(report.frames.iter().all(|f| !f.checksum.is_empty()));
    assert!(report.frames.iter().all(|f| !f.ascii_image.is_empty()));
}

#[test]
fn carrier_file_roundtrip_for_binary_payload() {
    use qes_quantum_encryption_system::carrier::{decrypt_from_carrier_file, encrypt_to_carrier_file};
    let p = params();
    let payload: Vec<u8> = (0..4096).map(|i| ((i * 37 + 19) % 256) as u8).collect();
    let cover = b"FAKE-COVER-FILE-BYTES-WITH-ANY-EXTENSION";
    let (carrier, info) = encrypt_to_carrier_file(&payload, cover, &p).unwrap();
    assert!(carrier.len() > cover.len());
    assert_eq!(info.cover_len, cover.len());
    let (out, verify_info) = decrypt_from_carrier_file(&carrier, &p).unwrap();
    assert_eq!(out, payload);
    assert_eq!(info.public_hash_hex, verify_info.public_hash_hex);
    assert_eq!(info.keyed_mac_hex, verify_info.keyed_mac_hex);
}

#[test]
fn carrier_file_tamper_is_rejected() {
    use qes_quantum_encryption_system::carrier::{decrypt_from_carrier_file, encrypt_to_carrier_file};
    let p = params();
    let (mut carrier, _) = encrypt_to_carrier_file(b"tajna data", b"cover", &p).unwrap();
    let idx = carrier.len() / 2;
    carrier[idx] ^= 1;
    assert!(decrypt_from_carrier_file(&carrier, &p).is_err());
}

#[test]
fn public_hash_and_keyed_mac_change_after_file_change() {
    use qes_quantum_encryption_system::carrier::{keyed_mac_hex, public_hash_hex};
    let p = params();
    let a = b"soubor A".to_vec();
    let mut b = a.clone();
    b[0] ^= 1;
    assert_ne!(public_hash_hex(&a), public_hash_hex(&b));
    assert_ne!(keyed_mac_hex(&a, &p).unwrap(), keyed_mac_hex(&b, &p).unwrap());
}

#[test]
fn self_tests_include_strength_indicators() {
    let tests = run_self_tests();
    assert!(tests.iter().any(|t| t.name.contains("lavinový efekt")), "chybí lavinový test");
    assert!(tests.iter().any(|t| t.name.contains("změna klíče")), "chybí test citlivosti na klíč");
    assert!(tests.iter().any(|t| t.name.contains("špatné heslo")), "chybí test odmítnutí špatného hesla");
    let failed: Vec<_> = tests.iter().filter(|t| !t.ok).collect();
    assert!(failed.is_empty(), "některé self testy selhaly: {failed:#?}");
}
