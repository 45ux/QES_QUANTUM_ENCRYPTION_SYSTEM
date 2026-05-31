use std::io::Read;
use std::time::Instant;

use anyhow::{anyhow, Result};
use base64::{engine::general_purpose::STANDARD as B64, Engine as _};
use serde::{Deserialize, Serialize};
use tiny_http::{Header, Method, Request, Response, Server, StatusCode};

use crate::carrier::{decrypt_from_carrier_file, encrypt_to_carrier_file, run_carrier_tests};
use crate::capsule::{check_text, create_capsule, keyed_mac_hex, public_hash_hex, verify_capsule_b64, run_capsule_tests, QesMode};
use crate::cipher::{decrypt_from_ascii_art, diagnose_pipeline, encrypt_to_ascii_art, run_self_tests, QcsParams};

const INDEX_HTML: &str = include_str!("../static/index.html");
const APP_JS: &str = include_str!("../static/app.js");
const STYLE_CSS: &str = include_str!("../static/style.css");

#[derive(Debug, Deserialize)]
struct EncryptRequest {
    data_b64: String,
    params: QcsParams,
}

#[derive(Debug, Deserialize)]
struct DecryptRequest {
    ascii_art: String,
    params: QcsParams,
    capsule_b64: Option<String>,
}

#[derive(Debug, Deserialize)]
struct CarrierEncryptRequest {
    data_b64: String,
    cover_b64: String,
    params: QcsParams,
}

#[derive(Debug, Deserialize)]
struct CarrierDecryptRequest {
    carrier_b64: String,
    params: QcsParams,
    capsule_b64: Option<String>,
}

#[derive(Debug, Deserialize)]
struct VerifyRequest {
    payload_b64: String,
    params: QcsParams,
    expected_public_hash: Option<String>,
    expected_keyed_mac: Option<String>,
}

#[derive(Debug, Deserialize)]
struct DiagnoseRequest {
    data_b64: String,
    params: QcsParams,
    include_frames: bool,
}

#[derive(Debug, Serialize)]
struct EncryptResponse {
    ok: bool,
    ascii_art: String,
    input_len: usize,
    encrypted_core_len: usize,
    ascii_art_chars: usize,
    note: String,
    capsule_b64: String,
    capsule_hex: String,
    public_hash_hex: String,
    keyed_mac_hex: String,
    check_text: String,
    elapsed_ms: u128,
    speed_bps: f64,
    speed_kib_s: f64,
    speed_mib_s: f64,
    ascii_chars_per_s: f64,
}

#[derive(Debug, Serialize)]
struct CarrierEncryptResponse {
    ok: bool,
    carrier_b64: String,
    input_len: usize,
    cover_len: usize,
    carrier_len: usize,
    public_hash_hex: String,
    keyed_mac_hex: String,
    note: String,
    capsule_b64: String,
    capsule_hex: String,
    check_text: String,
    elapsed_ms: u128,
    speed_bps: f64,
    speed_kib_s: f64,
    speed_mib_s: f64,
}

#[derive(Debug, Serialize)]
struct DecryptResponse {
    ok: bool,
    data_b64: String,
    output_len: usize,
    utf8_preview: Option<String>,
    elapsed_ms: u128,
    speed_bps: f64,
    speed_kib_s: f64,
    speed_mib_s: f64,
    public_hash_hex: Option<String>,
    keyed_mac_hex: Option<String>,
    note: String,
}

#[derive(Debug, Serialize)]
struct ErrorResponse {
    ok: bool,
    error: String,
}

pub fn run(addr: &str) -> Result<()> {
    let server = Server::http(addr).map_err(|e| anyhow!("Server nejde spustit: {e}"))?;
    println!("QES server běží na http://{addr}");
    println!("V mobilu otevři prohlížeč a zadej: http://{addr}");

    for request in server.incoming_requests() {
        if let Err(e) = handle_request(request) {
            eprintln!("Chyba požadavku: {e}");
        }
    }
    Ok(())
}

