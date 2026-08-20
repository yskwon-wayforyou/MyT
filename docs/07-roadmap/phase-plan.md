# Phase 로드맵

## 1. Phase 개요

```mermaid
gantt
  title MyT_Development_Roadmap
  dateFormat YYYY-MM
  axisFormat %Y-%m

  section Phase_0
  문서_설계           :done, p0, 2026-08, 2026-08

  section Phase_1
  KMP_프로젝트_셋업    :p1a, 2026-08, 2026-09
  Fleet_API_연동      :p1b, 2026-09, 2026-10
  Gauge_UI_개발        :p1c, 2026-09, 2026-10
  BT_자동실행          :p1d, 2026-10, 2026-10
  SpeedCam_엔진        :p1e, 2026-10, 2026-11
  Voice_Nav            :p1f, 2026-10, 2026-11
  적응형_레이아웃       :p1g, 2026-10, 2026-11
  실차_테스트_안정화    :p1h, 2026-11, 2026-12

  section Phase_1_5
  주행_충전_기록        :p15a, 2026-12, 2027-01
  Fleet_Telemetry      :p15b, 2026-12, 2027-02
  지도_경로_표시        :p15c, 2027-01, 2027-02

  section Phase_2
  백엔드_구축          :p2a, 2027-02, 2027-04
  멀티유저_OAuth       :p2b, 2027-03, 2027-04
  차량_제어            :p2c, 2027-03, 2027-05
  자동화_알림          :p2d, 2027-04, 2027-05
  구독_결제            :p2e, 2027-04, 2027-05
  Watch_위젯           :p2f, 2027-05, 2027-06
  스토어_배포          :p2g, 2027-05, 2027-06

  section Phase_3
  Home_Assistant       :p3a, 2027-06, 2027-08
  Web_대시보드         :p3b, 2027-07, 2027-09
  고급_분석            :p3c, 2027-07, 2027-09
```

## 2. Phase 0: 문서 · 설계 (완료)

| 항목 | 상태 | 산출물 |
|---|---|---|
| Tesla API/BLE 조사 | ✅ | tesla-api-bluetooth-findings.md |
| 과속카메라 데이터 조사 | ✅ | korea-speed-camera-data.md |
| 경쟁앱 분석 | ✅ | competitor-apps-analysis.md |
| 크로스플랫폼 기술 조사 | ✅ | cross-platform-tech-stack.md |
| 제품 개념 | ✅ | product-concept.md |
| 기능/비기능 요구사항 | ✅ | functional/non-functional-requirements.md |
| 기능 카탈로그 (85+) | ✅ | feature-catalog.md |
| 표시 정보 명세 | ✅ | display-specifications.md |
| 수락 기준 | ✅ | acceptance-criteria-phase1.md |
| 시스템 아키텍처 | ✅ | system-architecture.md |
| 상세 설계 + 콘티 | ✅ | detailed-design.md, app-conti.md |

## 3. Phase 1: 개인 차량 MVP

**목표:** 본인 Model 3에서 Gauge + SpeedCam + Voice Nav 2주 무중단

### 3.1 마일스톤

```mermaid
flowchart LR
  M1[M1_KMP_Setup] --> M2[M2_Fleet_API]
  M2 --> M3[M3_Gauge_UI]
  M3 --> M4[M4_BT_AutoLaunch]
  M4 --> M5[M5_SpeedCam]
  M5 --> M6[M6_VoiceNav]
  M6 --> M7[M7_Adaptive_Layout]
  M7 --> M8[M8_Integration_Test]
  M8 --> M9[M9_2Week_Stability]
```

| Milestone | 내용 | 완료 기준 |
|---|---|---|
| M1 | KMP + Compose Multiplatform 프로젝트 셋업 | Android + iOS 빌드 성공 |
| M2 | Fleet API OAuth + vehicle_data 폴링 | 본인 VIN 데이터 수신 |
| M3 | Gauge UI (속도, SOC, 기어, 온도) | Scene 12 구현 |
| M4 | BT 자동 실행 (Android + iOS) | AC-C01~C03 통과 |
| M5 | SpeedCam 엔진 + POI DB | AC-S01~S06 통과 |
| M6 | Voice Nav (STT + navigation_request) | AC-N01~N05 통과 |
| M7 | 적응형 레이아웃 (폰/태블릿) | AC-L01~L07 통과 |
| M8 | 통합 테스트 | AC 42/43 통과 |
| M9 | 2주 실차 안정화 | AC-ST01~06 통과 |

