# MyT iOS App (Phase 1 / 1.5)

## Prerequisites

1. JDK 17+ (`gradle.properties` already points to `~/.jdks/jdk-17.0.20+8`)
2. Xcode 16+ **and accept license**: `sudo xcodebuild -license`
3. Apple Development Team ID (Signing & Capabilities)

## Open project

```bash
open iosApp/iosApp.xcodeproj
```

Sources:

- `iosApp/iOSApp.swift` — `@main` entry
- `iosApp/ContentView.swift` — ComposeUIViewController host
- `iosApp/SpeechHelper.swift` — native SFSpeechRecognizer
- `iosApp/Info.plist` — BT / Mic / Location / background BT

## Build Compose framework

```bash
./scripts/build-ios-framework.sh
# or from Xcode: the “Compile Kotlin Framework” build phase runs
# ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

## Known limitation

JetBrains Navigation Compose 2.9.2 ships KLIB ABI for Kotlin 2.2+, while this repo stays on Kotlin 2.1 for Android stability.  
Until Kotlin is upgraded carefully, **iOS KLIB compile may fail** with ABI mismatch. Android builds are unaffected.

## Capabilities checklist

- [ ] Bluetooth LE
- [ ] Background Modes → bluetooth-central
- [ ] Microphone
- [ ] Speech Recognition
