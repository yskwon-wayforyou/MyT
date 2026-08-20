#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "==> Gradle wrapper"
if [[ ! -x "./gradlew" ]]; then
  echo "Run: gradle wrapper --gradle-version 8.11.1"
  exit 1
fi

echo "==> Common tests"
./gradlew :composeApp:cleanTest :composeApp:allTests --continue

echo "==> Android debug APK"
./gradlew :androidApp:assembleDebug

echo "==> iOS frameworks (requires Xcode on macOS)"
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 || echo "iOS build skipped"

echo "Done."
