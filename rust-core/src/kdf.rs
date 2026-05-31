use anyhow::{anyhow, Result};
use argon2::{Algorithm, Argon2, Params, Version};
use std::fs::File;
use std::io::Read;

pub const MASTER_SEED_LEN: usize = 64;
pub type Nonce = [u8; 32];

#[derive(Clone, Debug)]
pub struct Seeds {
    pub seed1: String,
    pub seed2: String,
    pub seed3: String,
    pub seed4: String,
}

impl Seeds {
    pub fn validate(&self) -> Result<()> {
        for (name, value) in [
            ("seed1", &self.seed1),
            ("seed2", &self.seed2),
            ("seed3", &self.seed3),
            ("seed4", &self.seed4),
        ] {
            if value.chars().count() < 4 {
                return Err(anyhow!("{} musí mít minimálně 4 znaky", name));
            }
        }
        Ok(())
    }

    fn salt_base(&self, domain: &[u8]) -> Vec<u8> {
        let mut salt = Vec::new();
        salt.extend_from_slice(b"QCS-ASCII-ART-SALT-v6|");
        salt.extend_from_slice(domain);
        salt.extend_from_slice(b"|");
        salt.extend_from_slice(self.seed1.as_bytes());
        salt.extend_from_slice(b"|");
        salt.extend_from_slice(self.seed2.as_bytes());
        salt.extend_from_slice(b"|");
        salt.extend_from_slice(self.seed3.as_bytes());
        salt.extend_from_slice(b"|");
        salt.extend_from_slice(self.seed4.as_bytes());
        salt
    }

    pub fn header_salt_bytes(&self) -> Vec<u8> {
        self.salt_base(b"header-key-hidden-nonce")
    }

    pub fn main_salt_bytes(&self, nonce: &Nonce) -> Vec<u8> {
        let mut salt = self.salt_base(b"main-key-with-hidden-nonce");
        salt.extend_from_slice(b"|");
        salt.extend_from_slice(nonce);
        salt
    }
}

fn argon2id_seed(password: &str, salt: &[u8], label: &str) -> Result<[u8; MASTER_SEED_LEN]> {
    if password.is_empty() {
        return Err(anyhow!("password nesmí být prázdný"));
    }

    // Mobilní/Termux profil: 64 MiB paměti, 3 průchody, 1 vlákno.
    // V produkčním režimu lze zvednout paměť nebo průchody.
    let params = Params::new(64 * 1024, 3, 1, Some(MASTER_SEED_LEN))
        .map_err(|e| anyhow!("Argon2 Params chyba: {:?}", e))?;
    let argon2 = Argon2::new(Algorithm::Argon2id, Version::V0x13, params);

    let mut out = [0u8; MASTER_SEED_LEN];
    argon2
        .hash_password_into(password.as_bytes(), salt, &mut out)
        .map_err(|e| anyhow!("Argon2id {label} chyba: {:?}", e))?;
    Ok(out)
}

/// Fáze 1: klíč jen pro skrytou ZERO hlavičku. Z něj se odemaskuje nonce.
pub fn header_seed(password: &str, seeds: &Seeds) -> Result<[u8; MASTER_SEED_LEN]> {
    seeds.validate()?;
    argon2id_seed(password, &seeds.header_salt_bytes(), "header")
}

/// Fáze 2: hlavní master seed. Do soli už vstupuje nonce, takže stejné heslo + stejný text
/// dá pokaždé jiný výstup.
pub fn master_seed(password: &str, seeds: &Seeds, nonce: &Nonce) -> Result<[u8; MASTER_SEED_LEN]> {
    seeds.validate()?;
    argon2id_seed(password, &seeds.main_salt_bytes(nonce), "main")
}

/// Náhodný nonce pro jeden konkrétní QCS soubor. V Termuxu je /dev/urandom dostupný.
pub fn random_nonce() -> Result<Nonce> {
    let mut nonce = [0u8; 32];
    let mut f = File::open("/dev/urandom")
        .map_err(|e| anyhow!("nejde otevřít /dev/urandom pro nonce: {e}"))?;
    f.read_exact(&mut nonce)
        .map_err(|e| anyhow!("nejde načíst náhodný nonce: {e}"))?;
    Ok(nonce)
}

pub fn derive_bytes(master_seed: &[u8; MASTER_SEED_LEN], label: &str, len: usize) -> Vec<u8> {
    let mut key = [0u8; 32];
    key.copy_from_slice(&master_seed[..32]);

    let mut hasher = blake3::Hasher::new_keyed(&key);
    hasher.update(b"QCS-DERIVE-v6|");
    hasher.update(label.as_bytes());
    hasher.update(b"|");
    hasher.update(&master_seed[32..]);

    let mut reader = hasher.finalize_xof();
    let mut out = vec![0u8; len];
    reader.fill(&mut out);
    out
}

pub fn derive_32(master_seed: &[u8; MASTER_SEED_LEN], label: &str) -> [u8; 32] {
    let bytes = derive_bytes(master_seed, label, 32);
    let mut out = [0u8; 32];
    out.copy_from_slice(&bytes);
    out
}
