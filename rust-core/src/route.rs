use crate::kdf::{derive_32, derive_bytes, Nonce, MASTER_SEED_LEN};

#[derive(Clone)]
pub struct RouteContext {
    pub master_seed: [u8; MASTER_SEED_LEN],
    pub nonce: Nonce,
    pub hidden_iv: [u8; 32],
    pub route_seed: [u8; 32],
    pub metadata_key: [u8; 32],
    pub mac_key: [u8; 32],
}

impl RouteContext {
    pub fn new(master_seed: [u8; MASTER_SEED_LEN], nonce: Nonce) -> Self {
        let hidden_iv = derive_32(&master_seed, "hidden_iv_from_nonce");
        let route_seed = derive_32(&master_seed, "route_seed_from_nonce");
        let metadata_key = derive_32(&master_seed, "metadata_key_hidden_label");
        let mac_key = derive_32(&master_seed, "mac_key_three_zero_seals");
        Self {
            master_seed,
            nonce,
            hidden_iv,
            route_seed,
            metadata_key,
            mac_key,
        }
    }

    pub fn bytes(&self, label: &str, len: usize) -> Vec<u8> {
        let mut key = [0u8; 32];
        key.copy_from_slice(&self.route_seed);

        let mut hasher = blake3::Hasher::new_keyed(&key);
        hasher.update(b"QCS-ROUTE-v6|");
        hasher.update(label.as_bytes());
        hasher.update(b"|");
        hasher.update(&self.nonce);
        hasher.update(b"|");
        hasher.update(&self.hidden_iv);
        hasher.update(b"|");
        hasher.update(&self.master_seed);

        let mut reader = hasher.finalize_xof();
        let mut out = vec![0u8; len];
        reader.fill(&mut out);
        out
    }

    pub fn round_bytes(&self, round: usize, label: &str, len: usize) -> Vec<u8> {
        self.bytes(&format!("round:{round}:{label}"), len)
    }

    pub fn permutation(&self, len: usize, label: &str) -> Vec<usize> {
        make_permutation(len, &self.bytes(label, len.saturating_mul(8).max(8)))
    }

    pub fn round_permutation(&self, len: usize, round: usize, label: &str) -> Vec<usize> {
        make_permutation(
            len,
            &self.round_bytes(round, label, len.saturating_mul(8).max(8)),
        )
    }

    /// Skutečný ověřovací tag/MAC. To je pečeť, ne XOR maska.
    pub fn mac(&self, label: &str, parts: &[&[u8]], len: usize) -> Vec<u8> {
        let mut hasher = blake3::Hasher::new_keyed(&self.mac_key);
        hasher.update(b"QCS-MAC-v6|");
        hasher.update(label.as_bytes());
        hasher.update(b"|");
        hasher.update(&self.nonce);
        for part in parts {
            hasher.update(&(part.len() as u64).to_le_bytes());
            hasher.update(part);
        }
        let mut reader = hasher.finalize_xof();
        let mut out = vec![0u8; len];
        reader.fill(&mut out);
        out
    }

    /// Skrytí / odemčení metadat. Stejná operace slouží oběma směry.
    pub fn mask_metadata(&self, data: &[u8]) -> Vec<u8> {
        let mut hasher = blake3::Hasher::new_keyed(&self.metadata_key);
        hasher.update(b"QCS-METADATA-MASK-v6|");
        hasher.update(&self.nonce);
        hasher.update(&self.hidden_iv);
        let mut reader = hasher.finalize_xof();
        let mut stream = vec![0u8; data.len().max(1)];
        reader.fill(&mut stream);
        data.iter().enumerate().map(|(i, b)| b ^ stream[i % stream.len()]).collect()
    }
}

pub fn constant_time_eq(a: &[u8], b: &[u8]) -> bool {
    if a.len() != b.len() {
        return false;
    }
    let mut diff = 0u8;
    for i in 0..a.len() {
        diff |= a[i] ^ b[i];
    }
    diff == 0
}

pub fn make_permutation(len: usize, seed_stream: &[u8]) -> Vec<usize> {
    let mut p: Vec<usize> = (0..len).collect();
    if len < 2 {
        return p;
    }

    let mut cursor = CursorRng::new(seed_stream);
    for i in (1..len).rev() {
        let j = cursor.next_usize(i + 1);
        p.swap(i, j);
    }
    p
}

pub fn invert_permutation(p: &[usize]) -> Vec<usize> {
    let mut inv = vec![0usize; p.len()];
    for (new_pos, &old_pos) in p.iter().enumerate() {
        inv[old_pos] = new_pos;
    }
    inv
}

pub fn apply_permutation(data: &mut Vec<u8>, p: &[usize]) {
    let old = data.clone();
    for (new_pos, &old_pos) in p.iter().enumerate() {
        data[new_pos] = old[old_pos];
    }
}

struct CursorRng<'a> {
    bytes: &'a [u8],
    index: usize,
    fallback: u64,
}

impl<'a> CursorRng<'a> {
    fn new(bytes: &'a [u8]) -> Self {
        let fallback = if bytes.len() >= 8 {
            u64::from_le_bytes(bytes[0..8].try_into().unwrap())
        } else {
            0x9E37_79B9_7F4A_7C15
        };
        Self {
            bytes,
            index: 0,
            fallback,
        }
    }

    fn next_u64(&mut self) -> u64 {
        if self.index + 8 <= self.bytes.len() {
            let value = u64::from_le_bytes(self.bytes[self.index..self.index + 8].try_into().unwrap());
            self.index += 8;
            value
        } else {
            self.fallback = self
                .fallback
                .wrapping_mul(6364136223846793005)
                .wrapping_add(1442695040888963407);
            self.fallback
        }
    }

    fn next_usize(&mut self, upper: usize) -> usize {
        if upper <= 1 {
            return 0;
        }
        (self.next_u64() as usize) % upper
    }
}

#[allow(dead_code)]
pub fn debug_hex_route_seeds(master_seed: &[u8; MASTER_SEED_LEN]) -> (String, String) {
    (
        to_hex(&derive_bytes(master_seed, "hidden_iv_from_nonce", 32)),
        to_hex(&derive_bytes(master_seed, "route_seed_from_nonce", 32)),
    )
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
