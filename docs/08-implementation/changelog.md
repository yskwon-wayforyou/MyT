# MyT Changelog

## [Unreleased] — Phase 1 Scaffold

### Added
- KMP + Compose Multiplatform project (`composeApp`, `androidApp`)
- Domain models: GaugeState, SpeedCamEngine, UnitConverter
- UI: GaugeScreen, SpeedCamOverlay, VoiceNavDialog, Onboarding, Settings
- Platform abstractions (Android + iOS stubs)
- SQLDelight schema for speed_camera POI
- Unit tests: UnitConverter, SpeedCamEngine
- Implementation tracking: progress-tracker, gates, phase specs
- Backend skeleton for Phase 2 (`backend/`)
- Phase 1.5 module stubs (`phase15/`)

### Known Issues
- JDK 17+ required for Gradle build (not installed on dev machine)
- iosApp Xcode project pending
- Fleet API OAuth not connected (Tesla Developer account required)
- POI data bundle not imported (15K cameras)

---

## [0.0.1-scaffold] — 2026-08-20

Initial project scaffold. Not a release build.

---

## Planned

| Version | Phase | Target |
|---|---|---|
| v0.1.0 | 1 | Gauge MVP APK/IPA |
| v0.2.0 | 1.5 | Trip + Telemetry + Map |
| v1.0.0 | 2 | Store release |
| v1.x | 3 | HA + Web + Analytics |
