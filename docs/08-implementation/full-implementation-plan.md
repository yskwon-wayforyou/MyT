# MyT 전체 구현 계획 (Phase 0 ~ Phase 3)

> 이 문서는 Cursor Plan의 내용을 프로젝트 저장소에 반영한 버전입니다.  
> 실시간 진행: [progress-tracker.md](progress-tracker.md) · Gate: [gates.md](gates.md)

## 로드맵

| Phase | 목표 | 모듈 | 배포 |
|---|---|---|---|
| 0 | 문서·설계 | - | ✅ |
| 1 | Gauge MVP | M0~M17 | v0.1.0 APK/IPA |
| 1.5 | Trip/Telemetry/Map | M18~M24 | v0.2.0 |
| 2 | 상용 배포 | M25~M38 | v1.0.0 Store |
| 3 | 고도화 | M39~M45 | v1.x |

**총 46 모듈 · ~85 기능 · ~248 Tasks**

자세한 Phase 1 모듈 정의는 [phase-plan.md](../07-roadmap/phase-plan.md) 및 Plan `MyT Full Implementation` 참조.

## Phase 1 Stage (S1~S8)

| Stage | 모듈 | 상태 |
|---|---|---|
| S1 Scaffold | M0~M2 | ✅ scaffold |
| S2 Data | M3~M6 | 🔄 stub |
| S3 UI | M8,M9,M12,M13 | 🔄 partial |
| S4 Features | M7,M10,M11 | 🔄 stub |
| S5 Platform | M14,M15 | 🔄 partial |
| S6 Integration | M16 | pending |
| S7 Deploy | M17 | partial |
| S8 Stability | 2주 실차 | pending |

## 다음 작업

1. JDK 17 설치 → G0 통과
2. Tesla Developer → OAuth (M3)
3. POI import (M5)
4. Fleet API real integration (M4)
5. 실차 AC 테스트 (M16)
