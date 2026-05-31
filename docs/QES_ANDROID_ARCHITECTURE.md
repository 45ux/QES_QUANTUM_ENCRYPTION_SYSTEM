# QES Android Native Architecture

Tato verze začíná převod QES z Termux/web serveru na plnohodnotnou Android aplikaci.

## Cíl

- žádný lokální server,
- žádný ruční odkaz `127.0.0.1`,
- normální ikona aplikace,
- nativní Android UI,
- šifrovací jádro v Rustu,
- později stejný Rust core pro Tails/Linux CLI.

## Vrstvy

```text
Android Activity / UI
        │
        ▼
Kotlin QesNative
        │ JNI
        ▼
Rust qes_core
        │
        ▼
QES Core / ASCII carrier / kapsle / MAC
```

## Co je hotové v tomto starteru

- Android projekt `android-app`,
- Kotlin třída `QesNative`,
- nativní Activity bez WebView,
- Rust JNI bridge `android_ffi.rs`,
- build skripty pro `cargo-ndk`,
- napojení textového šifrování/dešifrování.

## Co bude další krok

1. systémový výběr souborů,
2. ukládání `.qes`, `.qes128` a kontrolního `.txt`,
3. QES-128 kapsle v Android UI,
4. hash/MAC karta,
5. Adaptive Labyrinth Cover,
6. streamové zpracování velkých souborů.

## Poznámka

Toto je prototyp plnohodnotné APK architektury. Není to ještě finální bezpečnostní produkt a neobsahuje hotový audit.
