#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../rust-core"
if ! command -v cargo-ndk >/dev/null 2>&1; then
  echo "Chybí cargo-ndk. Nainstaluj: cargo install cargo-ndk"
  exit 1
fi
rustup target add aarch64-linux-android
cargo ndk -t arm64-v8a -o ../android-app/app/src/main/jniLibs build --release
