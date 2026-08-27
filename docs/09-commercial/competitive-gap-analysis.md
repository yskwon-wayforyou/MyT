# 경쟁력·기능 품질 비교 및 갭 보완

기준일: 2026-08-27  
**의사결정 반영:** [decisions-and-backlog.md](./decisions-and-backlog.md) (D1–D5)

기호: `●` 상용급 · `◐` 부분/데모 · `○` 있음(경쟁) · `-` 없음 · `★` MyT 차별 · `✕` MyT 범위 밖

시각 요약: Canvas `myt-competitive-commercial.canvas.tsx`


---

## 1. 제품 포지션

| | MyT (상용 목표) | Tessie | TezLab | Stats | Nikola | Teslascope | Tesla 공식 |
|---|---|---|---|---|---|---|---|
| 핵심 약속 | **주행 중 전체화면 Gauge + 한국 과속단속** | 올인원 원격·자동화 | EV 분석·FSD | 배터리·효율 | 보안·알림 | 웹 분석·자동화 | 기본 제어·카메라 |
| 주 사용 시점 | **운전 중** | 주차·원격 | 주행 후 | 주차 | 원격 | 데스크톱 | 원격 |
| 플랫폼 | Android 폰→탭→iOS (Watch ✕) | iOS/And/Watch/Web | iOS/And | iOS | iOS/Watch | Web/iOS | iOS/And |
| 가격대(참고) | **Free 전기능 선출시** → 이후 Plus/Pro | ~$6/월 | 구독 | 일회/구독 | ~$10/월 | 구독 | 무료 |

---

## 2. 기능 비교표 (세부분야)

### 2.1 주행 중 경험 · 안전 (MyT 핵심)

| 기능 | MyT | Tessie | TezLab | Stats | Nikola | Teslascope | 공식 | 품질 메모 |
|---|---|---|---|---|---|---|---|---|
| 전체화면 속도계 Gauge | ★● | - | - | - | - | - | - | 대형 타이포·적응 레이아웃 |
| 기어/전력/G/타이어 | ● | ◐ | - | - | - | - | ◐ | 경쟁은 카드형 소형 |
| BT Phone Key 자동 실행 | ★● | - | - | - | - | - | - | 운전 시작 UX |
| 한국 과속·구간 단속 | ★● | - | - | - | - | - | - | 로컬 POI+OTA |
| 음성 목적지 → 차량 내비 | ★● | - | - | - | - | - | ◐ | 공식은 앱 내 내비 |
| 가로/태블릿 적응 | ● | ◐ | - | - | - | - | ◐ | |

**판정:** 이 영역은 MyT가 **명확한 1위**. 상용 후에도 유지해야 할 USP.

### 2.2 원격 제어 · 차량 상태

| 기능 | MyT 상용 | 현재 | Tessie | TezLab | Stats | Nikola | Teslascope | 공식 |
|---|---|---|---|---|---|---|---|---|
| 잠금/해제·공조 | ● | ◐데모 | ○ | ○ | ○ | ○ | ○ | ○ |
| 트렁크/프렁크·경적 | ● | ◐ | ○ | ○ | ○ | ○ | ◐ | ○ |
| Dog/Camp/Sentry | ◐목표 | - | ○ | ◐ | ◐ | ◐ | ◐ | ○ |
| Live Camera | ● Free 목표 | ◐색프레임 | ◐ | - | - | - | - | ● |
| Quick Controls | ● | ●UI | - | ○ | - | - | - | - |
| 다중 차량 | ● | ◐ | ○ | ○ | ○ | ○ | ○ | ○ |

**갭:** 제어 패리티·Live Camera·모드 토글.  
**보완:** 실 Fleet Command(W1) → Dog/Camp/Sentry(W3) → Live Camera 실스트림 Free 포함(W4).

### 2.3 자동화 · 알림

| 기능 | MyT 상용 | 현재 | Tessie | TezLab | Stats | Nikola | Teslascope |
|---|---|---|---|---|---|---|---|
| 조건 트리거 자동화 | ● | ◐로컬3종 | ○ | ○ | ◐ | ◐ | ○ |
| OS 푸시 (FCM/APNs) | ● | ◐인앱 | ○ | ○ | ○ | ○ | ○ |
| 지오펜스 | ● | - | ○ | - | - | - | ○ |
| Sentry 이벤트 | ◐ | - | ○ | - | - | ○ | ○ |
| 스케줄 프리컨디션 | ● | - | ○ | ○ | - | - | ○ |

**갭:** Nikola/Tessie 수준의 “차 두고 나와도 안심” 알림.  
**보완:** FCM 우선 → 지오펜스 → Sentry 웹훅.

### 2.4 분석 · 배터리 · 데이터

