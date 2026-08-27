# 데이터 흐름 · 상태머신

## 1. 전체 데이터 흐름

```mermaid
flowchart TB
  subgraph trigger [Trigger]
    Launch[App_Launch_OAuth]
    BT[BLE_Connected]
    User[User_Action]
    Timer[Polling_Timer]
  end

  subgraph ingest [Data_Ingestion]
    FleetPoll[Fleet_API_Poll]
    POILocal[POI_Local_Query]
    STT[Speech_to_Text]
  end

  subgraph process [Processing]
    StateMachine[App_State_Machine]
    SpeedEngine[SpeedCam_Engine]
    LayoutEngine[Adaptive_Layout]
    UnitConv[Unit_Converter]
  end

  subgraph store [Storage]
    TokenStore[Secure_Token]
    POIDB[(POI_SQLite)]
    SettingsDB[(Settings)]
    TripDB[(Trips_Phase1.5)]
  end

  subgraph render [Rendering]
    GaugeView[Gauge_Compose_UI]
    AlertView[Alert_Overlay]
    NavView[Nav_Dialog]
  end

  Launch --> FleetPoll
  BT --> StateMachine
  Timer --> FleetPoll
  User --> STT
  User --> StateMachine

  FleetPoll --> UnitConv
  POILocal --> SpeedEngine
  FleetPoll --> SpeedEngine
  STT --> NavView

  UnitConv --> StateMachine
  SpeedEngine --> AlertView
  StateMachine --> LayoutEngine
  LayoutEngine --> GaugeView
  StateMachine --> GaugeView

  FleetPoll --> TokenStore
  POILocal --> POIDB
  StateMachine --> SettingsDB
  StateMachine --> TripDB
```

## 2. 앱 상태머신

```mermaid
stateDiagram-v2
  [*] --> Uninitialized

  Uninitialized --> Onboarding: First_Launch
  Uninitialized --> Idle: Has_Token

  Onboarding --> AuthFlow: Start
  AuthFlow --> VinSetup: OAuth_OK
  VinSetup --> BtSetup: VIN_Whitelisted
  BtSetup --> Idle: BT_Permission_Granted

  Idle --> BtDetected: PhoneKey_Connected
  Idle --> Connecting: App_Open_with_OAuth
  BtDetected --> Launching: Auto_Launch

  Launching --> Connecting: UI_Ready
  Connecting --> Streaming: Fleet_Data_OK
  Connecting --> SleepWait: Car_Sleep

  SleepWait --> Streaming: Car_Wake
  SleepWait --> SleepWait: Poll_30s

  Streaming --> Alerting: SpeedCam_Trigger
  Alerting --> Streaming: Alert_Dismissed

  Streaming --> VoiceNav: Voice_Button
  VoiceNav --> Streaming: Nav_Sent_or_Cancel

  Streaming --> Charging: ChargeState_Active
  Charging --> Streaming: ChargeState_End

  Streaming --> Idle: User_Exit
  Launching --> Idle: Session_Cancelled
  Idle --> Streaming: App_Foreground_Fleet_Poll

  Idle --> BtDetected: PhoneKey_Reconnected

  state Streaming {
    [*] --> Driving
    Driving --> Parked: Gear_P
    Parked --> Driving: Gear_D
  }
```

## 3. Telemetry 폴링 흐름

```mermaid
sequenceDiagram
  participant Timer as Poll_Timer
  participant Repo as FleetRepository
  participant API as Tesla_Fleet_API
  participant Car as Model_3
  participant VM as GaugeViewModel
  participant UI as Compose_UI

  Timer->>Repo: tick(interval)
  Note over Timer,UI: BLE 여부와 무관하게 OAuth 세션이면 폴링
  Repo->>API: GET /vehicles/{vin}/vehicle_data
  API->>Car: Wake + Fetch
  Car-->>API: drive_state, charge_state, climate_state
  API-->>Repo: VehicleDataResponse
  Repo->>Repo: Map_to_GaugeState
  Repo->>VM: emit(GaugeState)
  VM->>UI: recompose(GaugeState)

  Note over Timer,UI: 주차 시 interval → 30s
  Note over Timer,UI: Sleep 시 → wake_up 후 60s + "대기 중" UI
  Note over Timer,UI: BLE 없음 → Fleet 원격 조회 유지, 상태 배지 FLEET
```

