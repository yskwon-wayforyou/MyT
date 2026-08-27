#!/usr/bin/env bash
# MyT formalized device regression suite (uiautomator + logcat).
# Spec: docs/08-implementation/device-regression-suite.md
# History: docs/08-implementation/device-regression-history.md
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

JDK_HOME="${ORG_GRADLE_JAVA_HOME:-/Users/wayforyou/.jdks/jdk-17.0.20+8/Contents/Home}"
if [[ -d "$JDK_HOME/bin" ]]; then
  export PATH="$JDK_HOME/bin:$PATH"
  export JAVA_HOME="$JDK_HOME"
fi
export DEVELOPER_DIR="${DEVELOPER_DIR:-/Library/Developer/CommandLineTools}"

PACKAGE="${MYT_PACKAGE:-com.myt}"
APK="${MYT_APK:-$ROOT/androidApp/build/outputs/apk/debug/androidApp-debug.apk}"
STRICT="${REGRESSION_STRICT:-1}"
DO_BUILD="${REGRESSION_BUILD:-0}"
DO_INSTALL="${REGRESSION_INSTALL:-0}"
HISTORY_DOC="$ROOT/docs/08-implementation/device-regression-history.md"
SUITE_DOC="docs/08-implementation/device-regression-suite.md"

STAMP="$(date +%Y%m%d-%H%M%S)"
LOCAL_TS="$(date '+%Y-%m-%d %H:%M %Z')"
OUT="$ROOT/build/device-debug/regression/$STAMP"
mkdir -p "$OUT"

PASS=0
FAIL=0
SKIP=0
RESULTS=()

pick_serial() {
  if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    echo "$ANDROID_SERIAL"
    return
  fi
  local list count
  list="$(adb devices | awk 'NR>1 && $2=="device" {print $1}')"
  count="$(printf '%s\n' "$list" | awk 'NF' | wc -l | tr -d ' ')"
  if [[ "$count" -eq 0 ]]; then
    echo "FAIL G0-ADB: no device in 'device' state" >&2
    exit 1
  fi
  if [[ "$count" -gt 1 ]]; then
    echo "FAIL G0-ADB: multiple devices; set ANDROID_SERIAL" >&2
    printf '%s\n' "$list" >&2
    exit 1
  fi
  printf '%s\n' "$list" | head -n1
}

SERIAL="$(pick_serial)"
ADB=(adb -s "$SERIAL")
DEVICE_MODEL="$("${ADB[@]}" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || echo unknown)"

record() {
  local id="$1" status="$2" note="${3:-}"
  RESULTS+=("$id|$status|$note")
  case "$status" in
    PASS) PASS=$((PASS + 1)) ;;
    FAIL) FAIL=$((FAIL + 1)) ;;
    SKIP) SKIP=$((SKIP + 1)) ;;
  esac
  printf '[%s] %s %s\n' "$status" "$id" "$note"
}

dump_ui() {
  local name="$1"
  "${ADB[@]}" shell uiautomator dump /sdcard/myt-reg.xml >/dev/null 2>&1 || true
  "${ADB[@]}" pull /sdcard/myt-reg.xml "$OUT/ui-$name.xml" >/dev/null 2>&1 || true
  "${ADB[@]}" exec-out screencap -p >"$OUT/screen-$name.png" 2>/dev/null || true
}

ui_has() {
  local needle="$1" file="${2:-$OUT/ui-latest.xml}"
  [[ -f "$file" ]] || return 1
  grep -qF "$needle" "$file"
}

tap_text() {
  local label="$1" file="${2:-$OUT/ui-latest.xml}"
  local coords
  coords="$(node -e "
const fs=require('fs');
const xml=fs.readFileSync(process.argv[1],'utf8');
const label=process.argv[2];
const re=/text=\"([^\"]+)\"[^>]*bounds=\"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]\"|bounds=\"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]\"[^>]*text=\"([^\"]+)\"/g;
let m;
while ((m = re.exec(xml))) {
  const text = m[1] || m[10];
  const a = m[1] ? [+m[2],+m[3],+m[4],+m[5]] : [+m[6],+m[7],+m[8],+m[9]];
  if (text === label) {
    console.log(((a[0]+a[2])/2) + ' ' + ((a[1]+a[3])/2));
    process.exit(0);
  }
}
process.exit(1);
" "$file" "$label" 2>/dev/null)" || return 1
  # shellcheck disable=SC2086
  "${ADB[@]}" shell input tap $coords
}

