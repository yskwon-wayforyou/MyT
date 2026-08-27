---
description: 실단말 정형 리그레션 실행 후 히스토리 문서 갱신·실패 시 수정
---

# device-regression

MyT 실단말 리그레션 스위트를 실행하고, 결과를 히스토리에 남긴 뒤 실패 항목을 고친다.

## Spec / history

- 스위트: `docs/08-implementation/device-regression-suite.md`
- 히스토리: `docs/08-implementation/device-regression-history.md`
- 실행기: `scripts/device_regression_test.sh`
- 빌드+설치+리그레션: `scripts/build-install-regress.sh`

## Agent procedure

1. 단말 unlock 확인 (`adb devices` + 잠금화면 아님)
2. 실행:

```bash
export DEVELOPER_DIR=/Library/Developer/CommandLineTools
./scripts/build-install-regress.sh
# 또는 설치만 된 상태면
./scripts/device_regression_test.sh
```

3. `build/device-debug/regression/<stamp>/RESULT.md` 확인
4. FAIL이면 `/crash-log-triage` + 최소 수정 + 재실행
5. 히스토리 문서에 자동 append된 블록을 검토하고, 수동 QA 메모가 있으면 보완

## Always on build/install

- `./scripts/build-android-apk.sh` — 단말 연결 시 자동 리그레션
- `./scripts/device_apk_debug_loop.sh all` — collect 후 리그레션
- 생략: `SKIP_DEVICE_REGRESSION=1`
