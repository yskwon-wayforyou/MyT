#!/usr/bin/env bash
# Build Android debug + (optionally signed) release APK.
# If a device is connected, also installs debug APK and runs device regression
# unless SKIP_DEVICE_REGRESSION=1.
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-$HOME/.jdks/jdk-17.0.20+8/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"
export DEVELOPER_DIR="${DEVELOPER_DIR:-/Library/Developer/CommandLineTools}"

./gradlew :androidApp:assembleDebug :androidApp:assembleRelease

echo "Debug APK:   androidApp/build/outputs/apk/debug/androidApp-debug.apk"
echo "Release APK: androidApp/build/outputs/apk/release/androidApp-release*.apk"
if [[ ! -f androidApp/keystore.properties ]]; then
  echo "Note: release is unsigned (copy androidApp/keystore.properties.example → keystore.properties)"
fi

if [[ "${SKIP_DEVICE_REGRESSION:-0}" == "1" ]]; then
  echo "SKIP_DEVICE_REGRESSION=1 — device regression skipped"
  exit 0
fi

if adb devices | awk 'NR>1 && $2=="device" {found=1} END{exit !found}'; then
  echo "==> device connected — running install + regression"
  REGRESSION_BUILD=0 REGRESSION_INSTALL=1 ./scripts/device_regression_test.sh
else
  echo "No adb device — skip device regression (set SKIP_DEVICE_REGRESSION=1 to silence this)."
fi
