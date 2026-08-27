#!/usr/bin/env bash
# Drive simulation smoke test on connected device.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export DEVELOPER_DIR="${DEVELOPER_DIR:-/Library/Developer/CommandLineTools}"
export JAVA_HOME="${JAVA_HOME:-$HOME/.jdks/jdk-17.0.20+8/Contents/Home}"

echo "== Build & install =="
./gradlew :androidApp:assembleDebug -q
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk

echo "== Start charging map simulation =="
adb shell am force-stop com.myt
adb shell am start -n com.myt/.MainActivity
sleep 4
adb shell am broadcast -a com.myt.action.DRIVE_SIM \
  -n com.myt/com.myt.debug.DriveSimBroadcastReceiver --es scenario charging_parked
sleep 3

echo "== UI dump (map/charging) =="
adb shell uiautomator dump /sdcard/myt-sim.xml >/dev/null
adb pull /sdcard/myt-sim.xml /tmp/myt-sim.xml >/dev/null
node -e "
const fs=require('fs');
const xml=fs.readFileSync('/tmp/myt-sim.xml','utf8');
const texts=[...new Set([...xml.matchAll(/text=\"([^\"]+)\"/g)].map(m=>m[1]).filter(Boolean))];
console.log(texts.filter(t=>/CHARGING|충전|광교|시뮬|OpenStreetMap|OSM|테슬라/.test(t)).join('\n') || '(no matching texts)');
"

echo "== Speed cam L3 simulation =="
adb shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
adb shell am start -n com.myt/.MainActivity 2>/dev/null || true
adb shell am broadcast -a com.myt.action.DRIVE_SIM \
  -n com.myt/com.myt.debug.DriveSimBroadcastReceiver --es scenario speed_cam_l3
sleep 22
adb shell uiautomator dump /sdcard/myt-sim2.xml 2>/dev/null || true
adb pull /sdcard/myt-sim2.xml /tmp/myt-sim2.xml >/dev/null
node -e "
const fs=require('fs');
const xml=fs.readFileSync('/tmp/myt-sim2.xml','utf8');
const texts=[...new Set([...xml.matchAll(/text=\"([^\"]+)\"/g)].map(m=>m[1]).filter(Boolean))];
const hits=texts.filter(t=>/과속|단속|SPEED|CAM|제한|영통|시각|무음|SIM|km\\/h|km/.test(t));
console.log(hits.join('\n') || '(no speed cam texts — check sim overlay)');
"

echo "== Stop simulation =="
adb shell am broadcast -a com.myt.action.DRIVE_SIM \
  -n com.myt/com.myt.debug.DriveSimBroadcastReceiver --es scenario stop
echo "Done."
