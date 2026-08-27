# BLE / Phone Key로 차량 정보를 얻을 수 있는가?

> 작성: 2026-08-27  
> 관련: [tesla-api-bluetooth-findings.md](../01-research/tesla-api-bluetooth-findings.md), [device-telemetry-hybrid.md](./device-telemetry-hybrid.md)

## 1. 결론 (한 줄)

**연속 계기판 데이터(속도·SOC·타이어·전력)를 BLE만으로 Fleet API처럼 대체하는 것은 현실 불가에 가깝다.**  
BLE는 **근접(Presence)·명령(VCSEC)** 에 강점이 있고, MyT의 속도·위치는 **단말 GPS**, 차량 전용 필드는 **Fleet(저빈도)** 가 맞다.

## 2. BLE로 가능한 것

| 능력 | 방식 | MyT 적용 |
|------|------|----------|
| **차량 근접 감지** | Phone Key 광고명 `S########C`, VCSEC UUID, GATT connected | ✅ 2026-08-27 강화 (Presence) |
| **잠금/언락·트렁크·원격 시작 등 명령** | Tesla `vehicle-command` 프로토콜 + Virtual Key 등록 + BLE 세션 | Phase 2 (명령은 Fleet REST도 가능) |
| **세션/키체인 상태** | VCSEC protobuf 응답 | 연구용 · 상용 복잡도 높음 |
| **연속 텔레메트리 스트림** | 공식 “Phone Key → SOC/속도 스트림” API 없음 | ❌ 대체 불가 |

근거:

- [teslamotors/vehicle-command protocol](https://github.com/teslamotors/vehicle-command/blob/main/pkg/protocol/protocol.md): BLE는 REST와 동일한 **명령 라우팅**을 지원. VCSEC는 잠금 등, Infotainment 도메인은 나머지 명령.
- Phone Key는 시스템/공식 앱이 유지하는 **수동 인증·근접**용. 서드파티가 광고만 스캔해도 **게이지 필드가 흘러나오지 않음**.
- Fleet Manager 역할 키는 BLE 명령 인가에 제약이 있을 수 있음(프로토콜 문서 Roles).

## 3. Fleet API를 “대체”할 수 있는 항목 (MyT 하이브리드)

| 필드 | Fleet 필수? | 대체 |
|------|-------------|------|
| 속도 | 아니오 (주행 중) | **단말 GPS** (BT ON일 때만) |
| 위치·헤딩·단속 | 아니오 | **단말 GPS** |
| SOC / Range / Gear / 타이어 / Power / 충전 | **예** | Fleet 저빈도 + 로컬 캐시 |
| 잠금 명령 등 | 선택 | Phase 2: Fleet cmds 또는 BLE vehicle-command |

→ **API 호출을 줄이는 대체는 Device GPS(속도·위치)가 핵심**이며, 이미 TelemetryMerger에 반영됨.  
BLE Presence는 **쿼터 절감의 게이트**(GPS 켤지 여부)이지 데이터 소스 대체는 아님.

## 4. Presence 구현 메모 (이번 수정)

이전 문제: 차 안에서는 Phone Key가 연결돼도 앱은 `BT OFF`.

원인:

1. 스캔 이름이 `Tesla`/`Sentry`/`TI`만 매칭 → 실제 Phone Key `S`+hex+`C` 누락
2. `BluetoothAdapter.isEnabled == true`를 Connected로 취급 → 오탐/신뢰 붕괴
3. 연결 후 광고가 줄어들면 스캔만으로는 감지 실패

수정:

- `TeslaBlePresence` — Phone Key 정규식 + VCSEC UUID
- GATT `getConnectedDevices` / bonded `isConnected` 폴링
- ACL은 이름 필터 + 재평가
- adapter ON만으로는 Connected 아님

## 5. 향후 (Phase 2+)

1. Virtual Key 등록 UX 후 `vehicle-command` BLE로 **잠금/공조 명령** (Fleet command 쿼터 절감 가능)
2. Companion Device Manager로 Phone Key 페어링 UX
3. Infotainment 도메인 상태 폴링은 문서·펌웨어 제약 확인 후 실험 — **게이지 1차 소스로 올리지 않음**

## 6. 수락 기준

- [ ] 차 탑승·Phone Key 동작 시 MyT 하단/상태 `BT ON`
- [ ] 집/차량 밖(미연결)에서 `BT OFF`, Device GPS 미사용
- [ ] 속도는 BT ON + GPS 권한 시 GPS LIVE 표시
