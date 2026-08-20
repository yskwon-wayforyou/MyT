# MyT iOS App (Phase 1)

Compose Multiplatform iOS wrapper. Generate/update with:

```bash
# From project root (JDK required)
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

Then open `iosApp/iosApp.xcodeproj` in Xcode 16+.

## Setup (first time)

1. Install JDK 17+ and Xcode 16+
2. Run Gradle framework embed (above)
3. Open Xcode project
4. Set Team + Bundle ID (`com.myt`)
5. Enable capabilities: Bluetooth LE, Background Modes (bluetooth-central), Microphone

## Phase 1 targets

- iOS 16.0+
- iPhone + iPad (Universal)

See [install-guide.md](../docs/08-implementation/install-guide.md)