## 4. SpeedCam 엔진 흐름

```mermaid
sequenceDiagram
  participant GPS as Location_Update
  participant Engine as SpeedCamEngine
  participant DB as POI_Database
  participant Alert as AlertManager
  participant UI as SpeedCamOverlay
  participant Audio as AudioPlatform

  GPS->>Engine: lat_lng_heading_speed
  Engine->>DB: query_radius(500m)
  DB-->>Engine: candidates[]
  Engine->>Engine: filter_direction(candidates, heading)
  Engine->>Engine: calculate_TTI(distance, speed)

  alt No camera nearby
    Engine->>UI: hide
  else L1: 300-500m
    Engine->>UI: show_banner(yellow)
  else L2: 100-300m
    Engine->>UI: show_overlay(orange)
    Engine->>Audio: beep_once
  else L3: <100m + overspeed
    Engine->>UI: show_flash(red)
    Engine->>Audio: beep_three
  end
```

## 5. Voice Nav 흐름

```mermaid
sequenceDiagram
  actor User as 운전자
  participant UI as VoiceNavDialog
  participant STT as SpeechPlatform
  participant UC as VoiceNavUseCase
  participant API as FleetRepository
  participant Car as Model_3

  User->>UI: Tap_Mic
  UI->>STT: startListening(ko-KR)
  User->>STT: "강남역으로 안내해줘"
  STT-->>UI: "강남역"
  UI->>User: Confirm_Destination("강남역")
  User->>UI: Tap_Send
  UI->>UC: sendDestination("강남역")
  UC->>API: POST navigation_request
  API->>Car: Set_Navigation_Destination
  Car-->>API: queued: true
  API-->>UC: Success
  UC-->>UI: NavSent
  UI->>User: "차량 내비에 전송됨 ✓"
```

## 6. BT 자동 실행 흐름

### Android

```mermaid
sequenceDiagram
  participant Car as Model_3_BLE
  participant BT as BluetoothReceiver
  participant Svc as PresenceService
  participant App as MainActivity

  Car->>BT: ACL_CONNECTED
  BT->>Svc: onDeviceConnected(mac)
  Svc->>Svc: match_PhoneKey_MAC
  Svc->>App: Intent(ACTION_LAUNCH_GAUGE)
  App->>App: startGaugeMode()
  Note over App: FLAG_KEEP_SCREEN_ON
  Note over App: Immersive_Fullscreen
```

### iOS

```mermaid
sequenceDiagram
  participant Car as Model_3_BLE
  participant CBC as CBCentralManager
  participant Svc as BTMonitorService
  participant Notif as UNNotification
  participant App as MyTApp

  Car->>CBC: didConnect
  CBC->>Svc: phoneKeyConnected
  Svc->>Notif: "MyT: 차량 연결됨"
  Notif->>App: User_Taps_Notification
  App->>App: startGaugeMode()
```

## 7. 적응형 레이아웃 데이터 흐름

```mermaid
flowchart LR
  WindowSize[WindowSizeClass] --> LayoutUC[AdaptiveLayoutUseCase]
  Orientation[Orientation] --> LayoutUC
  DeviceType[Phone_or_Tablet] --> LayoutUC

  LayoutUC --> LayoutConfig[LayoutConfig]
  LayoutConfig --> |Compact| SinglePane[SinglePaneLayout]
  LayoutConfig --> |Medium| TwoPane[TwoPaneLayout]
  LayoutConfig --> |Expanded| ThreePane[ThreePaneLayout]

  GaugeState[GaugeState] --> SinglePane
  GaugeState --> TwoPane
  GaugeState --> ThreePane
  MapState[MapState] --> TwoPane
  MapState --> ThreePane
```

## 8. 데이터 모델

