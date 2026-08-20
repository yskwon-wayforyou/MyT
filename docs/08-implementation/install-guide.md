# MyT 설치 · 빌드 가이드

Phase 1 KMP 스캐폴드 기준으로 Android / iOS 개발 환경을 구성합니다.

## 사전 요구사항

| 도구 | 버전 | 용도 |
|---|---|---|
| JDK | 17+ (Temurin) | Kotlin / Gradle |
| Android Studio | Ladybug+ | Android 빌드 |
| Xcode | 16+ | iOS 빌드 (macOS) |
| Gradle | 8.11+ | Wrapper 포함 |

### JDK (macOS, Homebrew 없을 때)

Temurin 17은 아래 경로에 설치되어 있습니다:

```bash
export JAVA_HOME="$HOME/.jdks/jdk-17.0.20+8/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

쉘 기본값으로 쓰려면 `~/.zshrc`에 `JAVA_HOME` export를 추가하세요.

### Android SDK

`local.properties` (gitignored):

```properties
sdk.dir=/Users/wayforyou/Library/Android/sdk
```

예시: `local.properties.example` 참고.

## 0. Tesla Developer (Fleet API)

Phase 1 OAuth·차량 데이터 연동 전에 [tesla-developer-setup.md](tesla-developer-setup.md) 를 따라 등록하세요.

```bash
cp tesla.local.properties.example tesla.local.properties
# developer.tesla.com Client ID/Secret 입력
```

## 1. Gradle Wrapper

프로젝트 루트에서 wrapper가 없으면 생성합니다.

```bash
cd /Users/wayforyou/Projects/MyT
gradle wrapper --gradle-version 8.11.1
chmod +x gradlew scripts/build-all.sh
```

## 2. Android 빌드

```bash
./gradlew :androidApp:assembleDebug
```

APK 출력: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`

실기기 설치:

```bash
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

## 3. iOS 빌드 (macOS)

1. Xcode에서 `iosApp` 타겟을 추가하거나 KMP 템플릿의 `MainViewController()`를 SwiftUI `ContentView`에 연결합니다.
2. Framework 빌드:

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

## 4. 테스트

```bash
./gradlew :composeApp:cleanTest :composeApp:allTests
```

포함 테스트:

- `UnitConverterTest` — 단위 변환
- `SpeedCamEngineTest` — 과속단속 AlertLevel 로직

## 5. 모듈 구조

- `composeApp` — KMP 공유 코드 (UI, domain, data)
- `androidApp` — Android APK entry (`applicationId: com.myt`)

## 6. 다음 단계 (TODO)

- Tesla Fleet OAuth + `KtorFleetRepository` 실 API 연동
- Kable BLE Phone Key 모니터링
- SQLDelight POI DB import
- iOS Xcode 프로젝트(`iosApp`) 추가

## 7. 전체 빌드 스크립트

```bash
./scripts/build-all.sh
```
