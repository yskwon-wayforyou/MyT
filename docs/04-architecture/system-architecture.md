# 시스템 아키텍처

## 1. C4 Level 1 — 시스템 컨텍스트

```mermaid
flowchart LR
  Driver[운전자]
  MyT[MyT App]
  Tesla[Tesla Fleet API]
  OpenData[공공데이터포털]
  STT[Platform STT]

  Driver -->|"Touch Voice BLE"| MyT
  MyT -->|"vehicle_data navigation_request"| Tesla
  MyT -->|"POI download"| OpenData
  MyT -->|"음성 인식"| STT
```

## 2. C4 Level 2 — 컨테이너

```mermaid
flowchart TB
  Driver[운전자]
  subgraph MyTBoundary [MyT]
    App[Compose Multiplatform App]
    LocalDB[(SQLDelight Local DB)]
    Platform[Platform Services BT STT Audio Keystore]
  end
  FleetAPI[Tesla Fleet API]
  OpenData[data.go.kr]
  Telemetry[Fleet Telemetry Server]

  Driver --> App
  App --> Platform
  App --> LocalDB
  App -->|"OAuth REST"| FleetAPI
  App -->|"POI sync"| OpenData
  App -->|"WSS stream"| Telemetry
```

## 3. C4 Level 3 — 컴포넌트 (KMP shared)

```mermaid
flowchart TB
  subgraph presentation [Presentation_Layer]
    GaugeUI[GaugeScreen]
    NavUI[NavOverlay]
    SpeedUI[SpeedCamOverlay]
    VoiceUI[VoiceNavDialog]
    SettingsUI[SettingsScreen]
    OnboardUI[OnboardingFlow]
  end

  subgraph domain [Domain_Layer]
    PresenceUC[PresenceUseCase]
    TelemetryUC[TelemetryUseCase]
    SpeedCamUC[SpeedCamUseCase]
    VoiceNavUC[VoiceNavUseCase]
    AuthUC[AuthUseCase]
    LayoutUC[AdaptiveLayoutUseCase]
  end

  subgraph data [Data_Layer]
    FleetRepo[FleetApiRepository]
    POIRepo[SpeedCamPoiRepository]
    BTRepo[BluetoothRepository]
    TokenRepo[TokenRepository]
    SettingsRepo[SettingsRepository]
    TripRepo[TripRepository]
  end

  subgraph platform [Platform_Layer_expect_actual]
    BTPlatform[BluetoothPlatform]
    STTPlatform[SpeechPlatform]
    AudioPlatform[AudioAlertPlatform]
    HapticPlatform[HapticPlatform]
    StoragePlatform[SecureStoragePlatform]
  end

  GaugeUI --> TelemetryUC
  GaugeUI --> LayoutUC
  NavUI --> TelemetryUC
  SpeedUI --> SpeedCamUC
  VoiceUI --> VoiceNavUC
  OnboardUI --> AuthUC

  TelemetryUC --> FleetRepo
  SpeedCamUC --> POIRepo
  SpeedCamUC --> FleetRepo
  VoiceNavUC --> FleetRepo
  VoiceNavUC --> STTPlatform
  AuthUC --> TokenRepo
  AuthUC --> FleetRepo
  PresenceUC --> BTRepo
  PresenceUC --> BTPlatform

  SpeedCamUC --> AudioPlatform
  SpeedCamUC --> HapticPlatform
  TokenRepo --> StoragePlatform
  POIRepo --> localdb[(SQLDelight)]
  TripRepo --> localdb
  SettingsRepo --> localdb
```

## 4. 모듈 의존성

```mermaid
flowchart LR
  subgraph apps [App_Entry]
    androidApp[androidApp]
    iosApp[iosApp]
  end
  subgraph kmp [composeApp_KMP]
    commonMain[commonMain]
    androidMain[androidMain]
    iosMain[iosMain]
  end
  subgraph libs [Libraries]
    fleetSdk[tesla_fleet_sdk]
    kable[Kable_BLE]
    sqldelight[SQLDelight]
    ktor[Ktor_Client]
    koin[Koin_DI]
    adaptive[Material3_Adaptive]
  end

  androidApp --> commonMain
  iosApp --> commonMain
  commonMain --> fleetSdk
  commonMain --> sqldelight
  commonMain --> ktor
  commonMain --> koin
  commonMain --> adaptive
  androidMain --> kable
  iosMain --> kable
```

## 5. Phase별 아키텍처 진화

```mermaid
timeline
  title Architecture_Evolution
  section Phase_1
    Client_Only : App_direct_Fleet_API
    Local_POI : SQLDelight
    Polling : 2s_vehicle_data
  section Phase_1_5
    Telemetry : WebSocket_stream
    Trip_Store : Local_trip_history
    Map : Route_polyline
  section Phase_2
    Backend : Auth_proxy_Telemetry
    Multi_User : OAuth_per_user
    Billing : Store_integration
  section Phase_3
    HA_Integration : MQTT_Webhook
    Web_Dashboard : Browser_UI
```

## 6. 배포 아키텍처

### Phase 1 (Client-Only)

```mermaid
flowchart LR
  Phone[MyT_App] -->|HTTPS| FleetAPI[Tesla_Fleet_API]
  Phone -->|HTTPS| OpenData[data.go.kr]
  Phone -->|BLE| Car[Tesla_Model_3]
  Phone -->|Local| SQLite[(SQLDelight)]
```

### Phase 2 (Client + Backend)

```mermaid
flowchart LR
  Phone[MyT_App] -->|HTTPS| Backend[MyT_Backend]
  Backend -->|HTTPS| FleetAPI[Tesla_Fleet_API]
  Backend -->|WSS| Telemetry[Fleet_Telemetry]
  Telemetry --> Car[Tesla_Model_3]
  Phone -->|BLE| Car
  Backend --> DB[(PostgreSQL)]
  Backend --> Redis[(Redis_Cache)]
```

## 7. 보안 경계

```mermaid
flowchart TB
  subgraph trusted [Trusted_Zone]
    App[MyT_App]
    Keystore[Secure_Storage]
  end
  subgraph external [External]
    FleetAPI[Tesla_Fleet_API]
    OpenData[data.go.kr]
  end
  subgraph vehicle [Vehicle_Zone]
    Car[Tesla_Model_3]
    BLE[BLE_PhoneKey]
  end

  App -->|TLS_OAuth| FleetAPI
  App -->|TLS| OpenData
  App -->|BLE_proximity| BLE
  App -->|Encrypted| Keystore
  BLE --> Car
  FleetAPI -->|Signed_Commands| Car
```

## 8. 크로스플랫폼 아키텍처 원칙

| 원칙 | 설명 |
|---|---|
| **Shared First** | UI·로직·데이터 90%+ commonMain |
| **expect/actual Last** | BT, STT, Audio, Storage만 플랫폼 분리 |
| **Single Source of Truth** | Fleet API = 차량 상태 유일 소스 (BLE는 근접·자동실행 보조) |
| **Remote First** | BLE 없어도 OAuth 세션으로 vehicle_data를 폴링해 Gauge를 채운다 |
| **Local First (SpeedCam)** | POI DB 로컬, 네트워크 불필요 |
| **Adaptive by Default** | WindowSizeClass 기반, hardcode 금지 |
