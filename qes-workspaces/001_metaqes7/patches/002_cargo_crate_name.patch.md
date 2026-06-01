# Patch 002 — Cargo crate name

Status: APPLIED IN WORK BRANCH

Problém:
`cargo test` hlásil chybu `E0433`, protože kód importoval crate:

```text
qes_quantum_encryption_system
```

ale knihovna byla v `Cargo.toml` pojmenovaná jako:

```text
qes_core
```

Dotčený soubor:

```text
rust-core/Cargo.toml
```

Změna:

```text
[lib]
name = "qes_quantum_encryption_system"
```

Výsledek:
- `src/main.rs` najde správnou knihovnu;
- integrační testy najdou správnou knihovnu;
- Android/JNI `cdylib` crate-type zůstává zachovaný.

Poznámka:
Toto není změna šifry. Je to oprava názvu Rust knihovny/importů.