### 3.2 Phase 1 기능 (20개)

A01~A20 (feature-catalog.md 참조)

### 3.3 Phase 1 제외

- 멀티 차량 / 멀티 사용자
- 차량 원격 제어
- 주행/충전 기록
- 자동화 / Push 알림
- Watch / 위젯
- 구독 / 결제
- 백엔드 서버

## 4. Phase 1.5: 확장

| 기능 | 설명 |
|---|---|
| 주행 자동 기록 | Trip start/end, map, efficiency |
| 충전 세션 기록 | Session log, cost |
| Fleet Telemetry | 폴링 → WebSocket stream |
| 지도 경로 표시 | RouteLine polyline decode + map |
| POI DB OTA | 월간 자동 갱신 |
| Crashlytics | Firebase crash reporting |

## 5. Phase 2: 유상 배포

### 5.1 인프라

```mermaid
flowchart TB
  subgraph backend [MyT_Backend]
    Auth[Auth_Proxy]
    Telemetry[Fleet_Telemetry_Server]
    API[MyT_API]
    DB[(PostgreSQL)]
    Cache[(Redis)]
  end
  subgraph clients [Clients]
    iOS[MyT_iOS]
    Android[MyT_Android]
    Watch[Watch_Apps]
    Web[Web_Dashboard]
  end
  subgraph external [External]
    Tesla[Tesla_Fleet_API]
    Play[Google_Play]
    AppStore[App_Store]
  end

  iOS & Android --> API
  Watch --> API
  Web --> API
  API --> Auth --> Tesla
  API --> Telemetry --> Tesla
  API --> DB
  API --> Cache
  iOS --> AppStore
  Android --> Play
```

### 5.2 Phase 2 기능 (+45)

- Control (D01~D15): 잠금, 클라이-mate, 트렁크 등
- Auto (E01~E15): 스케줄, 트리거, 알림
- Platform (F03~F18): Watch, 위젯, Siri, 구독
- Analytics (B04~B17): FSD, CO₂, 비교, 배지

### 5.3 상용 준비

| 항목 | 내용 |
|---|---|
| Tesla Partner 등록 | 법인, 도메인, 공개키 |
| App Store / Play 등록 | 심사, 스크린샷, 설명 |
| 개인정보처리방침 | 위치·운행·차량 데이터 |
| 구독 모델 | Play Billing + StoreKit 2 |
| Fleet Telemetry 서버 | AWS/GCP 호스팅 |
| CI/CD | GitHub Actions → Store |

## 6. Phase 3: 고도화

| 기능 | 설명 |
|---|---|
| Home Assistant | MQTT/Webhook 연동 |
| HomeKit / Alexa | 스마트홈 |
| Web Dashboard | 브라우저 제어 |
| 고급 분석 | Grafana-style dashboards |
| 데이터 가져오기 | Tessie, TezLab import |
| Carbon Offset | CO₂ 상쇄 |
| Live Camera | Tesla Live Camera view |

## 7. 리스크 · 대응

| 리스크 | Phase | 대응 |
|---|---|---|
| Fleet API 비용 증가 | 1.5 | Telemetry 전환 |
| iOS BT 자동 실행 불가 | 1 | Notification + Shortcuts |
| Tesla API 변경 | All | SDK 업데이트, changelog 모니터 |
| App Store 심사 거절 | 2 | 운전 중 UI 가이드라인 준수 |
| BLE 3연결 제한 | 1 | 연결 감지만, 직접 BLE 최소 |
| Compose MP iOS 버그 | 1 | JetBrains 이슈 트래킹, fallback |

## 8. 다음 단계 (Phase 1 시작)

1. KMP + Compose Multiplatform 프로젝트 생성
2. Tesla Developer 계정 + Fleet API 등록
3. Fleet API OAuth + vehicle_data 연동
4. Gauge UI 프로토타입 (Scene 12)
5. 실차 테스트 시작

```mermaid
flowchart TD
  Now[Phase0_문서_완료] --> Next1[KMP_프로젝트_생성]
  Next1 --> Next2[Tesla_Developer_등록]
  Next2 --> Next3[Fleet_API_OAuth]
  Next3 --> Next4[Gauge_UI_프로토타입]
  Next4 --> Next5[실차_테스트]
```