```mermaid
classDiagram
  class GaugeState {
    +Float speedKmh
    +Gear gear
    +Float socPercent
    +Float rangeKm
    +Float insideTempC
    +Float outsideTempC
    +Float powerKw
    +Float longAccelG
    +Float latAccelG
    +TirePressures tires
    +NavInfo navigation
    +ChargeInfo charging
    +ConnectionStatus connection
    +Boolean locked
    +Float odometerKm
    +Boolean sentryMode
    +Boolean climateOn
    +Boolean bluetoothPresent
  }

  class NavInfo {
    +String destinationName
    +Float etaMinutes
    +Float distanceKm
    +String routePolyline
  }

  class TirePressures {
    +Float fl, fr, rl, rr
    +PressureUnit unit
  }

  class ChargeInfo {
    +ChargeState state
    +Float socPercent
    +Float chargeRateKw
    +Float timeToFullMinutes
    +String chargingState
    +Int chargeLimit
  }

  class SpeedCamAlert {
    +AlertLevel level
    +Float distanceM
    +Int speedLimitKmh
    +String cameraType
    +Float avgSpeedKmh
  }

  class LayoutConfig {
    +WindowSizeClass widthClass
    +WindowSizeClass heightClass
    +LayoutType layoutType
    +Boolean showMap
    +Boolean showDetail
  }

  GaugeState --> NavInfo
  GaugeState --> TirePressures
  GaugeState --> ChargeInfo
```

## 9. 이벤트 버스 (Domain Events)

| Event | Publisher | Subscriber | Action |
|---|---|---|---|
| `BtConnected` | BTPlatform | PresenceUC | Launch Gauge |
| `BtDisconnected` | BTPlatform | PresenceUC | 자동실행만 해제. Fleet 폴링은 유지 |
| `GaugeStateUpdated` | FleetRepo | GaugeUI, SpeedCamUC | Recompose |
| `SpeedCamAlert` | SpeedCamUC | AlertUI, AudioPlatform | Show+Beep |
| `NavDestinationSent` | VoiceNavUC | GaugeUI | Update NavInfo |
| `TokenExpired` | FleetRepo | AuthUC | Re-login flow |
| `LayoutChanged` | LayoutUC | GaugeUI | Re-layout |
| `CarSleeping` | FleetRepo | GaugeUI | Show sleep UI |

## 10. 히스토리 · 로컬 캐시 (Phase 1.5+)

```mermaid
flowchart LR
  Poll[TelemetryUseCase] --> TTL{snapshot age < interval?}
  TTL -->|yes| Cache[(vehicle_snapshot)]
  TTL -->|no| Fleet[Fleet API]
  Fleet --> Cache
  Fleet --> TripRec[LocalTripRecorder]
  Fleet --> ChargeRec[LocalChargeRecorder]
  Quota[FleetQuotaUseCase] --> FleetLog[(fleet_api_event)]
  Cache --> GaugeUI[Gauge UI]
  TripRec --> TripDB[(trip_record)]
  ChargeRec --> ChargeDB[(charge_session)]
  HistoryUI[HistoryScreen] --> TripDB
  HistoryUI --> ChargeDB
  HistoryUI --> FleetLog
```

- **캐시 우선**: `vehicle_snapshot`이 폴링 간격보다 신선하면 Fleet API 호출 생략
- **오프라인/쿼터**: fetch 실패 시 DB 스냅샷으로 게이지 표시
- 상세 설계: [history-and-voice-design.md](../05-design/history-and-voice-design.md)

## 11. 디버그 로그 · Gmail 내보내기

- **DebugLogger**: 링 버퍼(최대 2,000건), DEBUG/INFO/WARN/ERROR, 토큰·VIN·secret 자동 마스킹
- **수집 위치**: App/Lifecycle, Telemetry, Fleet API, Quota, Auth, Voice
- **UI**: 설정 → 「디버그 로그 보기 / Gmail 전송」→ `Route.DebugLogs`
- **내보내기**: UTF-8 `.txt` 리포트 + Gmail 우선 `ACTION_SEND` (미설치 시 공유 시트)
- **iOS**: `UIActivityViewController` 공유 (Mail 포함)