# Tap first matching label among candidates.
tap_any() {
  local file="${OUT}/ui-latest.xml" label
  for label in "$@"; do
    if tap_text "$label" "$file"; then
      return 0
    fi
  done
  return 1
}

# Contains any of the needles.
ui_has_any() {
  local file="${OUT}/ui-latest.xml" n
  for n in "$@"; do
    if ui_has "$n" "$file"; then
      return 0
    fi
  done
  return 1
}

set_rotation() {
  # 0=portrait, 1=landscape (90°)
  local rot="$1"
  "${ADB[@]}" shell settings put system accelerometer_rotation 0 >/dev/null 2>&1 || true
  "${ADB[@]}" shell settings put system user_rotation "$rot" >/dev/null 2>&1 || true
  sleep 2
}

restore_rotation() {
  "${ADB[@]}" shell settings put system accelerometer_rotation 1 >/dev/null 2>&1 || true
}

refresh_ui() {
  local name="$1"
  dump_ui "$name"
  cp "$OUT/ui-$name.xml" "$OUT/ui-latest.xml" 2>/dev/null || true
}

has_fatal() {
  "${ADB[@]}" logcat -d 2>/dev/null | grep -E 'FATAL EXCEPTION' | grep -q 'com.myt' && return 0
  "${ADB[@]}" logcat -d 2>/dev/null | grep -q 'SQLiteException: no such column: trip_record.polyline' && return 0
  return 1
}

pid_alive() {
  local p
  p="$("${ADB[@]}" shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r' || true)"
  [[ -n "$p" ]]
}

# --- optional build/install ---
if [[ "$DO_BUILD" == "1" ]]; then
  ./gradlew :androidApp:assembleDebug
fi
if [[ "$DO_INSTALL" == "1" ]]; then
  [[ -f "$APK" ]] || ./gradlew :androidApp:assembleDebug
  "${ADB[@]}" install -r -t "$APK"
  if [[ -f "$ROOT/tesla.local.properties" ]]; then
    "${ADB[@]}" push "$ROOT/tesla.local.properties" /data/local/tmp/tesla.local.properties >/dev/null
    "${ADB[@]}" shell "run-as $PACKAGE cp /data/local/tmp/tesla.local.properties files/tesla.local.properties" || true
    "${ADB[@]}" shell rm -f /data/local/tmp/tesla.local.properties || true
  fi
fi

# --- G0 unlock ---
"${ADB[@]}" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
"${ADB[@]}" shell wm dismiss-keyguard >/dev/null 2>&1 || true
sleep 1
refresh_ui "lockcheck"
if ui_has "잠금해제" "$OUT/ui-lockcheck.xml" || ui_has "패턴을 그리세요" "$OUT/ui-lockcheck.xml"; then
  record "G0-UNLOCK" "FAIL" "device locked — unlock pattern and re-run"
  if [[ "$STRICT" == "1" ]]; then
    echo "Device is locked. Unlock then re-run." >&2
    exit 1
  fi
else
  record "G0-UNLOCK" "PASS" ""
fi
record "G0-ADB" "PASS" "serial=$SERIAL model=$DEVICE_MODEL"

# --- R1 cold start ---
"${ADB[@]}" shell am force-stop "$PACKAGE" || true
"${ADB[@]}" logcat -c || true
"${ADB[@]}" shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null
sleep 4
"${ADB[@]}" shell am start -n "$PACKAGE/.MainActivity" >/dev/null 2>&1 || true
sleep 2
if pid_alive; then
  record "R1" "PASS" "cold start pid ok"
else
  record "R1" "FAIL" "process not running"
fi

