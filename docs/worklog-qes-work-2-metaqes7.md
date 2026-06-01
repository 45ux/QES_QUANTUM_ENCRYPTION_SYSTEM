# QES work branch: METAQES7 / ZERO_UNDER_TAG1

Branch: `qes-work-2-metaqes7`
Base commit: `2d90dbdb90098f38c2b3c8a9b8f1fe34578c996d`

Purpose:
- keep work away from `main`, so GitHub Actions APK build is not triggered automatically;
- prepare the next Rust core change safely;
- update metadata model from legacy `METAQCS6` toward `METAQES7`;
- represent `ZERO_UNDER_TAG1` as hidden metadata model;
- keep previous savepoint branch `qes-savepoint-1` untouched.

Planned source changes:
1. `rust-core/src/cipher.rs`
   - change `build_metadata` to emit `METAQES7` with `zero_model=ZERO_UNDER_TAG1`;
   - keep compatibility in `verify_metadata` for old `METAQCS6` packages;
   - update diagnostic labels so they no longer describe ZERO/TAG as the final visible model;
   - add a self-test for separated subkeys.

2. `android-app/app/src/main/java/org/zero/qes/MainActivity.java`
   - bump internal alpha version only when source changes are committed.

Safety notes:
- no secrets are stored here;
- this is a work branch note only;
- this branch should be merged into `main` only when the build is intentionally needed.
