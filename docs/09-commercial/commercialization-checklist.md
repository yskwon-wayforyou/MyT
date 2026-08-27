# 상용 상품화 체크리스트

개인 MVP·데모와 **유료 스토어 상품** 사이의 갭입니다.  
관련: [commercial-constraints.md](../01-research/commercial-constraints.md), [tesla-developer-setup.md](../08-implementation/tesla-developer-setup.md)

## A. Tesla / 백엔드 (필수)

| # | 항목 | 상태 | 비고 |
|---|---|---|---|
| A1 | Tesla Developer **파트너/상용** 앱 등록 | 미완 | **WP** — 품질·Free 안정화 후 재검토 |
| A2 | 공개 도메인 + Public Key 호스팅 | 미완 | **테스트용(W1)** 먼저 · 상용 도메인은 WP |
| A3 | 멀티유저 OAuth (PKCE, refresh, revoke) | 부분 | W1 테스트 안정화 → WP에서 상용 멀티유저 |
| A4 | Virtual Key 페어링 UX + 실패 복구 | 미완 | **W1 테스트 UX** |
| A5 | Fleet Telemetry 서버 (비용 절감) | 부분/설계 | W2–W4 |
| A6 | 실 Command proxy + 감사 로그 | 데모만 | **W1 즉시** (`FleetVehicleControlGateway`) |
| A7 | API 비용 가드 (월 크레딧·한도) | 설계됨 | W1–W2 런타임 |
| A8 | 약관·위치 공유 아이콘 사용자 고지 | 미완 | W1, W5 |

## B. 앱 제품 (차별 + 패리티)

| # | 항목 | 상태 | 비고 |
|---|---|---|---|
| B1 | Gauge / SpeedCam / Voice / BT 자동실행 | 핵심 있음 | 실차 2주 안정화 재확인 |
| B2 | 주행·충전 히스토리·지도 | 있음 | 품질·빈 상태 UX |
| B3 | 원격 제어 (실 API) | 데모 | **W1** 즉시 Fleet |
| B4 | 자동화 + **OS 푸시** | 로컬·인앱 | **W3** FCM |
| B5 | Play Billing + 엔타이틀먼트 | 로컬 플랜 | **W9** (Free 전기능 출시 후) |
| B6 | 홈 위젯 (Glance) | UI 미리보기 | W4 |
| B7 | Wear OS | 미리보기 | **✕ 범위 밖** |
| B8 | Live Camera 실스트림 | 색 프레임 | W4 · **Free 포함** |
| B9 | 데이터 이식 CSV / Tessie import | 있음 | W4 품질 |
| B10 | iOS 패리티 | 지연 | W7→W8 (D4 순서) |

## C. 신뢰·보안·컴플라이언스

| # | 항목 | 상태 |
|---|---|---|
| C1 | Privacy Policy / ToS (공개 URL) | 미완 |
| C2 | Play Data safety 양식 | 미완 |
| C3 | 암호화 저장 (토큰·VIN) | 점검 필요 |
| C4 | 계정 삭제·데이터 export | 부분(CSV) / 계정삭제 미완 |
| C5 | 크래시 리포트 PII 마스킹 | 점검 |
| C6 | 운전 중 주의 고지 (Play 정책) | UI 고지 강화 |

## D. 운영·성장

| # | 항목 | 상태 |
|---|---|---|
| D1 | Play Console 계정·결제 프로필 | 미완 |
| D2 | 스토어 리스팅 (KR/EN), 스크린샷, 피처 그래픽 | 미완 |
| D3 | 내부/비공개/오픈 테스트 트랙 | 미완 |
| D4 | 지원 채널 (이메일·FAQ) | 미완 |
| D5 | 원격 설정 (POI URL, 킬스위치) | 부분(POI) |
| D6 | 관측 (크래시율, ANR, Fleet 비용 대시보드) | 부분 |

## E. 출시 게이트 (권장)

1. 실차 Gauge + SpeedCam 2주 무중단 (기존 Phase 1 AC)
2. OAuth + Virtual Key 신규 사용자 온보딩 E2E
3. 제어 명령 실차 10종 × 안전 게이트 통과
4. Billing 샌드박스 — **W9** (Free 출시 게이트에서 제외)
5. Play Pre-launch report + 정책 위반 0건 — **W5**
6. 개인정보·데이터 삭제 요청 처리 플레이북 — **W5**

원본 Wave·ID 표: [decisions-and-backlog.md](./decisions-and-backlog.md)
