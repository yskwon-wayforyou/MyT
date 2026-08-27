#!/usr/bin/env node
/** Quick device UI probe for Commercial / Analytics / History demos. */
const { execSync } = require("child_process");
const fs = require("fs");

const SERIAL = process.env.ANDROID_SERIAL || "R3CY400P2PP";
const PKG = "com.myt";
const XML = "/tmp/myt-probe.xml";

function adb(args) {
  return execSync(`adb -s ${SERIAL} ${args}`, {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
}

function dump() {
  try {
    adb("shell uiautomator dump /sdcard/myt-probe.xml");
  } catch (_) {}
  adb(`pull /sdcard/myt-probe.xml ${XML}`);
  return fs.readFileSync(XML, "utf8");
}

function texts(xml) {
  return [...new Set([...xml.matchAll(/text="([^"]+)"/g)].map((m) => m[1]))];
}

function findTap(xml, needle, exactOnly = false) {
  const nodes = [];
  const re1 = /text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/g;
  const re2 = /bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*text="([^"]*)"/g;
  let m;
  while ((m = re1.exec(xml))) {
    nodes.push({ text: m[1], x1: +m[2], y1: +m[3], x2: +m[4], y2: +m[5] });
  }
  while ((m = re2.exec(xml))) {
    nodes.push({ text: m[5], x1: +m[1], y1: +m[2], x2: +m[3], y2: +m[4] });
  }
  let soft = null;
  for (const n of nodes) {
    if (typeof n.text !== "string" || !n.text) continue;
    const cx = (n.x1 + n.x2) / 2;
    const cy = (n.y1 + n.y2) / 2;
    if (n.text === needle) return [cx, cy, n.text];
    if (!exactOnly && !soft && n.text.includes(needle)) soft = [cx, cy, n.text];
  }
  return soft;
}

function tap(needle) {
  const xml = dump();
  const hit = findTap(xml, needle, false);
  if (!hit) {
    console.log("NOTFOUND", needle, "| texts:", texts(xml).join(" | ").slice(0, 400));
    return false;
  }
  const [x, y, label] = hit;
  console.log(`TAP "${label}" -> ${Math.floor(x)},${Math.floor(y)}`);
  adb(`shell input tap ${Math.floor(x)} ${Math.floor(y)}`);
  execSync("sleep 1.3");
  return true;
}

function tapExact(needle) {
  const xml = dump();
  const hit = findTap(xml, needle, true);
  if (!hit) {
    console.log("NOTFOUND exact", needle, "| texts:", texts(xml).join(" | ").slice(0, 400));
    return false;
  }
  const [x, y, label] = hit;
  console.log(`TAP exact "${label}" -> ${Math.floor(x)},${Math.floor(y)}`);
  adb(`shell input tap ${Math.floor(x)} ${Math.floor(y)}`);
  execSync("sleep 1.3");
  return true;
}

function back() {
  adb("shell input keyevent KEYCODE_BACK");
  execSync("sleep 1");
}

function assertHas(label, needles) {
  const t = texts(dump());
  const miss = needles.filter((n) => !t.some((x) => x.includes(n)));
  if (miss.length) {
    console.log(`FAIL ${label} missing:`, miss.join(", "));
    console.log(" got:", t.join(" | ").slice(0, 600));
    return false;
  }
  console.log(`OK ${label}:`, needles.join(", "));
  return true;
}

function waitForGauge(timeoutMs = 20000) {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    try {
      adb(`shell monkey -p ${PKG} -c android.intent.category.LAUNCHER 1`);
    } catch (_) {}
    execSync("sleep 2.5");
    const t = texts(dump());
    if (t.includes("더보기") && (t.includes("MyT") || t.includes("음성") || t.includes("기록"))) {
      console.log("OK gauge foreground");
      return true;
    }
    console.log("wait gauge…", t.slice(0, 5).join(" | "));
  }
  return false;
}

adb("logcat -c");
adb(`shell am force-stop ${PKG}`);
execSync("sleep 1");
if (!waitForGauge()) {
  console.log("FAIL could not foreground MyT gauge");
  process.exit(1);
}

let ok = true;
ok = tap("더보기") && ok;
ok = assertHas("HUB", ["설정", "고급 분석", "구독"]) && ok;
ok = tap("구독") && ok;
ok = assertHas("COMMERCIAL", ["Free", "Plus", "Pro"]) && ok;
if (tap("Plus")) {
  ok = assertHas("PLAN", ["Plus"]) && ok;
}
ok = (tap("닫기") || (back(), true)) && ok;

ok = tap("더보기") && ok;
ok = tap("고급 분석") && ok;
ok = assertHas("ANALYTICS", ["Live Camera", "FSD"]) && ok;
ok = tap("데모 스트림") && ok;
ok = assertHas("CAMERA", ["전방", "후방"]) && ok;
ok = (tap("닫기") || (back(), true)) && ok;

// Bring MyT to foreground the same way regression does (Samsung can leave launcher on top).
try {
  adb(`shell monkey -p ${PKG} -c android.intent.category.LAUNCHER 1`);
} catch (_) {}
execSync("sleep 4");
if (tapExact("기록")) {
  const histOk =
    assertHas("HISTORY", ["주행"]) ||
    assertHas("HISTORY2", ["충전"]) ||
    assertHas("HISTORY3", ["히스토리"]);
  if (!histOk) ok = false;
  back();
} else {
  console.log("WARN history tap skipped (gauge not foreground); covered by device_regression R4");
}

const pid = (() => {
  try {
    return adb(`shell pidof ${PKG}`).trim();
  } catch {
    return "";
  }
})();
console.log(pid ? `OK process pid=${pid}` : "FAIL process dead");
if (!pid) ok = false;

let log = "";
try {
  log = adb(`logcat -d --pid=${pid} -t 200`);
} catch {
  log = "";
}
const bad = /FATAL EXCEPTION|polyline_encoded|SQLiteException|NoBeanDefFoundException/;
const hits = log.split("\n").filter((l) => bad.test(l));
if (hits.length) {
  console.log("FAIL logcat:");
  console.log(hits.slice(0, 25).join("\n"));
  ok = false;
} else {
  console.log("OK logcat: no FATAL/polyline in recent pid buffer");
}

console.log(ok ? "PROBE PASS" : "PROBE FAIL");
process.exit(ok ? 0 : 1);
