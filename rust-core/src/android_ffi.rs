//! Android JNI bridge for QES.
//!
//! This module exposes the existing Rust QES core to a native Android APK.
//! It is compiled only for Android targets and loaded from Kotlin through
//! `System.loadLibrary("qes_core")`.

use crate::ascii_art::Particle;
use crate::cipher::{decrypt_from_ascii_art, encrypt_to_ascii_art, QcsParams};
use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jbyteArray, jstring};
use jni::JNIEnv;

fn make_string(env: &mut JNIEnv, value: &str) -> jstring {
    match env.new_string(value) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

fn get_string(env: &mut JNIEnv, input: JString) -> Result<String, String> {
    env.get_string(&input)
        .map(|s| s.to_string_lossy().into_owned())
        .map_err(|e| format!("JNI string chyba: {e}"))
}

fn get_bytes(env: &mut JNIEnv, input: JByteArray) -> Result<Vec<u8>, String> {
    env.convert_byte_array(input)
        .map_err(|e| format!("JNI byte array chyba: {e}"))
}

fn make_bytes(env: &mut JNIEnv, bytes: &[u8]) -> jbyteArray {
    match env.byte_array_from_slice(bytes) {
        Ok(arr) => arr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

fn build_params(
    env: &mut JNIEnv,
    password: JString,
    seed1: JString,
    seed2: JString,
    seed3: JString,
    seed4: JString,
    glyph: JString,
    value: i32,
    vector: JString,
    phase: i64,
    amplitude: i32,
) -> Result<QcsParams, String> {
    let glyph_s = get_string(env, glyph)?;
    let mut chars = glyph_s.chars();
    let glyph = chars.next().unwrap_or('X');

    Ok(QcsParams {
        password: get_string(env, password)?,
        seed1: get_string(env, seed1)?,
        seed2: get_string(env, seed2)?,
        seed3: get_string(env, seed3)?,
        seed4: get_string(env, seed4)?,
        particle: Particle {
            glyph,
            value: value.clamp(0, 255) as u8,
            vector: get_string(env, vector)?,
            phase: phase.max(0) as u64,
            amplitude: amplitude.clamp(0, 255) as u8,
        },
    })
}

/// Kotlin signature:
/// external fun encryptText(plaintext: String, password: String, seed1: String, seed2: String, seed3: String, seed4: String, glyph: String, value: Int, vector: String, phase: Long, amplitude: Int): String
#[no_mangle]
pub extern "system" fn Java_org_zero_qes_QesNative_encryptText(
    mut env: JNIEnv,
    _class: JClass,
    plaintext: JString,
    password: JString,
    seed1: JString,
    seed2: JString,
    seed3: JString,
    seed4: JString,
    glyph: JString,
    value: i32,
    vector: JString,
    phase: i64,
    amplitude: i32,
) -> jstring {
    let result = (|| {
        let data = get_string(&mut env, plaintext)?.into_bytes();
        let params = build_params(&mut env, password, seed1, seed2, seed3, seed4, glyph, value, vector, phase, amplitude)?;
        encrypt_to_ascii_art(&data, &params).map_err(|e| e.to_string())
    })();

    match result {
        Ok(out) => make_string(&mut env, &out),
        Err(e) => make_string(&mut env, &format!("QES_ERROR: {e}")),
    }
}

/// Kotlin signature:
/// external fun decryptText(packageText: String, password: String, seed1: String, seed2: String, seed3: String, seed4: String, glyph: String, value: Int, vector: String, phase: Long, amplitude: Int): String
#[no_mangle]
pub extern "system" fn Java_org_zero_qes_QesNative_decryptText(
    mut env: JNIEnv,
    _class: JClass,
    package_text: JString,
    password: JString,
    seed1: JString,
    seed2: JString,
    seed3: JString,
    seed4: JString,
    glyph: JString,
    value: i32,
    vector: JString,
    phase: i64,
    amplitude: i32,
) -> jstring {
    let result = (|| {
        let package = get_string(&mut env, package_text)?;
        let params = build_params(&mut env, password, seed1, seed2, seed3, seed4, glyph, value, vector, phase, amplitude)?;
        let bytes = decrypt_from_ascii_art(&package, &params).map_err(|e| e.to_string())?;
        String::from_utf8(bytes).map_err(|_| "výstup není platný UTF-8 text; použij dešifrování souboru".to_string())
    })();

    match result {
        Ok(out) => make_string(&mut env, &out),
        Err(e) => make_string(&mut env, &format!("QES_ERROR: {e}")),
    }
}

/// Kotlin signature:
/// external fun encryptBytes(data: ByteArray, ...): ByteArray
#[no_mangle]
pub extern "system" fn Java_org_zero_qes_QesNative_encryptBytes(
    mut env: JNIEnv,
    _class: JClass,
    data: JByteArray,
    password: JString,
    seed1: JString,
    seed2: JString,
    seed3: JString,
    seed4: JString,
    glyph: JString,
    value: i32,
    vector: JString,
    phase: i64,
    amplitude: i32,
) -> jbyteArray {
    let result = (|| {
        let data = get_bytes(&mut env, data)?;
        let params = build_params(&mut env, password, seed1, seed2, seed3, seed4, glyph, value, vector, phase, amplitude)?;
        let package = encrypt_to_ascii_art(&data, &params).map_err(|e| e.to_string())?;
        Ok(package.into_bytes())
    })();

    match result {
        Ok(bytes) => make_bytes(&mut env, &bytes),
        Err(e) => make_bytes(&mut env, format!("QES_ERROR: {e}").as_bytes()),
    }
}

/// Kotlin signature:
/// external fun decryptBytes(packageBytes: ByteArray, ...): ByteArray
#[no_mangle]
pub extern "system" fn Java_org_zero_qes_QesNative_decryptBytes(
    mut env: JNIEnv,
    _class: JClass,
    package_bytes: JByteArray,
    password: JString,
    seed1: JString,
    seed2: JString,
    seed3: JString,
    seed4: JString,
    glyph: JString,
    value: i32,
    vector: JString,
    phase: i64,
    amplitude: i32,
) -> jbyteArray {
    let result = (|| {
        let package_bytes = get_bytes(&mut env, package_bytes)?;
        let package = String::from_utf8(package_bytes).map_err(|_| "QES balík není UTF-8 ASCII art".to_string())?;
        let params = build_params(&mut env, password, seed1, seed2, seed3, seed4, glyph, value, vector, phase, amplitude)?;
        decrypt_from_ascii_art(&package, &params).map_err(|e| e.to_string())
    })();

    match result {
        Ok(bytes) => make_bytes(&mut env, &bytes),
        Err(e) => make_bytes(&mut env, format!("QES_ERROR: {e}").as_bytes()),
    }
}

#[no_mangle]
pub extern "system" fn Java_org_zero_qes_QesNative_selfTest(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    make_string(
        &mut env,
        "QES Android bridge OK. Rust core loaded. Spusť roundtrip v aplikaci.",
    )
}
