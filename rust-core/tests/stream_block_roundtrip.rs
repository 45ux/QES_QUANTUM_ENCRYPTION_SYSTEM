use qes_quantum_encryption_system::ascii_art::Particle;
use qes_quantum_encryption_system::cipher::{
    decrypt_from_ascii_art,
    encrypt_to_ascii_art,
    QcsParams,
};

fn params_for_block(mode: &str, block_index: usize) -> QcsParams {
    QcsParams {
        password: "stream-test-password".to_string(),
        seed1: "stream-seed-1".to_string(),
        seed2: "stream-seed-2".to_string(),
        seed3: "stream-seed-3".to_string(),
        seed4: format!("stream-seed-4|QES-STREAM-BLOCK|{}|{}", mode, block_index),
        particle: Particle {
            glyph: "X".to_string(),
            value: 77,
            vector: "stream-vector".to_string(),
            phase: 13,
            amplitude: 9,
        },
    }
}

fn test_data(len: usize) -> Vec<u8> {
    (0..len)
        .map(|i| ((i * 37 + i / 3 + 19) % 256) as u8)
        .collect()
}

fn hex_hash(data: &[u8]) -> String {
    blake3::hash(data).to_hex().to_string()
}

fn stream_roundtrip(data_len: usize, block_size: usize, mode: &str) {
    let plain = test_data(data_len);

    let mut encrypted_blocks: Vec<Vec<u8>> = Vec::new();

    for (block_index, chunk) in plain.chunks(block_size).enumerate() {
        let params = params_for_block(mode, block_index);
        let art = encrypt_to_ascii_art(chunk, &params).expect("encrypt stream block");
        assert!(!art.contains("ZERO:"), "stream block must not expose ZERO:");
        assert!(!art.contains("SEAL:"), "stream block must not expose SEAL:");
        encrypted_blocks.push(art.into_bytes());
    }

    let mut decrypted = Vec::new();

    for (block_index, block) in encrypted_blocks.iter().enumerate() {
        let params = params_for_block(mode, block_index);
        let art = String::from_utf8(block.clone()).expect("ASCII art must be UTF-8");
        let dec = decrypt_from_ascii_art(&art, &params).expect("decrypt stream block");
        decrypted.extend_from_slice(&dec);
    }

    assert_eq!(plain.len(), decrypted.len(), "stream size mismatch");
    assert_eq!(hex_hash(&plain), hex_hash(&decrypted), "stream SHA mismatch");
    assert_eq!(plain, decrypted, "stream bytes mismatch");
}

#[test]
fn stream_blocks_roundtrip_small_sizes() {
    for len in [0usize, 1, 2, 7, 31, 255, 1024, 4097] {
        stream_roundtrip(len, 256, "test-small");
    }
}

#[test]
fn stream_blocks_roundtrip_1mb_sha_exact() {
    stream_roundtrip(1024 * 1024, 256 * 1024, "test-1mb");
}
