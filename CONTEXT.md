# CONTEXT.md — live project state (agent-maintained, keep short)

## Current
- Phase: P0
- Next task: P0-T1

## Repo layout (update when structure changes)
```
fashion-shop/
  AGENTS.md
  docs/ (CONTEXT.md PLAN.md RULES.md SPEC.md)
  backend/            # Maven multi-module parent
    common/           # shared lib (ApiResponse, errors, security headers, base classes)
    gateway/ auth-service/ catalog-service/ inventory-service/ cart-service/
    promotion-service/ order-service/ payment-service/ notification-service/
  frontend/           # React + Vite + TS
  docker-compose.yml
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

## Implemented tables (append: `table — service — migration file`)

## Decisions (append: `date — decision — reason`)

## Known issues
