---
description: 연결된 Android 단말에 MyT APK를 빌드·설치·실행하고 logcat/dumpsys로 재현 확인 → 수정 → 재설치 루프
---

# device-apk-debug-loop

연결된 USB 단말에서 MyT APK를 **빌드 → 설치 → 실행 → 증상 수집 → 코드 수정 → 재검증**까지 에이전트가 직접 수행한다.

## When to use

- APK 설치 후 런타임 오류(OAuth, 크래시, 빈 화면 등)를 단말이 연결된 상태에서 재현·수정할 때
- 사용자가 “단말에서 직접 확인해서 고쳐줘”라고 할 때

## Preconditions

1. `adb devices`에 `device` 상태 단말이 1대 이상
2. JDK 17 (`gradle.properties`의 `org.gradle.java.home` 또는 `~/.jdks/...`)
3. 단말이 잠금 화면이면 **잠금 해제**를 사용자에게 요청한 뒤 UI 덤프/스크린샷을 재시도
4. 비밀값(`tesla.local.properties`의 secret)을 채팅에 **출력하지 않는다**

## Agent procedure (follow in order)

### 1) Discover device

```bash
adb devices -l
```

- 단말이 없으면 중단하고 연결을 요청한다.
- `SERIAL`을 고정한다 (`-s $SERIAL`).

### 2) Build debug APK

```bash
export PATH="/Users/wayforyou/.jdks/jdk-17.0.20+8/Contents/Home/bin:$PATH"
./gradlew :androidApp:assembleDebug
```

APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`  
패키지: `com.myt`

### 3) Install + cold start

```bash
adb -s "$SERIAL" install -r -t androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb -s "$SERIAL" shell am force-stop com.myt
adb -s "$SERIAL" logcat -c
adb -s "$SERIAL" shell monkey -p com.myt -c android.intent.category.LAUNCHER 1
```

또는 헬퍼:

```bash
./scripts/device_apk_debug_loop.sh install
```

### 4) Collect evidence (parallel)

- `adb logcat -d` — 태그 `Auth`, `Fleet`, `AndroidRuntime`, `chromium`, `Console`
- `dumpsys activity activities` — Custom Tab / VIEW intent의 **실제 authorize URL** (`redirect_uri` 포함 여부)
- `uiautomator dump` + 스크린샷 (`screencap`) — 잠금 해제된 경우만 UI 텍스트 신뢰
- 앱 내부 설정 오버레이: `run-as com.myt cat files/tesla.local.properties` (secret 마스킹)

### 5) Diagnose → minimal fix

- 원인 가설을 근거(로그 URL/에러 문구)와 함께 짧게 정리
- **요청 범위만** 최소 수정
- OAuth 관련이면 Tesla 문서 기준 파라미터는 `redirect_uri`(표준). HTTPS Allow-list와 앱 설정 값이 **문자 단위로 일치**해야 한다.

### 6) Rebuild → reinstall → re-verify

같은 수집 절차로 증상이 사라졌는지 확인.  
잠금/수동 로그인(Tesla 계정·MFA)이 필요하면 그 지점만 사용자에게 요청하고, 가능하면 콜백 deep link까지 adb로 검증한다.

### 7) Report to user (Korean, 오빠 호칭)

- 재현된 증상 / 원인 / 수정 파일 / 재설치 결과 / 사용자 확인이 남은 항목

## Helper script

`scripts/device_apk_debug_loop.sh` 가 빌드·설치·로그 수집·authorize URL 추출을 수행한다.

```bash
./scripts/device_apk_debug_loop.sh all          # build+install+launch+collect
./scripts/device_apk_debug_loop.sh collect      # logcat/dumpsys만
./scripts/device_apk_debug_loop.sh sync-config  # 로컬 tesla.local.properties → 기기 files/
```

## Do not

- secret/token을 커밋하거나 채팅에 그대로 붙이지 않는다
- 단말 잠금 패턴/비밀번호를 추측하거나 우회하지 않는다
- iOS/Xcode 라이선스 미동의 상태에서 iOS 빌드 실패를 “앱 버그”로 오진하지 않는다
