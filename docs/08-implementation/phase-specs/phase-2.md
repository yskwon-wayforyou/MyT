# Phase 2 구현 명세 (M25~M38)

## 목표
- MyT Cloud 백엔드
- 멀티유저 OAuth, 차량 제어, 자동화, Push
- Watch/Widget, 구독, App Store/Play
- v1.0.0

## 백엔드 (`backend/`)

| ID | 모듈 | 기술 | 상태 (2026-08-27) |
|---|---|---|---|
| M25 | Backend Scaffold | Ktor 3 Netty | skeleton |
| M26 | Auth Proxy | OAuth callback / refresh stubs | stub |
| M27 | Telemetry Server | tesla-fleet-telemetry fork | pending |
| M28 | MyT API | `/api/v1/vehicles`, automations, subscriptions | demo data |

## 클라이언트

| ID | 모듈 | 상태 |
|---|---|---|
| M29 | Vehicle Control | `DemoVehicleControlGateway` → **W1 Fleet 실연동** |
| M30 | Quick Controls UI | `QuickControlsPanel` in 차량 상세 |
| M31 | Control Safety | `SafetyGatedVehicleControl` |
| M32 | Automation Engine | demo → **W3** |
| M33 | Push (FCM + APNs) | stub → **W3 FCM** (APNs는 W7) |
| M34 | Apple Watch | **✕ 범위 밖** (의사결정 D4) |
| M35 | Wear OS | **✕ 범위 밖** (의사결정 D4) |
| M36 | Widgets | 미리보기 → **W4 Glance** |
| M37 | Play Billing | sandbox → **W9** (Free 전기능 출시 후) |
| M38 | Store Release + CI/CD | **W5** Play Free |

## Gate G11~G16

| Gate | 조건 | 메모 |
|---|---|---|
| G11 | Auth proxy + Telemetry live | Auth stub only |
| G12 | lock/climate/trunk | API command scaffold + client safety gate |
| G13 | 5+ automation rules | demo 5 rules in API |
| G14 | Watch + Widget | Watch ✕ · Widget → W4 |
| G15 | Billing sandbox | → **W9** |
| G16 | Store 승인 + 100 유저 | → **W5** Free 전기능 |

## 선행
Phase 1.5 G10