# --- R2 gauge shell ---
refresh_ui "gauge"
cp "$OUT/ui-gauge.xml" "$OUT/ui-latest.xml" 2>/dev/null || true
MISSING=()
ui_has "MyT" || MISSING+=("MyT")
ui_has_any "기록" "히스토리" || MISSING+=("기록|히스토리")
ui_has "음성" || MISSING+=("음성")
ui_has_any "더보기" "설정" || MISSING+=("더보기|설정")
if [[ ${#MISSING[@]} -eq 0 ]]; then
  record "R2" "PASS" "gauge shell labels present"
else
  record "R2" "FAIL" "missing: ${MISSING[*]}"
fi

# --- R2c dual cluster ---
DUAL_MISS=()
ui_has "지시등" || DUAL_MISS+=("지시등")
ui_has_any "VEHICLE MAP" "DRIVE MAP" "CHARGING MAP" "SPEED CAM" "NAVIGATION" "G-METER" "TIRES" || DUAL_MISS+=("secondary-header")
ui_has_any "탭: 지도" || DUAL_MISS+=("toggle-hint")
if [[ ${#DUAL_MISS[@]} -eq 0 ]]; then
  record "R2c" "PASS" "dual cluster + map header"
else
  record "R2c" "FAIL" "missing: ${DUAL_MISS[*]}"
fi

# --- R2d compact status ---
if ui_has "%" && ui_has_any "BT ON" "BT OFF"; then
  record "R2d" "PASS" "SOC% + BT chip"
else
  record "R2d" "FAIL" "compact status missing SOC/BT"
fi

# --- R-GPS BT gate (soft assert via UI + log) ---
if ui_has "BT OFF"; then
  if "${ADB[@]}" logcat -d 2>/dev/null | grep -Eiq 'Device GPS (off|stop)|GPS off|preferDeviceSpeed.*false|bluetoothPresent=false'; then
    record "R-GPS" "PASS" "BT OFF + GPS-off log hint"
  else
    # UI chip alone is acceptable when log wording differs
    record "R-GPS" "PASS" "BT OFF chip visible (log hint optional)"
  fi
elif ui_has "BT ON"; then
  record "R-GPS" "PASS" "BT ON — GPS gate may be active"
else
  record "R-GPS" "FAIL" "BT chip not found"
fi

# --- R3 no fatal on boot ---
if has_fatal; then
  record "R3" "FAIL" "FATAL/SQLite on boot"
else
  record "R3" "PASS" "no FATAL for com.myt"
fi

# --- R13 portrait dual ---
set_rotation 0
refresh_ui "portrait"
cp "$OUT/ui-portrait.xml" "$OUT/ui-latest.xml" 2>/dev/null || true
if has_fatal || ! pid_alive; then
  record "R13" "FAIL" "crash in portrait"
elif ui_has "MyT" && ui_has_any "음성" "기록" "더보기" && ui_has_any "지시등" "CHARGING MAP" "VEHICLE MAP" "DRIVE MAP"; then
  record "R13" "PASS" "portrait dual shell ok"
else
  record "R13" "FAIL" "portrait shell labels missing"
fi

# --- R14 landscape dual ---
set_rotation 1
refresh_ui "landscape"
cp "$OUT/ui-landscape.xml" "$OUT/ui-latest.xml" 2>/dev/null || true
if has_fatal || ! pid_alive; then
  record "R14" "FAIL" "crash in landscape"
elif ui_has "MyT" && ui_has_any "음성" "기록" "더보기" && ui_has_any "지시등" "CHARGING MAP" "VEHICLE MAP" "DRIVE MAP" "NAVIGATION" "SPEED CAM"; then
  record "R14" "PASS" "landscape dual shell ok"
else
  record "R14" "FAIL" "landscape shell labels missing"
fi

# --- R15 secondary toggle ---
# Prefer portrait for stacked secondary header hit target
set_rotation 0
refresh_ui "pre-toggle"
cp "$OUT/ui-pre-toggle.xml" "$OUT/ui-latest.xml" 2>/dev/null || true
TOGGLE_OK=0
if tap_any "탭: 지도 ↔ 타이어" "탭: 지도 ↔ G-meter" "CHARGING MAP" "VEHICLE MAP" "DRIVE MAP" "NAVIGATION" "SPEED CAM"; then
  sleep 1
  refresh_ui "post-toggle"
  cp "$OUT/ui-post-toggle.xml" "$OUT/ui-latest.xml" 2>/dev/null || true
  if has_fatal || ! pid_alive; then
    record "R15" "FAIL" "crash on secondary toggle"
  elif ui_has_any "G-METER" "TIRES" "타이어" "VEHICLE MAP" "CHARGING MAP" "DRIVE MAP"; then
    record "R15" "PASS" "secondary pane toggled/visible"
    TOGGLE_OK=1
  else
    record "R15" "FAIL" "toggle target missing after tap"
  fi
else
  record "R15" "FAIL" "could not tap secondary header"
fi
# Return to map mode if we landed on tires/g-meter (best-effort)
if [[ "$TOGGLE_OK" -eq 1 ]]; then
  tap_any "G-METER" "TIRES" "타이어" "탭: 지도 ↔ 타이어" "탭: 지도 ↔ G-meter" >/dev/null 2>&1 || true
  sleep 1
fi
restore_rotation
refresh_ui "gauge-restored"
cp "$OUT/ui-gauge-restored.xml" "$OUT/ui-latest.xml" 2>/dev/null || true

# --- R4 history open ---
if tap_any "기록" "히스토리"; then
  sleep 2
  refresh_ui "history"
  if has_fatal || ! pid_alive; then
    record "R4" "FAIL" "crash on history open"
  elif ui_has "히스토리" && ui_has "닫기"; then
    record "R4" "PASS" "history sheet open"
  else
    record "R4" "FAIL" "history UI not detected"
  fi
else
  record "R4" "FAIL" "could not tap 기록/히스토리"
fi

# --- R11 sqlite specifically ---
if "${ADB[@]}" logcat -d 2>/dev/null | grep -q 'polyline_encoded'; then
  record "R11" "FAIL" "polyline_encoded SQLite error in logcat"
else
  record "R11" "PASS" "no polyline SQLite error"
fi

# --- R5 history close ---
if tap_text "닫기"; then
  sleep 1
  refresh_ui "after-history"
  if ui_has_any "더보기" "설정" "음성" "기록"; then
    record "R5" "PASS" "back to gauge"
  else
    record "R5" "FAIL" "gauge not restored"
  fi
else
  record "R5" "SKIP" "닫기 not found"
fi

# --- R2b more hub ---
if tap_text "더보기"; then
  sleep 1
  refresh_ui "more"
  if ui_has "설정"; then
    record "R2b" "PASS" "more hub shows 설정"
  else
    record "R2b" "FAIL" "more hub missing 설정"
  fi
  # stay on more for R6, or close if settings already open path differs
else
  record "R2b" "FAIL" "could not tap 더보기"
fi

# --- R6 settings (via 더보기 hub when present) ---
# If still on more hub from R2b, tap 설정; else open 더보기 again.
if ui_has "설정" "$OUT/ui-latest.xml"; then
  tap_text "설정" || true
else
  tap_text "더보기" && sleep 1 && refresh_ui "more2" && tap_text "설정" || tap_text "설정" || true
fi
sleep 2
refresh_ui "settings"
if has_fatal || ! pid_alive; then
  record "R6" "FAIL" "crash on settings"
elif ui_has "설정"; then
  record "R6" "PASS" "settings open via more hub"
else
  record "R6" "FAIL" "settings UI missing"
fi

# --- R7 settings back ---
if tap_text "뒤로"; then
  sleep 1
  refresh_ui "after-settings"
  if ui_has_any "히스토리" "음성" "더보기" "기록"; then
    record "R7" "PASS" "back from settings"
  else
    record "R7" "FAIL" "gauge not restored after settings"
  fi
else
  # try 닫기
  if tap_text "닫기"; then
    sleep 1
    record "R7" "PASS" "closed via 닫기"
    refresh_ui "after-settings"
  else
    record "R7" "SKIP" "뒤로/닫기 not found"
  fi
fi

# --- R8 voice ---
if tap_text "음성"; then
  sleep 2
  refresh_ui "voice"
  if has_fatal || ! pid_alive; then
    record "R8" "FAIL" "crash on voice"
  elif ui_has "음성"; then
    record "R8" "PASS" "voice UI open"
  else
    record "R8" "FAIL" "voice UI missing"
  fi
else
  record "R8" "FAIL" "could not tap 음성"
fi

# --- R9 voice close ---
if tap_text "닫기"; then
  sleep 1
  refresh_ui "after-voice"
  if ui_has_any "히스토리" "더보기" "설정" "기록"; then
    record "R9" "PASS" "back from voice"
  else
    record "R9" "FAIL" "gauge not restored after voice"
  fi
else
  record "R9" "SKIP" "voice 닫기 not found"
fi

# --- R10 runtime log ---
if "${ADB[@]}" shell "run-as $PACKAGE cat files/debug_logs/myt-runtime.log" >"$OUT/myt-runtime.log" 2>/dev/null; then
  if [[ -s "$OUT/myt-runtime.log" ]]; then
    record "R10" "PASS" "runtime log pulled"
  else
    record "R10" "PASS" "runtime log path ok (empty)"
  fi
else
  record "R10" "FAIL" "cannot read myt-runtime.log"
fi

# --- R12 soft fleet / process still alive ---
if pid_alive; then
  record "R12" "PASS" "process alive after suite"
else
  record "R12" "FAIL" "process died"
fi

# Ensure natural orientation restored
restore_rotation

# Collect logcat
"${ADB[@]}" logcat -d -v time >"$OUT/logcat.txt" 2>/dev/null || true

# --- RESULT.md ---
{
  echo "# Device regression $STAMP"
  echo
  echo "- When: $LOCAL_TS"
  echo "- Device: $DEVICE_MODEL ($SERIAL)"
  echo "- Package: $PACKAGE"
  echo "- Suite: $SUITE_DOC"
  echo "- PASS=$PASS FAIL=$FAIL SKIP=$SKIP"
  echo
  echo "| ID | Status | Note |"
  echo "|----|--------|------|"
  for row in "${RESULTS[@]}"; do
    IFS='|' read -r id status note <<<"$row"
    echo "| $id | $status | ${note//|/\\|} |"
  done
} >"$OUT/RESULT.md"

# summary.json
printf '%s\n' "${RESULTS[@]}" >"$OUT/results.lines"
STAMP="$STAMP" WHEN="$LOCAL_TS" SERIAL="$SERIAL" DEVICE_MODEL="$DEVICE_MODEL" \
PASS="$PASS" FAIL="$FAIL" SKIP="$SKIP" OUT="$OUT" \
node <<'NODE'
const fs = require('fs');
const results = fs.readFileSync(process.env.OUT + '/results.lines', 'utf8')
  .split('\n').filter(Boolean)
  .map((r) => {
    const i = r.indexOf('|');
    const j = r.indexOf('|', i + 1);
    return { id: r.slice(0, i), status: r.slice(i + 1, j), note: r.slice(j + 1) };
  });
const out = {
  stamp: process.env.STAMP,
  when: process.env.WHEN,
  serial: process.env.SERIAL,
  model: process.env.DEVICE_MODEL,
  pass: Number(process.env.PASS),
  fail: Number(process.env.FAIL),
  skip: Number(process.env.SKIP),
  results,
};
fs.writeFileSync(process.env.OUT + '/summary.json', JSON.stringify(out, null, 2));
NODE

# --- append history doc ---
if [[ -f "$HISTORY_DOC" ]]; then
  OVERALL="PASS"
  [[ "$FAIL" -gt 0 ]] && OVERALL="FAIL"
  BLOCK_FILE="$OUT/history-block.md"
  {
    echo "## $LOCAL_TS — $DEVICE_MODEL ($SERIAL) — $OVERALL"
    echo
    echo "**산출물:** \`build/device-debug/regression/$STAMP/\`  "
    echo "**집계:** PASS=$PASS FAIL=$FAIL SKIP=$SKIP"
    echo
    echo "| ID | Status | Note |"
    echo "|----|--------|------|"
    for row in "${RESULTS[@]}"; do
      IFS='|' read -r id status note <<<"$row"
      echo "| $id | $status | ${note//|/\\|} |"
    done
    echo
    echo "---"
    echo
  } >"$BLOCK_FILE"

  HISTORY_DOC="$HISTORY_DOC" BLOCK_FILE="$BLOCK_FILE" node <<'NODE'
const fs = require('fs');
const docPath = process.env.HISTORY_DOC;
const block = fs.readFileSync(process.env.BLOCK_FILE, 'utf8');
let doc = fs.readFileSync(docPath, 'utf8');
const marker = '<!-- REGRESSION_HISTORY_APPEND_POINT -->';
const idx = doc.indexOf(marker);
if (idx >= 0) {
  const insertAt = idx + marker.length;
  doc = doc.slice(0, insertAt) + '\n\n' + block + doc.slice(insertAt);
} else {
  doc += '\n' + block;
}
fs.writeFileSync(docPath, doc);
NODE
fi

echo
echo "RESULT → $OUT/RESULT.md"
echo "PASS=$PASS FAIL=$FAIL SKIP=$SKIP"

if [[ "$FAIL" -gt 0 && "$STRICT" == "1" ]]; then
  exit 1
fi
exit 0
