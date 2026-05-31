use anyhow::{anyhow, Result};
use base64::{engine::general_purpose::STANDARD as B64, Engine as _};
use std::env;
use std::fs;

use qes_quantum_encryption_system::ascii_art::Particle;
use qes_quantum_encryption_system::capsule::{check_text, create_capsule, verify_capsule_b64, QesMode};
use qes_quantum_encryption_system::cipher::{decrypt_from_ascii_art, encrypt_to_ascii_art, QcsParams};

fn main() -> Result<()> {
    let args: Vec<String> = env::args().collect();
    match args.get(1).map(|s| s.as_str()) {
        None | Some("server") => qes_quantum_encryption_system::server::run("127.0.0.1:8787"),
        Some("self-test") | Some("test") => {
            let tests = qes_quantum_encryption_system::cipher::run_self_tests();
            let mut ok = true;
            for t in tests {
                println!("{}  {} — {}", if t.ok { "OK" } else { "FAIL" }, t.name, t.detail);
                ok &= t.ok;
            }
            if ok { Ok(()) } else { Err(anyhow!("Některý QES self-test selhal")) }
        }
        Some("encrypt") => cli_encrypt(&args[2..]),
        Some("decrypt") => cli_decrypt(&args[2..]),
        Some("help") | Some("--help") | Some("-h") => { print_help(); Ok(()) }
        Some(other) => Err(anyhow!("Neznámý příkaz: {other}. Použij: qes help")),
    }
}

fn cli_encrypt(args: &[String]) -> Result<()> {
    let input = need_arg(args, "--in")?;
    let output = need_arg(args, "--out")?;
    let capsule_out = need_arg(args, "--capsule")?;
    let params = params_from_args(args)?;
    let data = fs::read(input)?;
    let qes_text = encrypt_to_ascii_art(&data, &params)?;
    fs::write(output, qes_text.as_bytes())?;
    let capsule = create_capsule(qes_text.as_bytes(), QesMode::NormalAscii, data.len(), &params)?;
    let capsule_raw = B64.decode(capsule.capsule_b64.as_bytes())?;
    fs::write(capsule_out, capsule_raw)?;
    let check_path = format!("{output}.qes-check.txt");
    fs::write(&check_path, check_text(output, &capsule))?;
    println!("QES hotovo: {input} -> {output}");
    println!("Kapsle: {capsule_out}");
    println!("Kontrola: {check_path}");
    println!("Public hash: {}", capsule.public_hash_hex);
    println!("Keyed MAC: {}", capsule.keyed_mac_hex);
    Ok(())
}

fn cli_decrypt(args: &[String]) -> Result<()> {
    let input = need_arg(args, "--in")?;
    let output = need_arg(args, "--out")?;
    let capsule_path = get_arg(args, "--capsule");
    let params = params_from_args(args)?;
    let qes_text = fs::read_to_string(input)?;
    if let Some(path) = capsule_path {
        let capsule_bytes = fs::read(path)?;
        let capsule_b64 = if capsule_bytes.len() == 128 { B64.encode(&capsule_bytes) } else { String::from_utf8(capsule_bytes)? };
        verify_capsule_b64(qes_text.as_bytes(), &capsule_b64, QesMode::NormalAscii, &params)?;
        println!("QES kapsle ověřena.");
    } else {
        println!("Pozor: dešifrování bez QES kapsle. Pro praxi používej --capsule.");
    }
    let data = decrypt_from_ascii_art(&qes_text, &params)?;
    fs::write(output, data)?;
    println!("QES dešifrováno: {input} -> {output}");
    Ok(())
}

fn params_from_args(args: &[String]) -> Result<QcsParams> {
    Ok(QcsParams {
        password: need_arg(args, "--password")?.to_string(),
        seed1: need_arg(args, "--seed1")?.to_string(),
        seed2: need_arg(args, "--seed2")?.to_string(),
        seed3: need_arg(args, "--seed3")?.to_string(),
        seed4: need_arg(args, "--seed4")?.to_string(),
        particle: Particle {
            glyph: get_arg(args, "--glyph").unwrap_or("X").to_string(),
            value: get_arg(args, "--value").unwrap_or("77").parse().unwrap_or(77),
            vector: get_arg(args, "--vector").unwrap_or("vektor").to_string(),
            phase: get_arg(args, "--phase").unwrap_or("13").parse().unwrap_or(13),
            amplitude: get_arg(args, "--amplitude").unwrap_or("9").parse().unwrap_or(9),
        },
    })
}

fn need_arg<'a>(args: &'a [String], name: &str) -> Result<&'a str> {
    get_arg(args, name).ok_or_else(|| anyhow!("Chybí argument {name}"))
}

fn get_arg<'a>(args: &'a [String], name: &str) -> Option<&'a str> {
    args.windows(2).find(|w| w[0] == name).map(|w| w[1].as_str())
}

fn print_help() {
    println!(r#"QES – Quantum Encryption System

Server pro mobil / Tails lokálně:
  qes server

Self-test:
  qes self-test

Šifrování souboru:
  qes encrypt --in tajne.pdf --out tajne.qes --capsule tajne.qes128 \
    --password heslo --seed1 seed-111 --seed2 seed-222 --seed3 seed-333 --seed4 seed-444

Dešifrování souboru:
  qes decrypt --in tajne.qes --capsule tajne.qes128 --out obnovene.pdf \
    --password heslo --seed1 seed-111 --seed2 seed-222 --seed3 seed-333 --seed4 seed-444

Particle volitelně:
  --glyph X --value 77 --vector vektor --phase 13 --amplitude 9
"#);
}
