# MyT 실단말 리그레션 스위트

정형화된 **디바이스 UI/런타임 리그레션** 정의입니다.  
실행기: `scripts/device_regression_test.sh`  
결과 히스토리: [device-regression-history.md](./device-regression-history.md)

## 실행 방법

```bash
# 권장: 빌드 + 설치 + 리그레션 (단말이 device 상태여야 함)
./scripts/build-install-regress.sh

# 리그레션만 (이미 설치된 APK 기준)
./scripts/device_regression_test.sh

# 빌드만 하고 리그레션 생략
SKIP_DEVICE_REGRESSION=1 ./scripts/build-android-apk.sh
```

환경 변수:

| 변수 | 기본 | 설명 |
|------|------|------|
| `ANDROID_SERIAL` | 자동(1대) | 다중 단말 시 시리얼 |
| `SKIP_DEVICE_REGRESSION` | unset | `1`이면 빌드/설치 후 리그레션 생략 |
| `MYT_PACKAGE` | `com.myt` | 패키지명 |
| `REGRESSION_STRICT` | `1` | `0`이면 soft-fail(경고만, exit 0) |

## 사전 조건 (Gate)

| ID | 조건 | 실패 시 |
|----|------|---------|
| G0-ADB | `adb devices`에 `device` 1대 이상 | FAIL — USB/디버깅 확인 |
| G0-UNLOCK | 잠금화면 아님 (`잠금해제` 텍스트 없음) | FAIL — 패턴 해제 요청 |
| G0-APK | debug APK 존재 또는 빌드 성공 | FAIL |

## 테스트 케이스

| ID | 이름 | 절차 | 합격 기준 |
|----|------|------|-----------|
| R1 | Cold start | force-stop → launch → 3s 대기 | `com.myt` pid 존재, FATAL 없음 |
| R2 | Gauge shell | UI dump | `MyT`, (`기록` 또는 `히스토리`), `음성`, (`더보기` 또는 `설정`) |
| R2b | More hub | 더보기 탭 | `설정`, `API 사용량` (또는 설정 진입 가능) |
| R2c | Dual cluster | UI dump | `지시등` + (`VEHICLE MAP`\|`DRIVE MAP`\|`CHARGING MAP`\|`SPEED CAM`\|`NAVIGATION`\|`G-METER`\|`TIRES`) + (`탭: 지도`) |
| R2d | Compact status | UI dump | SOC(`%`) + (`BT ON`\|`BT OFF`) |
| R-GPS | BT GPS gate | runtime/logcat | BT 미연결 시 Device GPS 미사용 흔적 또는 `BT OFF` UI |
| R13 | Portrait dual | user_rotation=0 → dump | 게이지 셸 라벨 유지, FATAL 없음 |
| R14 | Landscape dual | user_rotation=1 → dump | 게이지 셸 라벨 유지, FATAL 없음 |
| R15 | Secondary toggle | 지도 헤더/`탭: 지도` 탭 | `G-METER` 또는 `TIRES`/`타이어` 전환, FATAL 없음 |
| R3 | No crash on boot | logcat | `FATAL EXCEPTION` + `com.myt` 없음 |
| R4 | History open | `기록`(또는 `히스토리`) 탭 | UI에 `히스토리`+`닫기`, FATAL 없음 |
| R5 | History close | `닫기` 탭 | 다시 `더보기`/`음성`/`기록` 보임 |
| R6 | Settings open | `더보기`→`설정` | UI에 `설정`, FATAL 없음 |
| R7 | Settings back | `뒤로` 탭 | 게이지 셸 복귀 |
| R8 | Voice open | `음성` 탭 | UI에 `음성`, FATAL 없음 |
| R9 | Voice close | `닫기` 탭 | 게이지 셸 복귀 |
| R10 | Runtime log | `run-as` cat | `files/debug_logs/myt-runtime.log` 존재(또는 생성 가능) |
| R11 | SQLite history | History 재오픈 | `polyline_encoded` SQLiteException 없음 |
| R12 | Soft fleet | runtime log | `Empty vehicle_data`가 있어도 프로세스 생존 |

## 2026-08-27 보강 (듀얼 게이지)

최근 반영분에 맞춘 assert:

- 숏컷 라벨: **음성 / 기록 / 더보기** (구 `히스토리` 버튼명 대체)
- 주 게이지: **지시등** 슬롯
- 보조 게이지: **지도 헤더** + 탭 토글 힌트
- 세로/가로 `user_rotation` 스모크

## 산출물

매 실행마다:

```
build/device-debug/regression/<stamp>/
  RESULT.md          # 케이스별 PASS/FAIL
  summary.json       # 기계 판독용
  ui-*.xml           # 단계별 UI dump
  logcat.txt
  screen-*.png
  myt-runtime.log    # 가능 시
```

그리고 `docs/08-implementation/device-regression-history.md` 상단에 **실행 요약 블록**이 자동 append 됩니다.

## 유지보수

- 새 화면/버튼이 생기면 이 표에 케이스 추가 + 스크립트 `CASES`에 assert 추가
- 실패가 재현되면 `/crash-log-triage`로 로그·이슈 연동
- 히스토리에 원인·수정 커밋/이슈 링크를 남긴다
