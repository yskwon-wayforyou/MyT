# Phase 2 구현 명세 (M25~M38)

## 목표
- MyT Cloud 백엔드
- 멀티유저 OAuth, 차량 제어, 자동화, Push
- Watch/Widget, 구독, App Store/Play
- v1.0.0

## 백엔드 (`backend/`)

| ID | 모듈 | 기술 |
|---|---|---|
| M25 | Backend Scaffold | Ktor 3, PostgreSQL, Redis, Docker |
| M26 | Auth Proxy | OAuth token vault, virtual key |
| M27 | Telemetry Server | tesla-fleet-telemetry fork |
| M28 | MyT API | User/Vehicle/Subscription REST |

## 클라이언트

| ID | 모듈 |
|---|---|
| M29 | Vehicle Control (lock, climate, trunk) |
| M30 | Quick Controls UI |
| M31 | Control Safety (driving restrictions) |
| M32 | Automation Engine |
| M33 | Push (FCM + APNs) |
| M34 | Apple Watch |
| M35 | Wear OS |
| M36 | Widgets + Live Activity |
| M37 | Play Billing + StoreKit 2 |
| M38 | Store Release + CI/CD |

## Gate G11~G16

## 선행
Phase 1.5 G10
