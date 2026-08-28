# 과속카메라 품질 — 비교 분석 및 MyT 개선 보고

기준일: 2026-08-28  
대상: 실차 QA 14건 중 단속·지도·속도 관련 (항목 4–11)

## 1. 비교 앱 요약

| 앱 | 전방 필터 | 제한속도 미만 안내 | 지도 회전 | 속도 0 반응 | 오탐(역방향) |
|---|---|---|---|---|---|
| **T map** | 도로 진행방향 + 차선 | 거리 표시 O | 헤딩업 · 차량 하단 | GPS 즉시 | 낮음 |
| **카카오내비** | 진행방향 cone | 거리 표시 O | 헤딩업 | 양호 | 낮음 |
| **아이나비** | 도로방향 + bearing | 거리 표시 O | 헤딩업 | 양호 | 낮음 |
| **MyT (개선 전)** | roadDirection ±45° only | 과속 시만 | 북쪽 고정 | EMA 잔류 4–10 | **역방향 오탐** |
| **MyT (이번 개선)** | forward cone 35° + road align 30° | L1 거리 항상 | 헤딩업 + 오프셋 | ≤1.5 km/h → 0, 100ms GPS | synthetic 테스트 통과 |

## 2. 이번 릴리스 변경 (요약)

### 주행·속도 (4, 5)
- Device GPS 100ms 주기, 정지 시 즉시 0 km/h (EMA 우회).
- 감속 중 EMA α=0.85로 빠른 추종.

### 지도·헤딩 (5–7)
- OSM 지도 **헤딩업** 회전 (`mapOrientation = -heading`).
- 차량 아이콘은 전방(위) 기준, 중심 **상단 2/3** 부근에 보이도록 center offset.
- 카메라 아이콘 강조(빨간 링) + 점멸 pulse, 역방향 제외.

### 단속 알고리즘 (9–10)
- `SpeedCamMatcher`: bearing-to-camera ≤35°, roadDirection 정렬 ≤30°, 역주행 차선 제거.
- 제한속도 이하도 전방 카메라면 **L1 + 거리 표시**.
- 경고 UI: 속도 게이지 유지 + **빨간 제한속도 arc** + radial gradient + 큰 거리/제한 숫자.

### 품질 목표
- Synthetic fixture: 역방향·후방 카메라 reject → **SpeedCamMatcherTest** 5케이스.
- 실차 99% 유효성: `SpeedCamMatcherTest` + `drive_simulation_test.sh` 스모크; 실차 로그 수집은 다음 주행 QA에서 `files/debug_logs` 기준 집계.

## 3. 잔여·후속

| 항목 | 상태 |
|---|---|
| FCM 원격 푸시 | 로컬 알림 + 딥링크 완료, Firebase 후속 |
| 음성 내비 실차 | wake 재시도 추가, 실패 시 GitHub issue enqueue |
| BT 자동 실행 | ACL/BLE → MainActivity foreground |
| 방향 잠금 | 세로 0° / 가로 270° (T map 방식) |
| 차량 sleep→drive | BT 연결 시 즉시 Fleet refresh, sleep 60s 폴링 |

## 4. 검증 방법

```bash
./gradlew :composeApp:testDebugUnitTest --tests "com.myt.domain.SpeedCamMatcherTest"
./scripts/drive_simulation_test.sh
./scripts/device_apk_debug_loop.sh all
```

실차: 역방향 고속도로 / 신호 대기 정지 / 가로·세로 전환 / BT 탑승 자동 실행을 체크리스트로 재확인.
