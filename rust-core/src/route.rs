use crate::kdf::{derive_32, derive_bytes, Nonce, MASTER_SEED_LEN};

#[derive(Clone)]
pub struct RouteContext {
    pub master_seed: [u8; MASTER_SEED_LEN],
    pub nonce: Nonce,
    pub hidden_iv: [u8; 32],
    pub route_seed: [u8; 32],
    pub metadata_key: [u8; 32],
    pub mac_key: [u8; 32],

    // QES V3 oddělené subklíče.
    pub pre_otp_key: [u8; 32],
    pub enc_key: [u8; 32],
    pub route_key: [u8; 32],
    pub replicator_key: [u8; 32],
    pub zero_key: [u8; 32],
    pub tag1_mac_key: [u8; 32],
    pub tag1_xof_key: [u8; 32],
    pub tag2_mac_key: [u8; 32],
    pub tag2_xof_key: [u8; 32],
    pub tag3_mac_key: [u8; 32],
    pub meta_bind_key: [u8; 32],
}

impl RouteContext {
    pub fn new(master_seed: [u8; MASTER_SEED_LEN], nonce: Nonce) -> Self {
        let hidden_iv = derive_32(&master_seed, "QES.V3.hidden_iv_from_nonce");
        let route_seed = derive_32(&master_seed, "QES.V3.route_seed_from_nonce");
        let metadata_key = derive_32(&master_seed, "QES.V3.metadata_key_legacy_compat");
        let mac_key = derive_32(&master_seed, "QES.V3.mac_key_legacy_compat");

        let pre_otp_key = derive_32(&master_seed, "QES.V3.PRE.OTP.KEY");
        let enc_key = derive_32(&master_seed, "QES.V3.ENC.PAD.KEY");
        let route_key = derive_32(&master_seed, "QES.V3.ROUTE.KEY");
        let replicator_key = derive_32(&master_seed, "QES.V3.REPLICATOR.KEY");
        let zero_key = derive_32(&master_seed, "QES.V3.ZERO.UNDER.TAG1.KEY");
        let tag1_mac_key = derive_32(&master_seed, "QES.V3.TAG1.MAC.UNDER.XOR.KEY");
        let tag1_xof_key = derive_32(&master_seed, "QES.V3.TAG1.XOF.MASK.KEY");
        let tag2_mac_key = derive_32(&master_seed, "QES.V3.TAG2.MAC.OVER.XOR.KEY");
        let tag2_xof_key = derive_32(&master_seed, "QES.V3.TAG2.XOF.MASK.KEY");
        let tag3_mac_key = derive_32(&master_seed, "QES.V3.TAG3.ASCII.WAVE.SEAL.KEY");
        let meta_bind_key = derive_32(&master_seed, "QES.V3.METADATA.BIND.KEY");

        Self {
            master_seed,
            nonce,
            hidden_iv,
            route_seed,
            metadata_key,
            mac_key,
            pre_otp_key,
            enc_key,
            route_key,
            replicator_key,
            zero_key,
            tag1_mac_key,
            tag1_xof_key,
            tag2_mac_key,
            tag2_xof_key,
            tag3_mac_key,
            meta_bind_key,
        }
    }

