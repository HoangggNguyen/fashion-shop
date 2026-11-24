# PLAN.md — phases & tasks (do in order; tick when done)

Legend: `[ ]` todo, `[x]` done. Task ID = `P<phase>-T<n>`. "SPEC §x" = section to read. Every task includes its tests (RULES R9) and a commit.

## Status
| Phase | Name | Status |
|---|---|---|
| P0 | Bootstrap & shared lib | todo |
| P1 | auth-service | todo |
| P2 | catalog-service | todo |
| P3 | inventory-service | todo |
| P4 | cart-service | todo |
| P5 | promotion-service | todo |
| P6 | order-service (saga) | todo |
| P7 | payment-service | todo |
| P8 | notification-service | todo |
| P9 | frontend foundation + auth | todo |
| P10 | frontend storefront | todo |
| P11 | frontend checkout + account | todo |
| P12 | frontend admin | todo |
| P13 | hardening & delivery | todo |

---
## P0 Bootstrap & shared lib
- [x] P0-T1 Create Maven multi-module parent `backend/` (Java 19, Spring Boot 3.3 BOM, modern libs e.g. JHipster framework/helpers where suitable, modules listed in CONTEXT.md). Empty service modules with `Application` class + `/actuator/health`.
- [ ] P0-T2 `common` module: `ApiResponse`, `PageResponse`, `ErrorCode`, exceptions, `GlobalExceptionHandler`, `BaseEntity` (auditing), `InternalAuthFilter` (R6), `@RequireRole`, `BaseClient` (RestClient, timeouts, internal token, error mapping). Unit tests.
- [ ] P0-T3 Local infra setup (no Docker): `scripts/init-mysql.sql` (creates all `*_db` schemas + user), `.env.example`, and local dev profile config (MySQL 8 localhost:3306, MailHog/local SMTP 1025).
- [ ] P0-T4 `gateway`: routes (SPEC §3), JWT validation filter, header injection/stripping, CORS, `X-Request-Id`, block `/internal/**`. Tests with WireMock-free `WebTestClient`.
- Exit: `mvn clean verify` passes; `gateway` starts and `/actuator/health` responds UP.

## P1 auth-service (SPEC §4.1)
- [ ] P1-T1 Flyway V1 tables + entities + repositories + admin seed from env.
- [ ] P1-T2 Register, login, JWT issue (access/refresh), refresh rotation, logout.
- [ ] P1-T3 Forgot/reset password (token hashed, 30 min) — notification call via `NotificationClient` (stub OK until P8, behind interface).
- [ ] P1-T4 Profile get/update, change password.
- [ ] P1-T5 Address CRUD + default rule + max 10.
- [ ] P1-T6 Admin user list/status + internal user endpoint.
- Exit: login via gateway returns JWT; protected route rejects missing token.

## P2 catalog-service (SPEC §4.2)
- [ ] P2-T1 Flyway V1 + entities (categories, brands, products, variants, images).
- [ ] P2-T2 Category tree + brand CRUD (admin) + public reads.
- [ ] P2-T3 Product CRUD (admin), slug generator, variants, soft delete. Inventory-stock-create call behind `InventoryClient` interface (stub until P3).
- [ ] P2-T4 `StorageService` Strategy + local impl + image upload/delete + static `/uploads/**`.
- [ ] P2-T5 Public product list with `Specification` filters/sort/paging; detail by slug; related products.
- [ ] P2-T6 Internal variant batch endpoint.
- [ ] P2-T7 Wishlist.
- [ ] P2-T8 Reviews (verified purchase via `OrderClient` interface; stub until P6) + rating aggregation + admin moderation.
- [ ] P2-T9 Dev seed data (SPEC §7).
- Exit: filter/sort queries covered by `@DataJpaTest` (H2 MySQL mode) tests.

## P3 inventory-service (SPEC §4.3)
- [ ] P3-T1 Flyway V1 + entities (`stocks` with `@Version`, reservations, movements).
- [ ] P3-T2 Admin stock list/adjust/movements + internal create-stock endpoint.
- [ ] P3-T3 Reservation reserve/commit/release/restock (idempotent, all-or-nothing, row locking). Concurrency test (parallel reserve never oversells).
- [ ] P3-T4 Expiry scheduler (1 min) + availability endpoint.
- [ ] P3-T5 Wire catalog `InventoryClient` to real endpoint (replace stub).
- Exit: concurrency test green.

## P4 cart-service (SPEC §4.4)
- [ ] P4-T1 Flyway V1 + entities + repositories.
- [ ] P4-T2 Cart add/update/remove/clear/merge with qty rules.
- [ ] P4-T3 Enriched `GET /cart` (catalog batch + inventory availability via clients), flags.
- [ ] P4-T4 Internal get/clear endpoints.

