#!/usr/bin/env bash
# Generate ComposeApp.framework for Xcode (run from repo root).
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-$HOME/.jdks/jdk-17.0.20+8/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
echo "Framework: composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework"
echo "Open iosApp/iosApp.xcodeproj in Xcode 16+ (create project if missing — see iosApp/README.md)"
