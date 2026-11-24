# RULES.md — coding rules (obey strictly)

## R1 Stack & versions
- Java 21, Spring Boot 3.3.x, Maven, Spring Data JPA (Hibernate), Flyway, MySQL 8, Lombok, MapStruct, springdoc-openapi, JUnit 5, Mockito, Testcontainers (MySQL).
- Inter-service HTTP: Spring `RestClient` wrapped in a client class (no Feign needed). Timeouts: connect 2s, read 5s.
- Frontend: React 18, Vite, TypeScript (strict), React Router, TanStack Query, Axios, Tailwind CSS, React Hook Form + Zod.

## R2 Package layout (per service, base `com.shop.<service>`)
```
controller/   REST only; no logic; returns ApiResponse<T>
dto/          request/response records (Java records), validated with jakarta.validation
service/      interfaces + impl in service/impl
domain/       entity/, repository/, enums
mapper/       MapStruct interfaces
client/       outbound HTTP clients to other services (Adapter)
config/       Spring config, security, OpenAPI
exception/    service-specific exceptions
```
- Dependency direction: controller → service → repository/client. Never controller → repository. Never entity in API.

## R3 Layer rules
- Entities never leave the service layer. Map with MapStruct to DTO records.
- Constructor injection only (`@RequiredArgsConstructor`). No field `@Autowired`.
- `@Transactional` on service impl methods (readOnly=true for queries). Never on controllers.
- No business logic in controllers, entities (except state transitions), or mappers.
- Method ≤ 40 lines, class ≤ 300 lines. Split otherwise.
- No magic strings/numbers: use enums/constants.
- No `System.out`; use SLF4J `@Slf4j`. Never log passwords, tokens, card data.

## R4 API conventions
- Base path `/api/v1/<resource>` (plural, kebab-case). Internal-only: `/internal/v1/...`.
- Envelope for all responses (from `common`):
  `{ "success": bool, "data": T|null, "error": {"code": "STRING", "message": "STRING", "details": []}|null }`
- Paging: query `page` (0-based), `size` (default 20, max 100), `sort=field,dir`. Response data: `{items, page, size, totalItems, totalPages}`.
- HTTP codes: 200, 201 (create), 204 (delete), 400 (validation), 401, 403, 404, 409 (conflict/state), 422 (business rule), 500.
- Time: UTC, ISO-8601 (`Instant`). Money: `BigDecimal`, VND, DB `DECIMAL(15,0)`. IDs: `BIGINT` auto-increment, exposed as numbers. Order public id: `orderNo` string.
- Every error → `GlobalExceptionHandler` (`@RestControllerAdvice`) in `common`. Throw `BusinessException(ErrorCode)`, `NotFoundException`, `ConflictException`. Never return stack traces.

## R5 Database
- One MySQL server, one schema per service (`<service>_db`). NO cross-schema joins, NO cross-service foreign keys; store foreign ids as plain BIGINT.
- Schema changes ONLY via Flyway: `src/main/resources/db/migration/V<n>__<desc>.sql`. Never edit an applied migration. `spring.jpa.hibernate.ddl-auto=validate`.
- Every table: `id`, `created_at`, `updated_at` (auditing via `BaseEntity` in `common`). Soft delete (`deleted` BOOLEAN) only where SPEC says.
- Index every FK-like column and every filter/sort column. Unique constraints in DB, not only in code.
- Concurrency: optimistic locking (`@Version`) on `stock`; pessimistic `SELECT ... FOR UPDATE` only inside inventory reservation.

## R6 Security
- Auth-service issues JWT (HS256, secret from env `JWT_SECRET`, access 15 min, refresh 7 days, refresh stored hashed in DB).
- Gateway validates JWT, strips client-supplied `X-User-*` headers, then injects `X-User-Id`, `X-User-Roles`.
- Downstream services trust `X-User-Id`/`X-User-Roles` ONLY if `X-Internal-Token` equals env `INTERNAL_TOKEN` (set by gateway and by service clients). Use a servlet filter in `common`.
- Roles: `CUSTOMER`, `ADMIN`. Use `@PreAuthorize`-style checks via `common` annotation `@RequireRole`.
- Passwords: BCrypt strength 10. Validate all input (`@Valid`). Parameterized queries only (JPA). CORS configured at gateway only.
- Secrets/config from environment variables only; commit `.env.example`, never `.env`.

