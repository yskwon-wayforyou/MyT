# Phase 3 구현 명세 (M39~M45)

## 목표
- Home Assistant, Web Dashboard, 고급 분석
- v1.x 지속 업데이트

| ID | 모듈 | 구현 |
|---|---|---|
| M39 | Home Assistant | MQTT discovery JSON + HA REST state bridge |
| M40 | HomeKit / Alexa | Siri Shortcuts, Alexa skill |
| M41 | Web Dashboard | `backend` `/dash` read-only stub → dash.myt.app |
| M42 | Advanced Analytics | Battery health chart, CO₂ summary |
| M43 | Data Import/Export | Tessie CSV import, trip/charge CSV export |
| M44 | Live Camera | 색프레임 → **W4 실스트림 (Free 포함)** |
| M45 | Carbon Badge | CO₂ tier gamification |

## Gate G17~G20

| Gate | 조건 | 검증 |
|---|---|---|
| **G17** | HA entity | `HaRestStateBridge` + discovery JSON; HA REST `sensor.myt_*` 업데이트 |
| **G18** | Web control live | `/dash` + `/api/v1/dashboard/state`; Phase 2 live control 후 확장 |
| **G19** | Battery degradation graph | `BatteryAnalyticsUseCase` + Analytics 화면 차트 |
| **G20** | Import/export | `SqlDataPortability` CSV + Tessie import |

## 코드 위치 (2026-08-27)

| 모듈 | 패키지/경로 |
|---|---|
| M39 | `composeApp/.../phase3/HaRestStateBridge.kt`, `HaDiscoveryBuilder.kt` |
| M41 | `backend/.../routes/DashboardRoutes.kt` |
| M42/M45 | `BatteryAnalyticsUseCase`, `CarbonBadgeUseCase`, `ui/analytics/` |
| M43 | `SqlDataPortability`, `TessieCsvParser` |
| M44 | `LiveCameraClient` stub |

## 선행
Phase 2 G16 (Store) — M41 live control, M44 stream은 Phase 2 Fleet 제어·인증 후 완성
