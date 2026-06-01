# PATCH PLAN: METAQES7 / ZERO_UNDER_TAG1

Stav: WORK IN PROGRESS

## 001 — METAQES7 metadata

Soubor:

```text
rust-core/src/cipher.rs
```

Změna:
- `build_metadata()` emituje `METAQES7`;
- metadata obsahují `zero_model=ZERO_UNDER_TAG1`;
- `verify_metadata()` přijímá `METAQCS6` i `METAQES7`;
- ověřuje `pre_core_otp=on`, `tag1_xof_mask=on`, `tag2_xof_mask=on`.

Stav: APPLIED IN WORK BRANCH

## 002 — Rust crate name fix

Soubor:

```text
rust-core/Cargo.toml
```

Změna:
- `[lib] name` je sjednocený s importy v `src/main.rs` a `tests/`.

Stav: APPLIED IN WORK BRANCH

## 003 — Fast Termux tests

Soubor:

```text
rust-core/src/cipher.rs
rust-core/src/server.rs
```

Změna:
- dlouhé diagnostické testy budou označené jako `#[ignore]`;
- rychlé roundtrip testy zůstanou běžné;
- odstraní se nepoužitý import v `server.rs`, pokud tam stále je.

Stav: PLANNED

## 004 — ASCII-only hidden format

Soubor:

```text
rust-core/src/ascii_art.rs
rust-core/src/cipher.rs
```

Změna:
- dočasný legacy viditelný `ZERO:` / `SEAL:` formát bude nahrazen skrytým slot modelem;
- venku má být vidět pouze ASCII art;
- kompatibilita starých balíčků musí zůstat zachovaná.

Stav: NOT STARTED