## R7 Design patterns (use where stated; do not over-engineer elsewhere)
| Pattern | Where | Rule |
|---|---|---|
| Layered architecture | all services | R2/R3 |
| DTO + Mapper | all services | records + MapStruct |
| Builder | entities, complex objects | Lombok `@Builder` |
| Strategy | shipping fee, discount calc, payment method, file storage | interface + `@Component` impls, selected by key |
| Factory | `PaymentStrategyFactory`, `DiscountStrategyFactory` | injects `List<Strategy>`, returns by enum key |
| State | order status transitions | `OrderStatus` enum holds allowed next states; `order.transitionTo()` throws 409 on illegal move |
| Specification | product search/filter, order admin filter | Spring Data `Specification<T>` composed dynamically |
| Adapter (ACL) | `client/` classes, VNPay gateway | map foreign DTOs to own internal models; never leak foreign DTOs into domain |
| Saga (orchestration) | checkout in order-service | explicit steps + compensations, see SPEC §6.1 |
| Transactional Outbox | order-service → notification-service | event row saved in same TX as order; scheduler delivers with retry |
| Chain of Responsibility | checkout validation, coupon validation | ordered `Validator` beans, each throws on failure |
| Template Method | `common` `BaseEntity`, abstract `BaseClient` (timeouts, error mapping, internal token) | |
| Idempotency Key | `POST /orders`, payment callback | header `Idempotency-Key`; unique column; replay returns original result |
| Observer | in-service side effects | Spring `ApplicationEventPublisher` (in-process only) |

## R8 Resilience (no external libs required)
- Client calls: retry max 2 on 5xx/timeout for idempotent GETs only. No retry on POST unless endpoint is idempotent by key.
- Saga compensation must be safe to run twice (idempotent release endpoints).
- Scheduled jobs: `@Scheduled` + `@SchedulerLock`-free design (single instance assumed); guard with status columns.

## R9 Testing (required per task)
- Unit tests (Mockito) for every service-impl public method with logic.
- `@DataJpaTest` + Testcontainers for custom queries/specifications.
- `@WebMvcTest` for controllers: validation + status codes.
- One integration test per saga/critical flow (order-service).
- Frontend: Vitest + React Testing Library for forms and cart logic only.
- Task is NOT done until `mvn -pl <module> -am test` (or `npm run test && npm run build`) passes.

## R10 Naming & style
- Classes: `ProductService`, `ProductServiceImpl`, `ProductController`, `CreateProductRequest`, `ProductResponse`, `ProductMapper`.
- Tables: snake_case plural. Columns: snake_case. JSON: camelCase.
- Error codes: `UPPER_SNAKE` prefixed by domain, e.g. `PRODUCT_NOT_FOUND`, `STOCK_INSUFFICIENT`, `ORDER_INVALID_STATE`.
- Commits: Conventional Commits. One task = one commit.
- Each service has `README.md` ≤ 20 lines: run command, env vars, port. No other prose docs.

## R11 Docker / run
- Each service has a `Dockerfile` (multi-stage, JRE 21 slim). Root `docker-compose.yml` runs MySQL, MailHog, all services, frontend.
- Health: `/actuator/health` on every service. Compose uses `depends_on` with healthcheck for MySQL.

## R12 Frontend rules
- Folder: `src/{api,components,features/<feature>,pages,routes,hooks,types,utils}`.
- All HTTP via a single Axios instance (`api/http.ts`) with JWT interceptor + refresh-on-401 once.
- Server state with TanStack Query; local UI state with `useState`; cart for guests in `localStorage`.
- Forms: React Hook Form + Zod. No `any`. Components ≤ 200 lines.
- Route guards: `RequireAuth`, `RequireAdmin`. Admin routes under `/admin`.
- Money format: `Intl.NumberFormat('vi-VN', {style:'currency', currency:'VND'})`.
