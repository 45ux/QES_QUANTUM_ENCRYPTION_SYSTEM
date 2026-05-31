# QES v9 Android Native Starter

QES – Quantum Encryption System, první krok k plnohodnotné Android APK.

Tento balík obsahuje:

- `rust-core/` – aktuální QES Rust jádro,
- `android-app/` – nativní Android aplikace bez serveru a bez WebView,
- `scripts/` – build skripty pro Rust knihovnu a APK,
- `docs/` – architektura a roadmapa.

## Stav

Toto je startovací vývojová verze. Cílem je převést QES z Termux lokálního serveru na normální Android aplikaci s ikonou.

## Spuštění Android buildu

Viz:

```text
docs/BUILD_ANDROID.md
```

## Termux/web verze

Původní Termux/serverová verze zůstává v `rust-core/` a stále jde spouštět přes:

```bash
cd rust-core
cargo run --release
```

## Bezpečnostní poznámka

QES je prototyp. Testy ověřují funkčnost vrstev, roundtrip, MAC/TAG a chování při změnách. Nejde o formální kryptografický důkaz ani auditovaný bezpečnostní produkt.
