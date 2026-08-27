#!/usr/bin/env bash
# Build debug APK → install on connected device → run formalized device regression.
# Skip regression: SKIP_DEVICE_REGRESSION=1
set -euo pipefail
cd "$(dirname "$0")/.."

JDK_HOME="${ORG_GRADLE_JAVA_HOME:-$HOME/.jdks/jdk-17.0.20+8/Contents/Home}"
if [[ -d "$JDK_HOME/bin" ]]; then
  export PATH="$JDK_HOME/bin:$PATH"
  export JAVA_HOME="$JDK_HOME"
fi
export DEVELOPER_DIR="${DEVELOPER_DIR:-/Library/Developer/CommandLineTools}"

echo "==> assembleDebug"
./gradlew :androidApp:assembleDebug

if [[ "${SKIP_DEVICE_REGRESSION:-0}" == "1" ]]; then
  echo "SKIP_DEVICE_REGRESSION=1 — install/regression skipped"
  echo "APK: androidApp/build/outputs/apk/debug/androidApp-debug.apk"
  exit 0
fi

# Require a device
if ! adb devices | awk 'NR>1 && $2=="device" {found=1} END{exit !found}'; then
  echo "ERROR: No adb device in 'device' state. Connect USB debugging or set SKIP_DEVICE_REGRESSION=1." >&2
  exit 1
fi

echo "==> device regression (build already done; install+test)"
REGRESSION_BUILD=0 REGRESSION_INSTALL=1 \
  ./scripts/device_regression_test.sh

echo "==> done. History: docs/08-implementation/device-regression-history.md"
