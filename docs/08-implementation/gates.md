# MyT Integration Gates

> Gate 통과 시각(KST)과 검증 결과를 기록합니다.

## Gate 정의

| Gate | Phase | 조건 | 상태 |
|---|---|---|---|
| **G0** | 1 | Android+iOS 빌드 성공 | ⏳ blocked (JDK 미설치) |
| **G1** | 1 | OAuth + vehicle_data + POI 15K | pending |
| **G2** | 1 | Gauge mock → full UI + Nav | 🔄 partial (mock UI) |
| **G3** | 1 | BT→Gauge + SpeedCam + Voice | pending |
| **G4** | 1 | 양 플랫폼 실기 | pending |
| **G5** | 1 | AC 42/43 | pending |
| **G6** | 1 | APK/IPA + 2주 무중단 | pending |
| **G7** | 1.5 | 100건+ trip/charge 기록 | pending |
| **G8** | 1.5 | Telemetry 지연 50% 감소 | pending |
| **G9** | 1.5 | polyline 지도 표시 | pending |
| **G10** | 1.5 | v0.2.0 배포 | pending |
| **G11** | 2 | Auth proxy + Telemetry live | pending |
| **G12** | 2 | lock/climate/trunk 동작 | pending |
| **G13** | 2 | 5+ automation rules | pending |
| **G14** | 2 | Watch + Widget | pending |
| **G15** | 2 | Billing sandbox | pending |
| **G16** | 2 | Store 승인 + 100 유저 | pending |
| **G17** | 3 | HA MQTT entity | pending |
| **G18** | 3 | Web control live | pending |
| **G19** | 3 | Battery degradation graph | pending |
| **G20** | 3 | Import/export | pending |

## 통과 기록

| Gate | Passed At (KST) | Verified By | Notes |
|---|---|---|---|
| G2-partial | 2026-08-20 11:44 | Cursor | GaugeScreen + mock GaugeState 렌더 스캐폴드 |

```mermaid
flowchart LR
  G0[G0_Build] --> G1[G1_Data]
  G1 --> G2[G2_UI]
  G2 --> G3[G3_Features]
  G3 --> G4[G4_Device]
  G4 --> G5[G5_AC]
  G5 --> G6[G6_Release]
  G6 --> G7[G7_Phase1.5]
  G7 --> G10[G10_v0.2]
  G10 --> G16[G16_Store]
  G16 --> G20[G20_Phase3]
```
