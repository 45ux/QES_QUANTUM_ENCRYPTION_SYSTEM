# Build Android APK

## Varianta A: Android Studio

1. Otevři složku `android-app` v Android Studiu.
2. Nainstaluj Android NDK.
3. V terminálu projektu spusť:

```bash
./scripts/build_android_rust.sh
```

4. V Android Studiu spusť `app` na telefonu nebo vytvoř APK.

## Varianta B: příkazová řádka

Požadavky:

```bash
rustup target add aarch64-linux-android
cargo install cargo-ndk
```

Build Rust knihovny:

```bash
./scripts/build_android_rust.sh
```

Build APK:

```bash
cd android-app
gradle assembleDebug
```

APK bude obvykle zde:

```text
android-app/app/build/outputs/apk/debug/app-debug.apk
```

## Důležité

Soubor `libqes_core.so` musí být v:

```text
android-app/app/src/main/jniLibs/arm64-v8a/libqes_core.so
```

Jinak aplikace zobrazí chybu, že Rust knihovna není vložena.