fn handle_request(mut request: Request) -> Result<()> {
    let method = request.method().clone();
    let url = request.url().split('?').next().unwrap_or(request.url()).to_string();

    match (method, url.as_str()) {
        (Method::Options, _) => respond_empty(request),
        (Method::Get, "/") | (Method::Get, "/index.html") => respond_text(request, INDEX_HTML, "text/html; charset=utf-8"),
        (Method::Get, "/app.js") => respond_text(request, APP_JS, "application/javascript; charset=utf-8"),
        (Method::Get, "/style.css") => respond_text(request, STYLE_CSS, "text/css; charset=utf-8"),
        (Method::Get, "/api/health") => respond_json_result(request, Ok(serde_json::json!({"ok": true, "name": "qes_quantum_encryption_system", "version": "v8.1-capsule-carrier-mac"}))),
        (Method::Post, "/api/encrypt") => {
            let body = read_body(&mut request)?;
            respond_json_result(request, api_encrypt(&body))
        }
        (Method::Post, "/api/decrypt") => {
            let body = read_body(&mut request)?;
            respond_json_result(request, api_decrypt_ascii(&body))
        }
        (Method::Post, "/api/encrypt-carrier") => {
            let body = read_body(&mut request)?;
            respond_json_result(request, api_encrypt_carrier(&body))
        }
        (Method::Post, "/api/decrypt-carrier") => {
            let body = read_body(&mut request)?;
            respond_json_result(request, api_decrypt_carrier(&body))
        }
        (Method::Post, "/api/verify") => {
            let body = read_body(&mut request)?;
            respond_json_result(request, api_verify(&body))
        }
        (Method::Post, "/api/diagnose") => {
            let body = read_body(&mut request)?;
            respond_json_result(request, api_diagnose(&body))
        }
        (Method::Post, "/api/test") | (Method::Get, "/api/test") => respond_json_result(request, api_test()),
        _ => {
            let response = Response::from_string("404")
                .with_status_code(StatusCode(404))
                .with_header(content_type("text/plain; charset=utf-8"));
            request.respond(response)?;
            Ok(())
        }
    }
}

fn api_encrypt(body: &str) -> Result<serde_json::Value> {
    let req: EncryptRequest = serde_json::from_str(body)?;
    let data = B64.decode(req.data_b64.as_bytes())?;
    let started = Instant::now();
    let ascii_art = encrypt_to_ascii_art(&data, &req.params)?;
    let capsule = create_capsule(ascii_art.as_bytes(), QesMode::NormalAscii, data.len(), &req.params)?;
    let check = check_text("qes_output.qes", &capsule);
    let elapsed = started.elapsed();
    let ascii_art_chars = ascii_art.chars().count();
    let secs = elapsed.as_secs_f64().max(0.000_001);
    let speed_bps = data.len() as f64 / secs;
    Ok(serde_json::to_value(EncryptResponse {
        ok: true,
        ascii_art,
        input_len: data.len(),
        encrypted_core_len: data.len(),
        ascii_art_chars,
        note: "QES normální režim: 1:1 platí pro encrypted_core. ASCII/QES soubor je textový nosič. QES-128 kapsle je samostatný artefakt pro uživatele.".to_string(),
        capsule_b64: capsule.capsule_b64,
        capsule_hex: capsule.capsule_hex,
        public_hash_hex: capsule.public_hash_hex,
        keyed_mac_hex: capsule.keyed_mac_hex,
        check_text: check,
        elapsed_ms: elapsed.as_millis(),
        speed_bps,
        speed_kib_s: speed_bps / 1024.0,
        speed_mib_s: speed_bps / 1024.0 / 1024.0,
        ascii_chars_per_s: ascii_art_chars as f64 / secs,
    })?)
}

