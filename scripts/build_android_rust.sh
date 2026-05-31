#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR/rust-core"

echo "Budování QES Rust core pro Android arm64-v8a..."
echo "Rust core složka: $(pwd)"

rustup target add aarch64-linux-android

cargo ndk \
  --target arm64-v8a \
  --platform 26 \
  --output-dir "$ROOT_DIR/android-app/app/src/main/jniLibs" \
  build --release --lib

echo "Hotovo. Rust knihovna:"
find "$ROOT_DIR/android-app/app/src/main/jniLibs" -type f -name "*.so" -print
