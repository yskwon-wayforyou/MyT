# Stub → 데모 실동작 현황

기준일: 2026-08-27  
범위: Phase 2/3 stub·scaffold를 앱에서 확인 가능한 데모로 연결한 상태.  
**의사결정:** [decisions-and-backlog.md](./decisions-and-backlog.md) — 실 Fleet(W1) 즉시 · Watch/Wear ✕ · Free 전기능 후 유료화(W9)

## 1. 동작하는 데모

| 영역 | 구현 | 확인 경로 | 한계 / 다음 |
|---|---|---|---|
| 차량 제어 | `DemoVehicleControlGateway` | Quick Controls | → **W1 Fleet 실명령**으로 교체 |
| 인앱 푸시 | Snackbar | 제어·자동화 | → **W3 FCM** |
| 자동화 | 로컬 트리거 수종 | 더보기 규칙 | → W3 확장 |
| Live Camera | 색 프레임 | Analytics | → **W4 실스트림 (Free)** |
| 구독 UI | Local Free/Plus/Pro | Commercial Hub | 출시 전 숨김 가능 · **W9** Billing |
| Watch 미리보기 | InMemory bridge | Commercial Hub | **✕ 제거 예정** |
| 위젯 미리보기 | UI 카드 | Commercial Hub | → **W4 Glance** |
| FSD 추정 | 시뮬 누적 | Analytics | → W4 Telemetry |
| HA / Web / CSV | 부분 실연동 | 설정·백엔드·Analytics | W4 품질 |

## 2. 검증 (2026-08-27)

| 검증 | 결과 |
|---|---|
| 유닛 · 리그레션 21/21 · 드라이브 시뮬 · commercial probe | PASS |

단말 테스트 중 수정: 더보기 허브 구독 노출, CrashSync WARN 집약.

## 3. 의도적 갭 (Wave)

- W1: 실 Fleet · Auth/VK 테스트
- W3: FCM
- W4: Camera · Glance · Watch UI 제거
- W5: Play Free
- W9: Billing
- WP: Partner 상용
- ✕: Wear/Watch
