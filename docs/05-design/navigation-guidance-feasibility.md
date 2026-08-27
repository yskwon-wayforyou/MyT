# 내비게이션 · 차선/회전 안내 — 데이터 출처 검토

> 2026-08-27 · 듀얼 게이지 보조창 설계와 연계

## 결론

Tesla **Fleet API `vehicle_data` / drive_state** 로는:

- ✅ 목적지, 남은 거리, ETA(분), 헤딩
- ❌ **다음 회전(maneuver), 차선 가이드, 현재 도로명(내비 엔진)**

BLE Phone Key / VCSEC 도 동일하게 **턴바이턴을 스트리밍하지 않음**.

차선·다음 회전을 보조 게이지에 넣으려면:

1. **외부 내비 SDK** (Google Directions / Kakao / HERE) + Device GPS, 또는  
2. 차량 화면 의존 (미러링 없음), 또는  
3. Fleet Telemetry에 추가 필드가 공식 공개되는지 주기 확인 (현시점 미확인).

MyT 현재: 보조 게이지에 **차량 중심 OSM 지도**(반경 500m~1km) + Fleet 내비 요약 + 단속 마커 + “차선/회전은 Fleet에 없음” 고지.

## 방향지시등

Fleet `vehicle_data` / BLE Phone Key **미제공**. UI 슬롯(`turnSignalLeft/Right`, `hazardLightsOn`)은 예약되어 있으며, 값이 null이면 `지시등 —`로 표시한다.
