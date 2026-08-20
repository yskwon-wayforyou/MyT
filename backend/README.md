# MyT Backend (Phase 2)

Ktor server skeleton for Phase 2 commercial release.

## Modules (planned)

| Module | Path | Description |
|---|---|---|
| M25 | `Application.kt` | Server bootstrap |
| M26 | `routes/AuthRoutes.kt` | OAuth proxy |
| M27 | `telemetry/` | Fleet Telemetry relay (TODO) |
| M28 | `routes/ApiRoutes.kt` | REST API |

## Run (requires JDK 17+)

```bash
cd backend
../gradlew :backend:run
# or when included in root settings:
./gradlew :backend:run
```

Health: `GET http://localhost:8080/health`

## Phase 2 dependencies

- PostgreSQL
- Redis
- Docker Compose (TODO)

See [phase-specs/phase-2.md](../docs/08-implementation/phase-specs/phase-2.md)