| 기능 | MyT 상용 | 현재 | Tessie | TezLab | Stats | Teslascope |
|---|---|---|---|---|---|---|
| 주행/충전 히스토리 | ● | ● | ○ | ○ | ○ | ○ |
| 배터리 degradation | ● | ◐ | ○ | ○ | ○★ | ○ |
| FSD/AP 통계 | ◐ | ◐시뮬 | ○ | ○★ | - | ○ |
| CO₂/배지 | ● | ● | ○ | ○ | ○ | - |
| CSV·타앱 import | ● | ● | ○ | - | ○ | ○ |
| 커뮤니티 비교 | - | - | ○ | ○ | ○ | - |

**갭:** Stats/TezLab의 배터리·FSD 깊이, 소셜 비교.  
**보완:** Telemetry 기반 실 AP 필드 → 배터리 장기 곡선 → (후순위) 익명 비교.

### 2.5 플랫폼 · 생태계

| 기능 | MyT 상용 | 현재 | Tessie | 기타 |
|---|---|---|---|---|
| Android 폰/태블릿 | ● | ● | ○ | |
| iOS | ●목표 | 지연 | ○ | Stats/Nikola iOS 강세 |
| Watch / Wear | ✕ | ✕ 미리보기 제거 | ○ Watch | 미지원 확정 |
| 홈 위젯 | ● Glance | 미리보기 | ○ | W4 |
| Web 대시보드 | ◐ | 백엔드있음 | ○ | Teslascope 강점 |
| HA / HomeKit / Alexa | ◐HA | HA부분 | ○전부 | |

---

## 3. 품질 축 비교 (주관 + 관찰)

| 품질 축 | MyT (상용 가정) | Tessie | TezLab | Stats | Nikola | 공식 | MyT 보완 포인트 |
|---|---|---|---|---|---|---|---|
| 주행 중 UI 가독성 | 9 | 4 | 3 | 3 | 3 | 5 | 유지·야간 대비 강화 |
| 원격 제어 신뢰성 | 7 | 9 | 8 | 7 | 8 | 9 | 실명령·재시도·상태 동기 |
| 알림 적시성 | 7 | 9 | 7 | 7 | 9 | 8 | FCM·Doze 대응 |
| 분석 깊이 | 6 | 8 | 9 | 9 | 5 | 3 | FSD·배터리 장기 |
| 온보딩 단순함 | 6 | 8 | 7 | 7 | 7 | 9 | VK·권한 가이드 |
| 한국 로컬 가치 | 9 | 3 | 3 | 2 | 2 | 4 | SpeedCam 데이터 품질 SLA |
| 가격 대비 | 8 | 7 | 7 | 8 | 6 | 10 | Free Gauge+Cam 유지 권장 |
| 크로스플랫폼 | 6 | 9 | 7 | 4 | 5 | 9 | iOS 부채 해소 |

점수: 1–10 상대 평가 (출시 가정).

---

## 4. 부족한 점 → 보완 (결정 반영 Wave)

| 우선 | 부족 | 보완 | Wave |
|---|---|---|---|
| P0 | 실 Fleet 제어 | Command gateway + 실차 QA | W1 |
| P0 | Auth/VK **테스트** 경로 | OAuth·페어링 UX (Partner는 이후) | W1 |
| P0 | 운전 중 경험 품질·실차 안정 | SpeedCam SLA, 2주 게이트 | W2 |
| P0 | OS 푸시 | FCM | W3 |
| P1 | 제어·자동화 패리티 | 모드·지오펜스·스케줄 | W3 |
| P1 | Live Camera·Glance·분석 깊이 | Free 패키지 | W4 |
| P1 | Play 정책·리스팅 | Free 전기능 출시 | W5 |
| P2 | Android 태블릿 / iOS | D4 순서 | W6–W8 |
| P2 | Free/Plus/Pro·Billing | 안정화 후 | W9 |
| P3 | Tesla Partner 상용 | 재검토 | WP |
| ✕ | Watch/Wear | 하지 않음 | — |

상세 ID 표: [decisions-and-backlog.md](./decisions-and-backlog.md) §3

---

## 5. 경쟁 전략 (개정)

> **운전 중 경험 카테고리**(Gauge · BT 자동실행 · 한국 과속단속 · 음성 내비)를 최고 수준으로 만들고,  
> 제어·알림·카메라·분석까지 **Free 한 앱에 담아** Play에 낸 뒤 안정화한다.  
> Tessie를 가격으로 이기기보다 **주행 중 UX로 카테고리를 선점**하고, 유료화(W9)는 사용 데이터 보고 나눈다.  
> Watch/Wear는 하지 않는다.

---

## 6. 참고

- [decisions-and-backlog.md](./decisions-and-backlog.md)
- [competitor-apps-analysis.md](../01-research/competitor-apps-analysis.md)
- [commercialization-checklist.md](./commercialization-checklist.md)
- [schedule.md](./schedule.md)