## P5 promotion-service (SPEC §4.5)
- [ ] P5-T1 Flyway V1 + entities; admin CRUD coupons; public active list.
- [ ] P5-T2 `DiscountStrategy` + factory; validator chain; `POST /coupons/validate`.
- [ ] P5-T3 Internal redeem/release (idempotent by orderRef) with limit checks under lock.

## P6 order-service (SPEC §4.6, §6.1, §6.2)
- [ ] P6-T1 Flyway V1 + entities; `OrderStatus` state machine with unit tests for every transition.
- [ ] P6-T2 Clients: cart, catalog, inventory, promotion, payment, auth (Adapter; internal models).
- [ ] P6-T3 `ShippingFeeStrategy` impls + `POST /orders/preview`.
- [ ] P6-T4 `CheckoutOrchestrator` saga + `SagaStep`s + idempotency; integration test incl. compensation paths (stock fail, coupon fail, payment fail).
- [ ] P6-T5 Customer endpoints: list, detail, cancel, return-request.
- [ ] P6-T6 Admin endpoints: list/filter, detail, status change (state machine), stats summary.
- [ ] P6-T7 Internal endpoints: purchased check, payment-result handler.
- [ ] P6-T8 Outbox table + publisher scheduler (target `NotificationClient`, stub until P8).
- [ ] P6-T9 Schedulers: auto-cancel unpaid (30 min), auto-complete delivered (7 days); on DELIVERED call catalog sold-count + payment cod-collected.
- [ ] P6-T10 Wire catalog `OrderClient` (reviews) to real endpoint.
- Exit: full checkout integration test green (COD and VNPAY-mock).

## P7 payment-service (SPEC §4.7)
- [ ] P7-T1 Flyway V1 + entities; `PaymentStrategy` + factory; COD + Mock strategies; internal create endpoint.
- [ ] P7-T2 `VnPayPaymentStrategy` (sign/verify HMAC-SHA512), return + IPN endpoints, idempotent callbacks, call order-service payment-result.
- [ ] P7-T3 COD-collected endpoint + `GET /payments/order/{orderNo}`.
- Exit: end-to-end order with MockPayment reaches CONFIRMED.

## P8 notification-service (SPEC §4.8)
- [ ] P8-T1 Flyway V1 + entity; internal endpoint; Thymeleaf templates for 5 types; JavaMailSender + MailHog.
- [ ] P8-T2 Async send + retry scheduler.
- [ ] P8-T3 Replace stubs: auth (WELCOME, RESET_PASSWORD), order outbox publisher → real client.
- Exit: register + place order produce mails visible in MailHog (8025).

## P9 frontend foundation + auth (SPEC §5, RULES R12)
- [ ] P9-T1 Vite React TS project, Tailwind, router, layouts (public, account, admin), `api/http.ts` with refresh interceptor, auth store, route guards.
- [ ] P9-T2 Login, register, forgot/reset pages (RHF + Zod).
- [ ] P9-T3 Guest cart in localStorage + merge on login.
- [ ] P9-T4 Shared UI kit: Button, Input, Modal, Pagination, Toast, Spinner, EmptyState, Price.

## P10 frontend storefront
- [ ] P10-T1 Home page.
- [ ] P10-T2 Product list: filters, sort, search, URL-synced, pagination.
- [ ] P10-T3 Product detail: gallery, variant picker, add to cart, wishlist toggle, reviews, related.
- [ ] P10-T4 Cart page (guest + server cart, flags).
- [ ] P10-T5 Coupons + static pages + 404.

## P11 frontend checkout + account
- [ ] P11-T1 Checkout page: address, payment, coupon, preview, place order, VNPAY redirect, result page.
- [ ] P11-T2 Account: profile, password, address book.
- [ ] P11-T3 My orders list + detail (timeline, cancel, return, review).
- [ ] P11-T4 Wishlist page.

## P12 frontend admin
- [ ] P12-T1 Admin layout + dashboard.
- [ ] P12-T2 Categories, brands.
- [ ] P12-T3 Products list + product form (variants grid, images).
- [ ] P12-T4 Inventory.
- [ ] P12-T5 Orders list + detail + status actions.
- [ ] P12-T6 Customers, coupons, reviews.

## P13 hardening & delivery
- [ ] P13-T1 Local run & startup scripts (`scripts/start-all.ps1` / `scripts/start-all.sh` + frontend build/preview) work end-to-end against local MySQL 8 with seed data (no Docker).
- [ ] P13-T2 Cross-service smoke test script (`scripts/smoke.sh`: register → browse → cart → order → pay-mock → deliver).
- [ ] P13-T3 Security pass: verify R6 (internal token, header stripping, role checks) with tests; remove unused code; N+1 check on list endpoints.
- [ ] P13-T4 Root `README.md` ≤ 40 lines (run, ports, accounts, env).
