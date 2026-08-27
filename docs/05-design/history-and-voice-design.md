# 히스토리 · 로컬 캐시 · 음성 명령 설계

## 목표

- **히스토리 허브**: 주행·충전·Fleet API 사용 내역을 필터·정렬·차트로 시각화
- **로컬 우선**: Fleet API 응답은 SQLDelight에 저장하고, 폴링 간격 내에서는 재호출하지 않음
- **음성 확장**: 전화·문자·카카오·TTS·내비·화면 이동을 한 음성 진입점에서 처리

## 데이터 모델 (SQLDelight)

| 테이블 | 용도 |
|--------|------|
| `trip_record` | 주행 세션 (기어·속도·주행거리·SOC) |
| `charge_session` | 충전 세션 (SOC·에너지·피크 kW) |
| `fleet_api_event` | Fleet 호출 카테고리·성공 여부·상세 |
| `vehicle_snapshot` | VIN별 최신 `GaugeState` JSON 캐시 |

## 기록 파이프라인

```
TelemetryUseCase (폴링)
  ├─ cache hit (age < interval) → DB 스냅샷만 표시, API 스킵
  ├─ fetch success → KtorFleetRepository.saveVehicleSnapshot + LocalTrip/ChargeRecorder
  └─ fetch fail / quota block → loadVehicleSnapshot 폴백

FleetQuotaUseCase.record → fleet_api_event INSERT
```

### 캐시 TTL 정책

- `TelemetryUseCase`가 현재 상태(주행/주차/충전/슬립)에 맞는 폴링 간격을 계산
- `vehicle_snapshot.updated_at_ms`가 간격 미만이면 **Fleet API 호출 생략**
- 수동 새로고침(`refreshOnce`)은 캐시 무시하고 강제 fetch

## 히스토리 UI

- **Route.History** — 게이지 ActionBar 「히스토리」 또는 음성 「히스토리/기록」
- 탭: 주행 | 충전 | Fleet API
- 필터: 7일 / 30일 / 전체, 탭별 정렬 (최신·거리·충전량·카테고리)
- **일별 막대 차트** + 카드형 테이블 목록

## 음성 명령 (`VoiceCommandUseCase`)

| 발화 예 | 동작 |
|---------|------|
| 히스토리 / 기록 / 내역 | History 화면 |
| 설정 | Settings |
| 계기 / 게이지 / 홈 | Gauge |
| 전화 … | `ACTION_DIAL` |
| 문자 … | SMS 앱 또는 `smsto:` |
| 카카오 … | KakaoTalk 공유 Intent |
| 읽어줘 | TTS (최근 Fleet 이벤트 상세 등) |
| 내비 / 목적지 … | Tesla Fleet navigation command |

### 플랫폼 제약

- **Android**: 전화·문자·카카오 공유·TTS 구현. 카카오 **수신 알림 읽기**는 Notification Listener 권한이 필요해 후속 Phase에서 별도 서비스로 확장.
- **iOS**: Communications/TTS 스텁 — CallKit·Messages URL scheme 연동 예정.

## DI (Koin)

- `MyTDatabase`, `SqlHistoryRepository`, `LocalTripRecorder`, `LocalChargeSessionRecorder`
- `HistoryUseCase`, `HistoryViewModel`, `VoiceCommandUseCase`
- `FleetQuotaUseCase(historyRepository)`, `KtorFleetRepository(..., historyRepository)`

## 향후 확장

- CSV/JSON 내보내기, 월별 요금 추정 그래프
- Notification Listener 기반 카카오·SMS 수신 읽기
- iOS Siri Shortcuts / Android App Actions
- 주행 히스토리 지도 오버레이 (GPS 연동 시)
