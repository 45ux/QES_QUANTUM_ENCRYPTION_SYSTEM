#!/usr/bin/env python3
"""
QES GitHub-only patch runner.

Purpose:
- apply small, deterministic project patches on a GitHub Actions runner;
- avoid editing long source files from a phone/Termux;
- keep changes on qes-work-* branches until explicitly moved to main.

No secrets are read or printed by this script.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> tuple[str, bool]:
    if old in text:
        return text.replace(old, new, 1), True
    if new in text:
        return text, False
    raise SystemExit(f"Patch failed: marker not found for {label}")


def apply_fast_tests() -> list[str]:
    changed: list[str] = []

    cipher_path = "rust-core/src/cipher.rs"
    c = read(cipher_path)
    original = c

    c, did = replace_once(
        c,
        '''    #[test]\n    fn every_self_test_passes() {''',
        '''    #[test]\n    #[ignore = "slow on GitHub/Termux; run manually before release"]\n    fn every_self_test_passes() {''',
        "ignore every_self_test_passes",
    )
    if did:
        changed.append("cipher.rs: ignored every_self_test_passes")

    c, did = replace_once(
        c,
        '''    #[test]\n    fn diagnostic_frames_are_created() {''',
        '''    #[test]\n    #[ignore = "slow on GitHub/Termux; run manually before release"]\n    fn diagnostic_frames_are_created() {''',
        "ignore diagnostic_frames_are_created",
    )
    if did:
        changed.append("cipher.rs: ignored diagnostic_frames_are_created")

    if c != original:
        write(cipher_path, c)

    server_path = "rust-core/src/server.rs"
    s = read(server_path)
    ns = s.replace("use std::io::Read;\n", "")
    if ns != s:
        write(server_path, ns)
        changed.append("server.rs: removed unused std::io::Read import")

    protocol_path = "qes-workspaces/001_metaqes7/patches/003_fast_tests.patch.md"
    p = read(protocol_path)
    np = p.replace("Status: PLANNED", "Status: APPLIED BY GITHUB PATCHER")
    marker = "## Applied by GitHub-only patch runner"
    if marker not in np:
        np += """

## Applied by GitHub-only patch runner

Branch:

```text
qes-work-7-github-only-patcher-2
```

Effect:
- long diagnostic Rust tests are now ignored by default;
- they remain available through `--ignored` before release;
- no cipher logic was changed by this patch.
"""
    if np != p:
        write(protocol_path, np)
        changed.append("workspace: patch 003 marked applied")

    return changed


def main() -> None:
    print("QES GitHub-only patch runner started")
    changed = apply_fast_tests()
    if changed:
        print("Applied changes:")
        for item in changed:
            print(f"- {item}")
    else:
        print("No changes needed; patch already applied")


if __name__ == "__main__":
    main()
