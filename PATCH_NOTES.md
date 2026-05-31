# PATCH NOTES FOR AI

## Před dalším patchem
Nejdřív zkontrolovat:
- android-app/app/src/main/java/org/zero/qes/MainActivity.java
- android-app/app/src/main/java/org/zero/qes/QesNative.java
- rust-core/src/android_ffi.rs
- rust-core/src/cipher.rs
- rust-core/src/capsule.rs
- rust-core/src/carrier.rs

## Důležitá pravidla
- Nepřidávat falešnou funkčnost.
- AES uvádět jako plán, dokud není napojený.
- Kompresi uvádět jako plán, dokud není napojená.
- Vault zatím jen roadmap.
- UI má být ostré, kontrastní, symetrické.
- Nepoužívat kulaté prvky jako hlavní styl.
- Nepřidávat generované obrázky.
- Nepřidávat GitHub tokeny do souborů.
