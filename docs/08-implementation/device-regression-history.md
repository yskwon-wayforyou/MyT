# MyT 실단말 리그레션 히스토리

자동/수동 실단말 리그레션 결과를 **시간 역순**으로 기록합니다.  
스위트 정의: [device-regression-suite.md](./device-regression-suite.md)  
실행기: `./scripts/device_regression_test.sh` (빌드·설치 시 `./scripts/build-install-regress.sh`)

---

## 2026-08-26 10:57–11:05 KST — SM-S931N (R3CY400P2PP) — 초기 실단말 QA (수동+adb)

**빌드:** `0.2.0-debug` → 수정 후 `0.2.1` (versionCode 3)  
**방법:** `device_apk_debug_loop` + uiautomator 탭 + logcat/런타임 로그  
**총평:** 치명 버그 다수 발견·수정 후 핵심 네비게이션 PASS. 잠금화면으로 최종 타이어 시각 확인은 미완.

### 실행 타임라인

| 시각(대략) | 동작 | 결과 |
|------------|------|------|
| 10:57 | APK 설치·실행 | 계기판 표시. `FLEET·BT·잠김`, SOC 캐시/절전 배너 |
| 10:57 | 펜딩 크래시/에러 확인 | SQLite `polyline_encoded` 크래시 + Empty vehicle_data 에러 큐 |
| 10:58 | **히스토리** 탭 | **FATAL** — `SQLiteException: no such column: trip_record.polyline_encoded` |
| 11:00 | 마이그레이션 `2.sqm` + Fleet soft-fail 배포 | 재설치 후 히스토리 정상 (닫기/주행/충전/Fleet API 탭) |
| 11:01 | **설정** | PASS — 레이아웃·표시항목·Tesla properties·디버그 |
| 11:02 | **음성** | 화면 PASS. STT 오류(7)은 미인식(soft) — 이후 WARN만 남기도록 수정 |
| 11:02 | 재시도(절전) | Fleet 연결·SOC 33%·타이어 **0.2 bar**(환산 버그) |
| 11:03 | TPMS bar 환산 제거 | APK 재설치 |
| 11:04 | 잠금화면 | 추가 UI 검증 중단 |
| 11:04 | GitHub Issue | [#1](https://github.com/yskwon-wayforyou/MyT/issues/1) polyline 크래시 등록 |

### 케이스별 (스위트 매핑)

| ID | 결과 | 메모 |
|----|------|------|
| R1 Cold start | PASS | |
| R2 Gauge shell | PASS | MyT / 히스토리 / 설정 / 음성 |
| R3 No crash on boot | PASS → FAIL(히스토리) → PASS | 마이그레이션 후 회복 |
| R4–R5 History | FAIL → PASS | Issue #1 |
| R6–R7 Settings | PASS | |
| R8–R9 Voice | PASS (UI) | STT 7 soft |
| R10 Runtime log | PASS | `files/debug_logs/myt-runtime.log` |
| R11 SQLite history | FAIL → PASS | `migrations/2.sqm` |
| R12 Soft fleet | PASS (수정 후) | wake + cache, GitHub 스팸 제거 |

### 수정 반영

- `composeApp/.../migrations/2.sqm` — polyline / efficiency 컬럼
- `VehicleDataUnavailableException` + Fleet/Telemetry soft-fail
- `TeslaFleetApi` TPMS: bar 그대로 사용
- Voice STT: `debugLogger.w` (이슈 미등록)
- `ErrorIssueEnqueuer`: empty vehicle_data / STT soft skip

### 미해소 / 후속

- [ ] 잠금 해제 후 타이어 정상 bar(≈2.x) 시각 확인
- [ ] 실차 wake 후 라이브 SOC·타이어 필드 검증
- [ ] `github.issues.token` 설정 시 앱 내 자동 이슈 업로드 E2E
- [x] 정형 리그레션 스위트/히스토리/빌드 연동 (`device-regression-suite.md`, `device_regression_test.sh`, `build-install-regress.sh`)
- [x] 잠금 해제 후 `./scripts/build-install-regress.sh` 첫 자동 실행으로 히스토리 append (2026-08-26 14:04 KST — PASS=14)

---

<!-- REGRESSION_HISTORY_APPEND_POINT -->

## 2026-08-27 17:10 KST — SM-S931N (R3CY400P2PP) — PASS

**산출물:** `build/device-debug/regression/20260827-171056/`  
**집계:** PASS=21 FAIL=0 SKIP=0

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | PASS | gauge shell labels present |
| R2c | PASS | dual cluster + map header |
| R2d | PASS | SOC% + BT chip |
| R-GPS | PASS | BT OFF chip visible (log hint optional) |
| R3 | PASS | no FATAL for com.myt |
| R13 | PASS | portrait dual shell ok |
| R14 | PASS | landscape dual shell ok |
| R15 | PASS | secondary pane toggled/visible |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R2b | PASS | more hub shows 설정 |
| R6 | PASS | settings open via more hub |
| R7 | PASS | back from settings |
| R8 | PASS | voice UI open |
| R9 | PASS | back from voice |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-27 17:08 KST — SM-S931N (R3CY400P2PP) — FAIL

**산출물:** `build/device-debug/regression/20260827-170828/`  
**집계:** PASS=11 FAIL=7 SKIP=3

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | PASS | gauge shell labels present |
| R2c | FAIL | missing: 지시등 |
| R2d | PASS | SOC% + BT chip |
| R-GPS | PASS | BT OFF chip visible (log hint optional) |
| R3 | PASS | no FATAL for com.myt |
| R13 | PASS | portrait dual shell ok |
| R14 | FAIL | landscape shell labels missing |
| R15 | FAIL | could not tap secondary header |
| R4 | FAIL | could not tap 기록/히스토리 |
| R11 | PASS | no polyline SQLite error |
| R5 | SKIP | 닫기 not found |
| R2b | FAIL | could not tap 더보기 |
| R6 | FAIL | settings UI missing |
| R7 | SKIP | 뒤로/닫기 not found |
| R8 | FAIL | could not tap 음성 |
| R9 | SKIP | voice 닫기 not found |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-27 16:43 KST — SM-S931N (R3CY400P2PP) — PASS

**산출물:** `build/device-debug/regression/20260827-164346/`  
**집계:** PASS=21 FAIL=0 SKIP=0

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | PASS | gauge shell labels present |
| R2c | PASS | dual cluster + map header |
| R2d | PASS | SOC% + BT chip |
| R-GPS | PASS | BT OFF chip visible (log hint optional) |
| R3 | PASS | no FATAL for com.myt |
| R13 | PASS | portrait dual shell ok |
| R14 | PASS | landscape dual shell ok |
| R15 | PASS | secondary pane toggled/visible |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R2b | PASS | more hub shows 설정 |
| R6 | PASS | settings open via more hub |
| R7 | PASS | back from settings |
| R8 | PASS | voice UI open |
| R9 | PASS | back from voice |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-27 16:42 KST — SM-S931N (R3CY400P2PP) — FAIL

**산출물:** `build/device-debug/regression/20260827-164207/`  
**집계:** PASS=18 FAIL=3 SKIP=0

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | FAIL | missing: MyT |
| R2c | PASS | dual cluster + map header |
| R2d | PASS | SOC% + BT chip |
| R-GPS | PASS | BT OFF chip visible (log hint optional) |
| R3 | PASS | no FATAL for com.myt |
| R13 | FAIL | portrait shell labels missing |
| R14 | FAIL | landscape shell labels missing |
| R15 | PASS | secondary pane toggled/visible |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R2b | PASS | more hub shows 설정 |
| R6 | PASS | settings open via more hub |
| R7 | PASS | back from settings |
| R8 | PASS | voice UI open |
| R9 | PASS | back from voice |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-27 16:35 KST — SM-S931N (R3CY400P2PP) — FAIL

**산출물:** `build/device-debug/regression/20260827-163549/`  
**집계:** PASS=18 FAIL=1 SKIP=2

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | PASS | gauge shell labels present |
| R2c | PASS | dual cluster + map header |
| R2d | PASS | SOC% + BT chip |
| R-GPS | PASS | BT OFF chip visible (log hint optional) |
| R3 | PASS | no FATAL for com.myt |
| R13 | PASS | portrait dual shell ok |
| R14 | PASS | landscape dual shell ok |
| R15 | PASS | secondary pane toggled/visible |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R2b | PASS | more hub shows 설정 |
| R6 | PASS | settings open via more hub |
| R7 | SKIP | 뒤로/닫기 not found |
| R8 | FAIL | could not tap 음성 |
| R9 | SKIP | voice 닫기 not found |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-27 16:32 KST — SM-S931N (R3CY400P2PP) — PASS

**산출물:** `build/device-debug/regression/20260827-163203/`  
**집계:** PASS=21 FAIL=0 SKIP=0

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | PASS | gauge shell labels present |
| R2c | PASS | dual cluster + map header |
| R2d | PASS | SOC% + BT chip |
| R-GPS | PASS | BT OFF chip visible (log hint optional) |
| R3 | PASS | no FATAL for com.myt |
| R13 | PASS | portrait dual shell ok |
| R14 | PASS | landscape dual shell ok |
| R15 | PASS | secondary pane toggled/visible |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R2b | PASS | more hub shows 설정 |
| R6 | PASS | settings open via more hub |
| R7 | PASS | back from settings |
| R8 | PASS | voice UI open |
| R9 | PASS | back from voice |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-27 16:27 KST — SM-S931N (R3CY400P2PP) — PASS

**산출물:** `build/device-debug/regression/20260827-162738/`  
**집계:** PASS=21 FAIL=0 SKIP=0

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | PASS | gauge shell labels present |
| R2c | PASS | dual cluster + map header |
| R2d | PASS | SOC% + BT chip |
| R-GPS | PASS | BT OFF chip visible (log hint optional) |
| R3 | PASS | no FATAL for com.myt |
| R13 | PASS | portrait dual shell ok |
| R14 | PASS | landscape dual shell ok |
| R15 | PASS | secondary pane toggled/visible |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R2b | PASS | more hub shows 설정 |
| R6 | PASS | settings open via more hub |
| R7 | PASS | back from settings |
| R8 | PASS | voice UI open |
| R9 | PASS | back from voice |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-27 16:20 KST — SM-S931N (R3CY400P2PP) — PASS

**산출물:** `build/device-debug/regression/20260827-162034/`  
**집계:** PASS=21 FAIL=0 SKIP=0

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | PASS | gauge shell labels present |
| R2c | PASS | dual cluster + map header |
| R2d | PASS | SOC% + BT chip |
| R-GPS | PASS | BT OFF chip visible (log hint optional) |
| R3 | PASS | no FATAL for com.myt |
| R13 | PASS | portrait dual shell ok |
| R14 | PASS | landscape dual shell ok |
| R15 | PASS | secondary pane toggled/visible |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R2b | PASS | more hub shows 설정 |
| R6 | PASS | settings open via more hub |
| R7 | PASS | back from settings |
| R8 | PASS | voice UI open |
| R9 | PASS | back from voice |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-27 16:17 KST — SM-S931N (R3CY400P2PP) — FAIL

**산출물:** `build/device-debug/regression/20260827-161730/`  
**집계:** PASS=14 FAIL=7 SKIP=0

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | FAIL | missing: MyT 기록\|히스토리 음성 |
| R2c | FAIL | missing: 지시등 secondary-header toggle-hint |
| R2d | FAIL | compact status missing SOC/BT |
| R-GPS | FAIL | BT chip not found |
| R3 | PASS | no FATAL for com.myt |
| R13 | FAIL | portrait shell labels missing |
| R14 | FAIL | landscape shell labels missing |
| R15 | FAIL | could not tap secondary header |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R2b | PASS | more hub shows 설정 |
| R6 | PASS | settings open via more hub |
| R7 | PASS | back from settings |
| R8 | PASS | voice UI open |
| R9 | PASS | back from voice |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-27 15:37 KST — SM-S931N (R3CY400P2PP) — PASS

**산출물:** `build/device-debug/regression/20260827-153706/`  
**집계:** PASS=21 FAIL=0 SKIP=0

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | PASS | gauge shell labels present |
| R2c | PASS | dual cluster + map header |
| R2d | PASS | SOC% + BT chip |
| R-GPS | PASS | BT OFF chip visible (log hint optional) |
| R3 | PASS | no FATAL for com.myt |
| R13 | PASS | portrait dual shell ok |
| R14 | PASS | landscape dual shell ok |
| R15 | PASS | secondary pane toggled/visible |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R2b | PASS | more hub shows 설정 |
| R6 | PASS | settings open via more hub |
| R7 | PASS | back from settings |
| R8 | PASS | voice UI open |
| R9 | PASS | back from voice |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-27 15:23 KST — SM-S931N (R3CY400P2PP) — PASS

**산출물:** `build/device-debug/regression/20260827-152346/`  
**집계:** PASS=21 FAIL=0 SKIP=0

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | PASS | gauge shell labels present |
| R2c | PASS | dual cluster + map header |
| R2d | PASS | SOC% + BT chip |
| R-GPS | PASS | BT OFF chip visible (log hint optional) |
| R3 | PASS | no FATAL for com.myt |
| R13 | PASS | portrait dual shell ok |
| R14 | PASS | landscape dual shell ok |
| R15 | PASS | secondary pane toggled/visible |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R2b | PASS | more hub shows 설정 |
| R6 | PASS | settings open via more hub |
| R7 | PASS | back from settings |
| R8 | PASS | voice UI open |
| R9 | PASS | back from voice |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-27 14:50 KST — SM-S931N (R3CY400P2PP) — PASS

**산출물:** `build/device-debug/regression/20260827-145045/`  
**집계:** PASS=21 FAIL=0 SKIP=0

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | PASS | gauge shell labels present |
| R2c | PASS | dual cluster + map header |
| R2d | PASS | SOC% + BT chip |
| R-GPS | PASS | BT OFF chip visible (log hint optional) |
| R3 | PASS | no FATAL for com.myt |
| R13 | PASS | portrait dual shell ok |
| R14 | PASS | landscape dual shell ok |
| R15 | PASS | secondary pane toggled/visible |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R2b | PASS | more hub shows 설정 |
| R6 | PASS | settings open via more hub |
| R7 | PASS | back from settings |
| R8 | PASS | voice UI open |
| R9 | PASS | back from voice |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-27 14:46 KST — SM-S931N (R3CY400P2PP) — PASS

**산출물:** `build/device-debug/regression/20260827-144644/`  
**집계:** PASS=21 FAIL=0 SKIP=0

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | PASS | gauge shell labels present |
| R2c | PASS | dual cluster + map header |
| R2d | PASS | SOC% + BT chip |
| R-GPS | PASS | BT OFF chip visible (log hint optional) |
| R3 | PASS | no FATAL for com.myt |
| R13 | PASS | portrait dual shell ok |
| R14 | PASS | landscape dual shell ok |
| R15 | PASS | secondary pane toggled/visible |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R2b | PASS | more hub shows 설정 |
| R6 | PASS | settings open via more hub |
| R7 | PASS | back from settings |
| R8 | PASS | voice UI open |
| R9 | PASS | back from voice |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-27 14:42 KST — SM-S931N (R3CY400P2PP) — PASS

**산출물:** `build/device-debug/regression/20260827-144242/`  
**집계:** PASS=21 FAIL=0 SKIP=0

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | PASS | gauge shell labels present |
| R2c | PASS | dual cluster + map header |
| R2d | PASS | SOC% + BT chip |
| R-GPS | PASS | BT OFF chip visible (log hint optional) |
| R3 | PASS | no FATAL for com.myt |
| R13 | PASS | portrait dual shell ok |
| R14 | PASS | landscape dual shell ok |
| R15 | PASS | secondary pane toggled/visible |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R2b | PASS | more hub shows 설정 |
| R6 | PASS | settings open via more hub |
| R7 | PASS | back from settings |
| R8 | PASS | voice UI open |
| R9 | PASS | back from voice |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-27 13:35 KST — SM-S931N (R3CY400P2PP) — PASS

**산출물:** `build/device-debug/regression/20260827-133516/`  
**집계:** PASS=21 FAIL=0 SKIP=0

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | PASS | gauge shell labels present |
| R2c | PASS | dual cluster + map header |
| R2d | PASS | SOC% + BT chip |
| R-GPS | PASS | BT OFF chip visible (log hint optional) |
| R3 | PASS | no FATAL for com.myt |
| R13 | PASS | portrait dual shell ok |
| R14 | PASS | landscape dual shell ok |
| R15 | PASS | secondary pane toggled/visible |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R2b | PASS | more hub shows 설정 |
| R6 | PASS | settings open via more hub |
| R7 | PASS | back from settings |
| R8 | PASS | voice UI open |
| R9 | PASS | back from voice |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-27 13:26 KST — SM-S931N (R3CY400P2PP) — PASS

**산출물:** `build/device-debug/regression/20260827-132658/`  
**집계:** PASS=21 FAIL=0 SKIP=0

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | PASS | gauge shell labels present |
| R2c | PASS | dual cluster + map header |
| R2d | PASS | SOC% + BT chip |
| R-GPS | PASS | BT OFF chip visible (log hint optional) |
| R3 | PASS | no FATAL for com.myt |
| R13 | PASS | portrait dual shell ok |
| R14 | PASS | landscape dual shell ok |
| R15 | PASS | secondary pane toggled/visible |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R2b | PASS | more hub shows 설정 |
| R6 | PASS | settings open via more hub |
| R7 | PASS | back from settings |
| R8 | PASS | voice UI open |
| R9 | PASS | back from voice |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-27 13:25 KST — SM-S931N (R3CY400P2PP) — PASS

**산출물:** `build/device-debug/regression/20260827-132531/`  
**집계:** PASS=21 FAIL=0 SKIP=0

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | PASS | gauge shell labels present |
| R2c | PASS | dual cluster + map header |
| R2d | PASS | SOC% + BT chip |
| R-GPS | PASS | BT OFF chip visible (log hint optional) |
| R3 | PASS | no FATAL for com.myt |
| R13 | PASS | portrait dual shell ok |
| R14 | PASS | landscape dual shell ok |
| R15 | PASS | secondary pane toggled/visible |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R2b | PASS | more hub shows 설정 |
| R6 | PASS | settings open via more hub |
| R7 | PASS | back from settings |
| R8 | PASS | voice UI open |
| R9 | PASS | back from voice |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-27 12:33 KST — SM-S931N (R3CY400P2PP) — PASS

**산출물:** `build/device-debug/regression/20260827-123328/`  
**집계:** PASS=21 FAIL=0 SKIP=0

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | PASS | gauge shell labels present |
| R2c | PASS | dual cluster + map header |
| R2d | PASS | SOC% + BT chip |
| R-GPS | PASS | BT OFF chip visible (log hint optional) |
| R3 | PASS | no FATAL for com.myt |
| R13 | PASS | portrait dual shell ok |
| R14 | PASS | landscape dual shell ok |
| R15 | PASS | secondary pane toggled/visible |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R2b | PASS | more hub shows 설정 |
| R6 | PASS | settings open via more hub |
| R7 | PASS | back from settings |
| R8 | PASS | voice UI open |
| R9 | PASS | back from voice |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-27 11:46 KST — SM-S931N (R3CY400P2PP) — PASS

**산출물:** `build/device-debug/regression/20260827-114608/`  
**집계:** PASS=14 FAIL=0 SKIP=0

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | PASS | gauge shell labels present |
| R3 | PASS | no FATAL for com.myt |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R6 | PASS | settings open via more hub |
| R7 | PASS | back from settings |
| R8 | PASS | voice UI open |
| R9 | PASS | back from voice |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---



## 2026-08-26 14:04 KST — SM-S931N (R3CY400P2PP) — PASS

**산출물:** `build/device-debug/regression/20260826-140403/`  
**집계:** PASS=14 FAIL=0 SKIP=0

| ID | Status | Note |
|----|--------|------|
| G0-UNLOCK | PASS |  |
| G0-ADB | PASS | serial=R3CY400P2PP model=SM-S931N |
| R1 | PASS | cold start pid ok |
| R2 | PASS | gauge shell labels present |
| R3 | PASS | no FATAL for com.myt |
| R4 | PASS | history sheet open |
| R11 | PASS | no polyline SQLite error |
| R5 | PASS | back to gauge |
| R6 | PASS | settings open via more hub |
| R7 | PASS | back from settings |
| R8 | PASS | voice UI open |
| R9 | PASS | back from voice |
| R10 | PASS | runtime log pulled |
| R12 | PASS | process alive after suite |

---


