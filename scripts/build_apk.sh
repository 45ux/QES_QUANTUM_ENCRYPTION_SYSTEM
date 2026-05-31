#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/build_android_rust.sh
cd android-app
if [ -x ./gradlew ]; then
  ./gradlew assembleDebug
else
  gradle assembleDebug
fi
