#!/data/data/com.termux/files/usr/bin/bash
set -e
cd "$(dirname "$0")/.."
echo "=== QES PROJECT STATUS ==="
echo "PWD: $(pwd)"
echo
echo "--- Git ---"
git status --short || true
echo
echo "--- Files ---"
find . -maxdepth 3 -type f | sort | sed 's#^\./##' | head -n 120
echo
echo "--- Rust tests ---"
if [ -f Cargo.toml ]; then
  cargo test --release
elif [ -f rust-core/Cargo.toml ]; then
  cd rust-core && cargo test --release
else
  echo "Cargo.toml nenalezen."
fi