    pub fn bytes(&self, label: &str, len: usize) -> Vec<u8> {
        self.xof_cascade_with_key(
            &self.route_key,
            "QES.V3.ROUTE.BYTES",
            label.as_bytes(),
            len,
        )
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

    // Interní XOF kaskáda pro dlouhé OTP-like pady a masky.
    // Teď běží nad BLAKE3 XOF ve třech oddělených doménách.
    // Názvy STAGE1/STAGE2/STAGE3 připravují místo pro pozdější SHAKE256 -> KMAC256 -> BLAKE3.
    pub fn xof_cascade_with_key(
        &self,
        key: &[u8; 32],
        label: &str,
        context: &[u8],
        len: usize,
    ) -> Vec<u8> {
        let mut stage1 = blake3::Hasher::new_keyed(key);
        stage1.update(b"QES-XOF-CASCADE-V1|STAGE1-SHAKE-SLOT|");
        stage1.update(label.as_bytes());
        stage1.update(b"|");
        stage1.update(&self.nonce);
        stage1.update(b"|");
        stage1.update(&self.hidden_iv);
        stage1.update(b"|");
        stage1.update(context);
        let stage1_hash = stage1.finalize();

        let mut stage2_key = [0u8; 32];
        stage2_key.copy_from_slice(stage1_hash.as_bytes());

        let mut stage2 = blake3::Hasher::new_keyed(&stage2_key);
        stage2.update(b"QES-XOF-CASCADE-V1|STAGE2-KMAC-SLOT|");
        stage2.update(label.as_bytes());
        stage2.update(b"|");
        stage2.update(&self.route_seed);
        stage2.update(b"|");
        stage2.update(&self.master_seed);
        stage2.update(b"|");
        stage2.update(context);
        let stage2_hash = stage2.finalize();

        let mut stage3_key = [0u8; 32];
        stage3_key.copy_from_slice(stage2_hash.as_bytes());

        let mut stage3 = blake3::Hasher::new_keyed(&stage3_key);
        stage3.update(b"QES-XOF-CASCADE-V1|STAGE3-BLAKE3-XOF|");
        stage3.update(label.as_bytes());
        stage3.update(b"|");
        stage3.update(&self.nonce);
        stage3.update(b"|");
        stage3.update(&self.hidden_iv);
        stage3.update(b"|");
        stage3.update(context);

        let mut reader = stage3.finalize_xof();
        let mut out = vec![0u8; len];
        reader.fill(&mut out);
        out
    }

    pub fn pre_core_pad(&self, len: usize) -> Vec<u8> {
        self.xof_cascade_with_key(
            &self.pre_otp_key,
            "QES.V3.PRE_CORE_XOR_OTP_PAD",
            b"plaintext-before-diffusion-permutation-superposition",
            len,
        )
    }

    pub fn apply_pre_core_otp(&self, data: &mut [u8]) {
        if data.is_empty() {
            return;
        }

        let pad = self.pre_core_pad(data.len());
        for (b, k) in data.iter_mut().zip(pad.iter()) {
            *b ^= *k;
        }
    }

    fn mac_with_key(
        &self,
        key: &[u8; 32],
        domain: &[u8],
        label: &str,
        parts: &[&[u8]],
        len: usize,
    ) -> Vec<u8> {
        let mut hasher = blake3::Hasher::new_keyed(key);
        hasher.update(domain);
        hasher.update(label.as_bytes());
        hasher.update(b"|");
        hasher.update(&self.nonce);
        hasher.update(b"|");
        hasher.update(&self.hidden_iv);

        for part in parts {
            hasher.update(&(part.len() as u64).to_le_bytes());
            hasher.update(part);
        }

        let mut reader = hasher.finalize_xof();
        let mut out = vec![0u8; len];
        reader.fill(&mut out);
        out
    }

    // MAC s oddělenými subklíči podle labelu.
    pub fn mac(&self, label: &str, parts: &[&[u8]], len: usize) -> Vec<u8> {
        let upper = label.to_ascii_uppercase();

        if upper.contains("TAG1") {
            return self.mac_with_key(
                &self.tag1_mac_key,
                b"QES-TAG1-MAC-V3|",
                label,
                parts,
                len,
            );
        }

        if upper.contains("TAG2") {
            return self.mac_with_key(
                &self.tag2_mac_key,
                b"QES-TAG2-MAC-V3|",
                label,
                parts,
                len,
            );
        }

        if upper.contains("TAG3") || upper.contains("SEAL") {
            return self.mac_with_key(
                &self.tag3_mac_key,
                b"QES-TAG3-ASCII-SEAL-MAC-V3|",
                label,
                parts,
                len,
            );
        }

        self.mac_with_key(
            &self.mac_key,
            b"QES-MAC-LEGACY-COMPAT-V3|",
            label,
            parts,
            len,
        )
    }

    pub fn tag1_mask(&self, tag: &[u8], len: usize) -> Vec<u8> {
        self.xof_cascade_with_key(
            &self.tag1_xof_key,
            "QES.V3.TAG1.XOF.MASK",
            tag,
            len,
        )
    }

    pub fn tag2_mask(&self, tag: &[u8], len: usize) -> Vec<u8> {
        self.xof_cascade_with_key(
            &self.tag2_xof_key,
            "QES.V3.TAG2.XOF.MASK",
            tag,
            len,
        )
    }

    // Skrytí / odemčení ZERO metadat. Stejná operace slouží oběma směry.
    pub fn mask_metadata(&self, data: &[u8]) -> Vec<u8> {
        let stream = self.xof_cascade_with_key(
            &self.zero_key,
            "QES.V3.ZERO.UNDER.TAG1.METADATA.MASK",
            b"hidden-zero-metadata",
            data.len().max(1),
        );

        data.iter()
            .enumerate()
            .map(|(i, b)| b ^ stream[i % stream.len()])
            .collect()
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
