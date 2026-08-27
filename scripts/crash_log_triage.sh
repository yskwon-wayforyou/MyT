#!/usr/bin/env bash
# Pull MyT crash/runtime logs from a connected device and file GitHub issues via `gh`.
# Used by Cursor command: crash-log-triage
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

PACKAGE="${MYT_PACKAGE:-com.myt}"
REPO="${MYT_GITHUB_REPO:-yskwon-wayforyou/MyT}"
OUT_DIR="${MYT_CRASH_OUT:-$ROOT/build/device-debug/crash-logs}"
mkdir -p "$OUT_DIR"

pick_serial() {
  if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    echo "$ANDROID_SERIAL"
    return
  fi
  local list count
  list="$(adb devices | awk 'NR>1 && $2=="device" {print $1}')"
  count="$(printf '%s\n' "$list" | awk 'NF' | wc -l | tr -d ' ')"
  if [[ "$count" -eq 0 ]]; then
    echo "No adb device. Connect USB debugging." >&2
    exit 1
  fi
  if [[ "$count" -gt 1 ]]; then
    echo "Multiple devices; set ANDROID_SERIAL." >&2
    printf '%s\n' "$list" >&2
    exit 1
  fi
  printf '%s\n' "$list" | head -n1
}

SERIAL="$(pick_serial)"
ADB=(adb -s "$SERIAL")
STAMP="$(date +%Y%m%d-%H%M%S)"
BASE="$OUT_DIR/$STAMP"
mkdir -p "$BASE/debug_logs" "$BASE/pending_github_issues" "$BASE/crash_reports"

echo "SERIAL=$SERIAL PACKAGE=$PACKAGE"
echo "Pulling app private files via run-as…"

# Copy out of app sandbox via tar (adb pull of nested dirs is unreliable)
"${ADB[@]}" shell "run-as $PACKAGE sh -c 'cd files && tar -cf - debug_logs pending_github_issues crash_reports 2>/dev/null'" \
  >"$BASE/files.tar" || true
if [[ -s "$BASE/files.tar" ]]; then
  tar -xf "$BASE/files.tar" -C "$BASE" 2>/dev/null || true
fi
rm -f "$BASE/files.tar"
# Fallback: individual cats for runtime log
"${ADB[@]}" shell "run-as $PACKAGE cat files/debug_logs/myt-runtime.log" >"$BASE/debug_logs/myt-runtime.log" 2>/dev/null || true
"${ADB[@]}" shell "run-as $PACKAGE cat files/crash_reports/myt-last-crash.txt" >"$BASE/crash_reports/myt-last-crash.txt" 2>/dev/null || true

# Flatten accidental nested dirs from pull
find "$BASE" -type f \( -name '*.log' -o -name '*.json' -o -name '*.md' -o -name '*.txt' \) | sort >"$BASE/index.txt"

echo "Collected files:"
cat "$BASE/index.txt" || true

file_issues() {
  command -v gh >/dev/null || { echo "gh not installed — skip GitHub issue create"; return 0; }
  local json body_file title labels url number
  shopt -s nullglob
  for json in "$BASE"/pending_github_issues/**/*.json "$BASE"/pending_github_issues/*.json; do
    [[ -f "$json" ]] || continue
    # Skip already uploaded
    if grep -q '"uploaded": true' "$json" 2>/dev/null; then
      echo "skip uploaded: $json"
      continue
    fi
    title="$(node -e "const fs=require('fs');console.log(JSON.parse(fs.readFileSync(process.argv[1],'utf8')).title||'MyT crash')" "$json")"
    body_file="${json%.json}.md"
    if [[ ! -f "$body_file" ]]; then
      body_file="$BASE/issue-body-$$.md"
      node -e "const fs=require('fs');const j=JSON.parse(fs.readFileSync(process.argv[1],'utf8'));fs.writeFileSync(process.argv[2], j.body||'')" "$json" "$body_file"
    fi
    labels="auto-reported,crash"
    if grep -q '"kind": "error"' "$json"; then
      labels="auto-reported,bug,runtime-error"
    fi
    echo "Creating GitHub issue: $title"
    url="$(gh issue create --repo "$REPO" --title "$title" --body-file "$body_file" --label "$labels" 2>/dev/null || \
      gh issue create --repo "$REPO" --title "$title" --body-file "$body_file")"
    echo "Created: $url"
    number="$(echo "$url" | grep -Eo '[0-9]+$' || true)"
    # Best-effort mark on device
    local id
    id="$(basename "$json" .json)"
    "${ADB[@]}" shell "run-as $PACKAGE sh -c 'sed -i \"s/\\\"uploaded\\\": false/\\\"uploaded\\\": true/\" files/pending_github_issues/${id}.json 2>/dev/null || true'" || true
    echo "$url" >>"$BASE/created-issues.txt"
  done
}

if [[ "${1:-all}" == "pull" ]]; then
  echo "Pull only → $BASE"
  exit 0
fi

file_issues

# Summary for the agent
{
  echo "# Crash log triage summary ($STAMP)"
  echo
  echo "## Runtime log tail"
  echo '```'
  RUNTIME="$(find "$BASE" -name 'myt-runtime.log' | head -1 || true)"
  if [[ -n "$RUNTIME" ]]; then
    tail -n 120 "$RUNTIME"
  else
    echo "(no myt-runtime.log found)"
  fi
  echo '```'
  echo
  echo "## Last crash"
  echo '```'
  CRASH="$(find "$BASE" -name 'myt-last-crash.txt' | head -1 || true)"
  if [[ -n "$CRASH" ]]; then
    cat "$CRASH"
  else
    echo "(no crash report)"
  fi
  echo '```'
  echo
  echo "## Created issues"
  if [[ -f "$BASE/created-issues.txt" ]]; then
    cat "$BASE/created-issues.txt"
  else
    echo "(none)"
  fi
} >"$BASE/SUMMARY.md"

echo "SUMMARY → $BASE/SUMMARY.md"
echo "$BASE"