fn api_encrypt_carrier(body: &str) -> Result<serde_json::Value> {
    let req: CarrierEncryptRequest = serde_json::from_str(body)?;
    let data = B64.decode(req.data_b64.as_bytes())?;
    let cover = B64.decode(req.cover_b64.as_bytes())?;
    let started = Instant::now();
    let (carrier, info) = encrypt_to_carrier_file(&data, &cover, &req.params)?;
    let capsule = create_capsule(&carrier, QesMode::CarrierAppend, data.len(), &req.params)?;
    let check = check_text("qes_carrier_output.bin", &capsule);
    let elapsed = started.elapsed();
    let secs = elapsed.as_secs_f64().max(0.000_001);
    let speed_bps = data.len() as f64 / secs;
    Ok(serde_json::to_value(CarrierEncryptResponse {
        ok: true,
        carrier_b64: B64.encode(&carrier),
        input_len: data.len(),
        cover_len: info.cover_len,
        carrier_len: info.carrier_len,
        public_hash_hex: capsule.public_hash_hex,
        keyed_mac_hex: capsule.keyed_mac_hex,
        note: "QES v8.1 carrier režim: payload je vložen jako ZERO carrier blok s vnitřním keyed MAC. Změna cover části nebo payload části se odmítne ještě před dešifrováním. Adaptive Labyrinth Cover bude další etapa; kapsle a MAC už jsou oddělené pro uživatele.".to_string(),
        capsule_b64: capsule.capsule_b64,
        capsule_hex: capsule.capsule_hex,
        check_text: check,
        elapsed_ms: elapsed.as_millis(),
        speed_bps,
        speed_kib_s: speed_bps / 1024.0,
        speed_mib_s: speed_bps / 1024.0 / 1024.0,
    })?)
}

fn api_decrypt_ascii(body: &str) -> Result<serde_json::Value> {
    let req: DecryptRequest = serde_json::from_str(body)?;
    if let Some(capsule) = req.capsule_b64.as_ref().filter(|s| !s.trim().is_empty()) {
        verify_capsule_b64(req.ascii_art.as_bytes(), capsule, QesMode::NormalAscii, &req.params)?;
    }
    let started = Instant::now();
    let data = decrypt_from_ascii_art(&req.ascii_art, &req.params)?;
    let note = if req.capsule_b64.as_ref().map(|s| !s.trim().is_empty()).unwrap_or(false) {
        "QES kapsle ověřena. Dešifrováno z normálního QES nosiče.".to_string()
    } else {
        "Dešifrováno bez QES kapsle. Pro praxi používej kapsli pro kontrolu souboru a MAC.".to_string()
    };
    Ok(decrypt_response_value(data, started.elapsed(), None, None, note)?)
}

fn api_decrypt_carrier(body: &str) -> Result<serde_json::Value> {
    let req: CarrierDecryptRequest = serde_json::from_str(body)?;
    let carrier = B64.decode(req.carrier_b64.as_bytes())?;
    if let Some(capsule) = req.capsule_b64.as_ref().filter(|s| !s.trim().is_empty()) {
        verify_capsule_b64(&carrier, capsule, QesMode::CarrierAppend, &req.params)?;
    }
    let started = Instant::now();
    let (data, info) = decrypt_from_carrier_file(&carrier, &req.params)?;
    Ok(decrypt_response_value(data, started.elapsed(), Some(info.public_hash_hex), Some(info.keyed_mac_hex), "QES carrier dešifrován. Pokud byla vložena kapsle, byla před dešifrováním ověřena.".to_string())?)
}

fn decrypt_response_value(data: Vec<u8>, elapsed: std::time::Duration, public_hash: Option<String>, keyed_mac: Option<String>, note: String) -> Result<serde_json::Value> {
    let secs = elapsed.as_secs_f64().max(0.000_001);
    let speed_bps = data.len() as f64 / secs;
    let utf8_preview = String::from_utf8(data.clone()).ok().map(|s| {
        if s.chars().count() > 2000 { s.chars().take(2000).collect::<String>() + "\n…" } else { s }
    });
    Ok(serde_json::to_value(DecryptResponse {
        ok: true,
        data_b64: B64.encode(&data),
        output_len: data.len(),
        utf8_preview,
        elapsed_ms: elapsed.as_millis(),
        speed_bps,
        speed_kib_s: speed_bps / 1024.0,
        speed_mib_s: speed_bps / 1024.0 / 1024.0,
        public_hash_hex: public_hash,
        keyed_mac_hex: keyed_mac,
        note,
    })?)
}

