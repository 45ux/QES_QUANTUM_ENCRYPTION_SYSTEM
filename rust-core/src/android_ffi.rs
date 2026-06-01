//! Android JNI bridge for QES.
//!
//! Native Android bridge for QES Rust core.
//! This file must compile for Android target as cdylib.

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
    env.convert_byte_array(&input)
        .map_err(|e| format!("JNI byte array chyba: {e}"))
}

fn make_bytes(env: &mut JNIEnv, bytes: &[u8]) -> jbyteArray {
    match env.byte_array_from_slice(bytes) {
        Ok(arr) => arr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[allow(clippy::too_many_arguments)]
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
    let glyph_string = glyph_s.chars().next().unwrap_or('X').to_string();

    Ok(QcsParams {
        password: get_string(env, password)?,
        seed1: get_string(env, seed1)?,
        seed2: get_string(env, seed2)?,
        seed3: get_string(env, seed3)?,
        seed4: get_string(env, seed4)?,
        particle: Particle {
            glyph: glyph_string,
            value: value.clamp(0, 255) as u8,
            vector: get_string(env, vector)?,
            phase: phase.max(0) as u64,
            amplitude: amplitude.clamp(0, 255) as u8,
        },
    })
}

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
    let result: Result<String, String> = (|| {
        let data = get_string(&mut env, plaintext)?.into_bytes();
        let params = build_params(
            &mut env,
            password,
            seed1,
            seed2,
            seed3,
            seed4,
            glyph,
            value,
            vector,
            phase,
            amplitude,
        )?;
        encrypt_to_ascii_art(&data, &params).map_err(|e| e.to_string())
    })();

    match result {
        Ok(out) => make_string(&mut env, &out),
        Err(e) => make_string(&mut env, &format!("QES_ERROR: {e}")),
    }
}

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
    let result: Result<String, String> = (|| {
        let package = get_string(&mut env, package_text)?;
        let params = build_params(
            &mut env,
            password,
            seed1,
            seed2,
            seed3,
            seed4,
            glyph,
            value,
            vector,
            phase,
            amplitude,
        )?;
        let bytes = decrypt_from_ascii_art(&package, &params).map_err(|e| e.to_string())?;
        String::from_utf8(bytes)
            .map_err(|_| "výstup není platný UTF-8 text; použij dešifrování souboru".to_string())
    })();

    match result {
        Ok(out) => make_string(&mut env, &out),
        Err(e) => make_string(&mut env, &format!("QES_ERROR: {e}")),
    }
}

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
    let result: Result<Vec<u8>, String> = (|| {
        let input_data = get_bytes(&mut env, data)?;
        let params = build_params(
            &mut env,
            password,
            seed1,
            seed2,
            seed3,
            seed4,
            glyph,
            value,
            vector,
            phase,
            amplitude,
        )?;
        let package = encrypt_to_ascii_art(&input_data, &params).map_err(|e| e.to_string())?;
        Ok(package.into_bytes())
    })();

    match result {
        Ok(bytes) => make_bytes(&mut env, &bytes),
        Err(e) => make_bytes(&mut env, format!("QES_ERROR: {e}").as_bytes()),
    }
}

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
    let result: Result<Vec<u8>, String> = (|| {
        let input_package_bytes = get_bytes(&mut env, package_bytes)?;
        let package = String::from_utf8(input_package_bytes)
            .map_err(|_| "QES balík není UTF-8 ASCII art".to_string())?;
        let params = build_params(
            &mut env,
            password,
            seed1,
            seed2,
            seed3,
            seed4,
            glyph,
            value,
            vector,
            phase,
            amplitude,
        )?;
        decrypt_from_ascii_art(&package, &params).map_err(|e| e.to_string())
    })();

    match result {
        Ok(bytes) => make_bytes(&mut env, &bytes),
        Err(e) => make_bytes(&mut env, format!("QES_ERROR: {e}").as_bytes()),
    }
}


#[allow(clippy::too_many_arguments)]
fn build_stream_params(
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
    stream_mode: JString,
    block_index: i64,
) -> Result<QcsParams, String> {
    let password_s = get_string(env, password)?;
    let seed1_s = get_string(env, seed1)?;
    let seed2_s = get_string(env, seed2)?;
    let seed3_s = get_string(env, seed3)?;
    let seed4_s = get_string(env, seed4)?;
    let glyph_s = get_string(env, glyph)?;
    let vector_s = get_string(env, vector)?;
    let mode_s = get_string(env, stream_mode)?;
    let safe_index = if block_index < 0 { 0 } else { block_index };

    let glyph_string = glyph_s.chars().next().unwrap_or('X').to_string();

    Ok(QcsParams {
        password: password_s,
        seed1: seed1_s,
        seed2: seed2_s,
        seed3: seed3_s,
        seed4: format!("{seed4_s}|QES-STREAM-BLOCK|{mode_s}|{safe_index}"),
        particle: Particle {
            glyph: glyph_string,
            value: value.clamp(0, 255) as u8,
            vector: vector_s,
            phase: phase.max(0) as u64,
            amplitude: amplitude.clamp(0, 255) as u8,
        },
    })
}

#[no_mangle]
pub extern "system" fn Java_org_zero_qes_QesNative_encryptStreamBlock(
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
    stream_mode: JString,
    block_index: i64,
) -> jbyteArray {
    let result: Result<Vec<u8>, String> = (|| {
        let input_data = get_bytes(&mut env, data)?;
        let params = build_stream_params(
            &mut env,
            password,
            seed1,
            seed2,
            seed3,
            seed4,
            glyph,
            value,
            vector,
            phase,
            amplitude,
            stream_mode,
            block_index,
        )?;
        let package = encrypt_to_ascii_art(&input_data, &params).map_err(|e| e.to_string())?;
        Ok(package.into_bytes())
    })();

    match result {
        Ok(bytes) => make_bytes(&mut env, &bytes),
        Err(e) => make_bytes(&mut env, format!("QES_ERROR: {e}").as_bytes()),
    }
}

#[no_mangle]
pub extern "system" fn Java_org_zero_qes_QesNative_decryptStreamBlock(
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
    stream_mode: JString,
    block_index: i64,
) -> jbyteArray {
    let result: Result<Vec<u8>, String> = (|| {
        let input_package_bytes = get_bytes(&mut env, package_bytes)?;
        let package = String::from_utf8(input_package_bytes)
            .map_err(|_| "QES stream blok není UTF-8 ASCII art".to_string())?;
        let params = build_stream_params(
            &mut env,
            password,
            seed1,
            seed2,
            seed3,
            seed4,
            glyph,
            value,
            vector,
            phase,
            amplitude,
            stream_mode,
            block_index,
        )?;
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
