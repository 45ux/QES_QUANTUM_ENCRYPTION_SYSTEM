# QES work branch: fix Rust crate name mismatch

Base branch: `qes-work-2-metaqes7`
Base commit: `b4ef7ad4cd28558780f8ae01b63aa0fa0c6345f4`

Problem observed in Termux:

`cargo test` fails with E0433 because `src/main.rs` and integration tests import `qes_quantum_encryption_system`, while the local package/library currently compiles as `qes_core`.

This is a Rust crate name mismatch, not a METAQES7 algorithm failure.

Planned fix:
- either update import paths from `qes_quantum_encryption_system::...` to the actual crate name;
- or set the library crate name in Cargo.toml so old imports resolve.

Preferred minimal fix:
- inspect `rust-core/Cargo.toml`;
- if package name is `qes_core`, add or correct `[lib] name = "qes_quantum_encryption_system"` only if compatible with Android/JNI build;
- otherwise rewrite imports in `src/main.rs` and tests to `qes_core::...`.

No secrets are stored here.