fn api_verify(body: &str) -> Result<serde_json::Value> {
    let req: VerifyRequest = serde_json::from_str(body)?;
    let payload = B64.decode(req.payload_b64.as_bytes())?;
    let public_hash = public_hash_hex(&payload);
    let keyed_mac = keyed_mac_hex(&payload, &req.params)?;
    let public_match = req.expected_public_hash.as_ref().map(|v| normalize_hex(v) == public_hash);
    let mac_match = req.expected_keyed_mac.as_ref().map(|v| normalize_hex(v) == keyed_mac);
    Ok(serde_json::json!({
        "ok": true,
        "public_hash_hex": public_hash,
        "keyed_mac_hex": keyed_mac,
        "public_hash_match": public_match,
        "keyed_mac_match": mac_match,
        "note": "Hash ověří změnu souboru veřejně. Keyed MAC ověří změnu souboru s heslem a seedy."
    }))
}

fn api_diagnose(body: &str) -> Result<serde_json::Value> {
    let req: DiagnoseRequest = serde_json::from_str(body)?;
    let data = B64.decode(req.data_b64.as_bytes())?;
    Ok(serde_json::to_value(diagnose_pipeline(&data, &req.params, req.include_frames)?)?)
}

fn api_test() -> Result<serde_json::Value> {
    let mut tests = run_self_tests();
    tests.extend(run_carrier_tests());
    tests.extend(run_capsule_tests());
    let passed = tests.iter().filter(|t| t.ok).count();
    let failed = tests.len().saturating_sub(passed);
    Ok(serde_json::json!({"ok": true, "all_ok": failed == 0, "passed": passed, "failed": failed, "tests": tests, "note": "Testy ověřují roundtrip, vrstvy, MAC/TAG, lavinový efekt, citlivost na klíč a detekci změn. Nejsou formálním kryptografickým důkazem."}))
}

fn read_body(request: &mut Request) -> Result<String> {
    let mut body = String::new();
    request.as_reader().read_to_string(&mut body)?;
    Ok(body)
}

fn respond_text(request: Request, text: &str, mime: &str) -> Result<()> {
    let response = Response::from_string(text.to_string())
        .with_header(content_type(mime))
        .with_header(header("Cache-Control", "no-store"))
        .with_header(header("Access-Control-Allow-Origin", "*"));
    request.respond(response)?;
    Ok(())
}

fn respond_empty(request: Request) -> Result<()> {
    let response = Response::from_string("").with_status_code(StatusCode(204))
        .with_header(header("Access-Control-Allow-Origin", "*"))
        .with_header(header("Access-Control-Allow-Headers", "Content-Type"))
        .with_header(header("Access-Control-Allow-Methods", "GET, POST, OPTIONS"));
    request.respond(response)?;
    Ok(())
}

fn respond_json_result(request: Request, result: Result<serde_json::Value>) -> Result<()> {
    match result {
        Ok(value) => {
            let response = Response::from_string(serde_json::to_string_pretty(&value)?)
                .with_header(content_type("application/json; charset=utf-8"))
                .with_header(header("Cache-Control", "no-store"))
                .with_header(header("Access-Control-Allow-Origin", "*"));
            request.respond(response)?;
        }
        Err(e) => {
            let error = ErrorResponse { ok: false, error: e.to_string() };
            let response = Response::from_string(serde_json::to_string_pretty(&error)?)
                .with_status_code(StatusCode(400))
                .with_header(content_type("application/json; charset=utf-8"))
                .with_header(header("Cache-Control", "no-store"))
                .with_header(header("Access-Control-Allow-Origin", "*"));
            request.respond(response)?;
        }
    }
    Ok(())
}

fn normalize_hex(s: &str) -> String {
    s.chars().filter(|c| !c.is_whitespace()).flat_map(|c| c.to_lowercase()).collect()
}

fn content_type(value: &str) -> Header { header("Content-Type", value) }

fn header(name: &str, value: &str) -> Header {
    Header::from_bytes(name.as_bytes(), value.as_bytes()).expect("valid header")
}
