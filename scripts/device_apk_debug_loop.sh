#!/usr/bin/env bash
# MyT: build/install/launch/collect loop for a connected Android device.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

JDK_HOME="${ORG_GRADLE_JAVA_HOME:-/Users/wayforyou/.jdks/jdk-17.0.20+8/Contents/Home}"
if [[ -d "$JDK_HOME/bin" ]]; then
  export PATH="$JDK_HOME/bin:$PATH"
  export JAVA_HOME="$JDK_HOME"
fi

PACKAGE="${MYT_PACKAGE:-com.myt}"
APK="${MYT_APK:-$ROOT/androidApp/build/outputs/apk/debug/androidApp-debug.apk}"
OUT_DIR="${MYT_DEBUG_OUT:-$ROOT/build/device-debug}"
mkdir -p "$OUT_DIR"

pick_serial() {
  if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    echo "$ANDROID_SERIAL"
    return
  fi
  local list
  list="$(adb devices | awk 'NR>1 && $2=="device" {print $1}')"
  local count
  count="$(printf '%s\n' "$list" | awk 'NF' | wc -l | tr -d ' ')"
  if [[ "$count" -eq 0 ]]; then
    echo "No adb device in 'device' state. Connect USB and enable debugging." >&2
    exit 1
  fi
  if [[ "$count" -gt 1 ]]; then
    echo "Multiple devices; set ANDROID_SERIAL. Found:" >&2
    printf '%s\n' "$list" >&2
    exit 1
  fi
  printf '%s\n' "$list" | head -n1
}

SERIAL="$(pick_serial)"
ADB=(adb -s "$SERIAL")

cmd_build() {
  ./gradlew :androidApp:assembleDebug
}

cmd_install() {
  [[ -f "$APK" ]] || cmd_build
  "${ADB[@]}" install -r -t "$APK"
}

cmd_launch() {
  "${ADB[@]}" shell am force-stop "$PACKAGE" || true
  "${ADB[@]}" logcat -c || true
  "${ADB[@]}" shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null
}

cmd_sync_config() {
  local src="$ROOT/tesla.local.properties"
  [[ -f "$src" ]] || { echo "Missing $src" >&2; exit 1; }
  # Push into app private files (debuggable build required for run-as)
  "${ADB[@]}" push "$src" /data/local/tmp/tesla.local.properties >/dev/null
  "${ADB[@]}" shell "run-as $PACKAGE cp /data/local/tmp/tesla.local.properties files/tesla.local.properties"
  "${ADB[@]}" shell rm -f /data/local/tmp/tesla.local.properties
  echo "Synced tesla.local.properties → device files/ (secrets not printed)"
}

cmd_collect() {
  local stamp
  stamp="$(date +%Y%m%d-%H%M%S)"
  local base="$OUT_DIR/$stamp"
  mkdir -p "$base"
  "${ADB[@]}" logcat -d -v time >"$base/logcat.txt" || true
  "${ADB[@]}" shell dumpsys activity activities >"$base/activities.txt" || true
  "${ADB[@]}" exec-out screencap -p >"$base/screen.png" || true
  "${ADB[@]}" shell uiautomator dump /sdcard/myt-ui.xml >/dev/null 2>&1 || true
  "${ADB[@]}" pull /sdcard/myt-ui.xml "$base/ui.xml" >/dev/null 2>&1 || true

  # Extract authorize URLs / redirect params (no secrets expected in URL)
  grep -Eo 'https://fleet-auth[^" ]+' "$base/activities.txt" 2>/dev/null \
    | head -n 5 >"$base/authorize-urls.txt" || true
  grep -Ei 'redirect_uri|redirect_url|Auth|OAuth|AndroidRuntime|FATAL' "$base/logcat.txt" \
    | head -n 200 >"$base/oauth-hints.txt" || true

  # Masked device config
  if "${ADB[@]}" shell "run-as $PACKAGE cat files/tesla.local.properties" >"$base/device-config.raw" 2>/dev/null; then
    sed -E 's/(secret|token|password)=.*/\1=***REDACTED***/I' "$base/device-config.raw" >"$base/device-config.masked.txt"
    rm -f "$base/device-config.raw"
  fi

  echo "SERIAL=$SERIAL"
  echo "Collected → $base"
  if [[ -s "$base/authorize-urls.txt" ]]; then
    echo "Authorize URL sample:"
    head -n1 "$base/authorize-urls.txt"
  fi
}

cmd_all() {
  cmd_build
  cmd_install
  cmd_sync_config
  cmd_launch
  sleep 3
  cmd_collect
  if [[ "${SKIP_DEVICE_REGRESSION:-0}" != "1" ]]; then
    echo "==> formalized device regression"
    REGRESSION_BUILD=0 REGRESSION_INSTALL=0 "$ROOT/scripts/device_regression_test.sh" || {
      echo "Regression failed — see build/device-debug/regression/ and docs/08-implementation/device-regression-history.md" >&2
      exit 1
    }
  fi
}

usage() {
  cat <<EOF
Usage: $0 {build|install|launch|sync-config|collect|all}
  ANDROID_SERIAL=... optional device serial
  SKIP_DEVICE_REGRESSION=1  skip formalized regression after 'all'
EOF
}

case "${1:-all}" in
  build) cmd_build ;;
  install) cmd_install ;;
  launch) cmd_launch ;;
  sync-config) cmd_sync_config ;;
  collect) cmd_collect ;;
  all) cmd_all ;;
  *) usage; exit 2 ;;
esac
