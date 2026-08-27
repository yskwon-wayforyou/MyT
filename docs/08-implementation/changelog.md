# MyT Changelog

## [0.2.1] — 2026-08-26

### Fixed (device QA)
- History crash: SQLDelight migration `2.sqm` adds `polyline_encoded` / `efficiency_km_per_kwh`
- Empty Fleet `vehicle_data`: wake + cache soft-fail (no GitHub spam)
- Tire pressure: Fleet TPMS is already bar — removed incorrect PSI→bar conversion
- Voice STT no-match (error 7): warn only, do not auto-file GitHub issue

### Added
- Real-time WARN/ERROR file log: `files/debug_logs/myt-runtime.log` (rotated)
- Crash → `files/crash_reports/` + pending GitHub issue queue `files/pending_github_issues/`
- Cold-start `CrashIssueSyncUseCase`: bootstrap last crash + upload when `github.issues.token` set
- Agent triage: `/crash-log-triage` + `scripts/crash_log_triage.sh` (adb pull + `gh issue create`)
- Cursor rule `.cursor/rules/crash-log-triage.mdc`

### Config
```
github.issues.enabled=true
github.issues.repo=yskwon-wayforyou/MyT
github.issues.token=   # optional; empty → agent `gh` path
```

---

## [0.2.0] — 2026-08-20

### Added
- Phase 1 + 1.5 feature set: Gauge cluster, History, Voice, SpeedCam, Quota, Debug logs
- Kable BLE presence scan (Android ACL + BLE, iOS BLE)
- Hybrid Telemetry stream (WSS URL + REST polling fallback)
- National POI bootstrap (30-camera bundle) + OTA CSV sync + `fetch_national_poi.py`
- Android OSM Leaflet map route; iOS canvas polyline fallback
- iOS Xcode project (`iosApp.xcodeproj`), Info.plist, SpeechHelper.swift
- Release signing via `androidApp/keystore.properties` (optional)
- ProGuard rules, unit tests (Fleet mock, Vin, AppStateMachine, Telemetry payload)

### Build
- Debug/Release APK: `./scripts/build-android-apk.sh`
- iOS framework: `./scripts/build-ios-framework.sh` (requires Xcode license)

### Known gaps (ops / external)
- Xcode license must be accepted for iOS simulator compile (`sudo xcodebuild -license`)
- Navigation Compose 2.9.2 KLIB ABI needs Kotlin 2.2+ for iOS (Android OK on Kotlin 2.1)
- Firebase Crashlytics: local crash file reporter used until `google-services.json` is added
- Signed release needs real keystore; IPA needs Apple Team ID
- 2-week vehicle stability (AC-ST) is a field validation gate

---

## [0.0.1-scaffold] — 2026-08-20

Initial project scaffold.

---

## Planned

| Version | Phase | Target |
|---|---|---|
| v0.2.0 | 1.5 | Trip + Telemetry + Map ✅ code |
| v1.0.0 | 2 | Store release |
| v1.x | 3 | HA + Web + Analytics |
