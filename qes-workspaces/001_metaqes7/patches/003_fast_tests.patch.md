# Patch 003 — Fast Termux tests

Status: APPLIED BY GITHUB PATCHER

Cíl:
Na mobilu v Termuxu nemají běžet dlouhé diagnostické testy automaticky, protože mohou viset přes 60 sekund a zbytečně vybíjet telefon.

Dotčené soubory:

```text
rust-core/src/cipher.rs
rust-core/src/server.rs
```

Plánovaná změna:
- `every_self_test_passes` označit jako `#[ignore]`;
- `diagnostic_frames_are_created` označit jako `#[ignore]`;
- rychlé roundtrip testy ponechat aktivní;
- odstranit nepoužitý import `std::io::Read` v `server.rs`, pokud tam stále je.

Rychlé testy:

```bash
cargo test --lib cipher::tests::core_roundtrip -- --nocapture
cargo test --lib cipher::tests::ascii_roundtrip -- --nocapture
```

Ruční dlouhé testy před release:

```bash
cargo test every_self_test_passes -- --ignored --nocapture
cargo test diagnostic_frames_are_created -- --ignored --nocapture
```

Poznámka:
Toto není změna šifrování. Je to pouze úprava testovacího režimu pro mobilní Termux workflow.


## Applied by GitHub-only patch runner

Branch:

```text
qes-work-7-github-only-patcher-2
```

Effect:
- long diagnostic Rust tests are now ignored by default;
- they remain available through `--ignored` before release;
- no cipher logic was changed by this patch.
