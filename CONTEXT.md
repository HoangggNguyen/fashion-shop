# CONTEXT.md — live project state (agent-maintained, keep short)

## Current
- Phase: P0
- Next task: P0-T3

## Repo layout (update when structure changes)
```
qlvatkpm/
  AGENTS.md CONTEXT.md PLAN.md RULES.md SPEC.md
  scripts/            # local MySQL init SQL & dev run/smoke scripts
  backend/            # Maven multi-module parent (Java 19, Spring Boot 3.3.x)
    common/           # shared lib (ApiResponse, errors, security headers, base classes)
    gateway/ auth-service/ catalog-service/ inventory-service/ cart-service/
    promotion-service/ order-service/ payment-service/ notification-service/
  frontend/           # React + Vite + TS
```

## Services (update status: todo | wip | done)
| Service | Port | DB schema | Status |
|---|---|---|---|
| gateway | 8080 | - | todo |
| auth-service | 8081 | auth_db | todo |
| catalog-service | 8082 | catalog_db | todo |
| inventory-service | 8083 | inventory_db | todo |
| cart-service | 8084 | cart_db | todo |
| promotion-service | 8085 | promotion_db | todo |
| order-service | 8086 | order_db | todo |
| payment-service | 8087 | payment_db | todo |
| notification-service | 8088 | notification_db | todo |
| frontend | 5173 | - | todo |

## Implemented endpoints (append: `METHOD path — service`)
- `GET /actuator/health` — all 9 services (skeletons)
- Files (P0-T1): `backend/pom.xml`, `backend/common/pom.xml`, 9 service modules (`pom.xml`, `*Application.java`, `application.yml`, `*ApplicationTests.java`), `.gitignore`
- Files (P0-T2): `common` (`ApiResponse`, `ApiError`, `PageResponse`, `BaseEntity`, `ErrorCode`, exceptions, `GlobalExceptionHandler`, `InternalAuthFilter`, `UserContext`, `@RequireRole`, `BaseClient`, `CommonAutoConfiguration`) + 5 unit test classes

## Implemented tables (append: `table — service — migration file`)

## Decisions (append: `date — decision — reason`)
- 2026-11-24 — Keep markdown docs (`AGENTS.md`, `CONTEXT.md`, `PLAN.md`, `RULES.md`, `SPEC.md`) at repo root instead of `docs/` — user preference.
- 2026-11-24 — Use Java 19 + Spring Boot 3.3.x (and modern libs like JHipster helpers/springdoc/MapStruct where helpful) — user preference.
- 2026-11-24 — Skip Docker and Testcontainers; run MySQL 8 / MailHog (or local SMTP) directly on host and use H2 (MySQL mode) / local MySQL for tests — user preference.

## Known issues
