# Phase 1.5 구현 명세 (M18~M24)

## 목표
- 주행/충전 자동 기록
- Fleet Telemetry WebSocket 스트림
- 지도 경로 + History UI
- v0.2.0

## 모듈

| ID | 모듈 | 구현 | 스캐폴드 |
|---|---|---|---|
| M18 | Trip Recorder | Gear D→P 감지, polyline, efficiency | `TripRecorder.kt` |
| M19 | Charge Session | ChargeState 변화, kWh, push | `ChargeSessionRecorder.kt` |
| M20 | Telemetry Client | WSS, config, stream→GaugeState | `TelemetryStreamClient.kt` |
| M21 | Map Route UI | MapLibre, RouteLine decode | `MapRoutePlaceholder.kt` |
| M22 | History UI | ListDetail, calendar | Route.TripHistory |
| M23 | POI OTA | Background download | pending |
| M24 | Crashlytics | Firebase | pending |

## Gate
- G7: 100+ records
- G8: latency -50%
- G9: map polyline
- G10: v0.2.0

## 선행
Phase 1 G6 통과
