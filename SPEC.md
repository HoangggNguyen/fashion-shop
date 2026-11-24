# SPEC.md — Fashion Shop (read only the sections your task references)

## §1 Scope
Online clothing store. Roles: GUEST, CUSTOMER, ADMIN. Currency VND. Payment: COD + VNPay (sandbox; mock gateway in dev). Single shop (no multi-vendor).
Out of scope: RabbitMQ, Redis, Kafka, social login, multi-language, shipping carrier API, returns refund automation, recommendation engine.

## §2 Ports & infra
gateway 8080 | auth 8081 | catalog 8082 | inventory 8083 | cart 8084 | promotion 8085 | order 8086 | payment 8087 | notification 8088 | frontend 5173 | MySQL 3306 (localhost) | MailHog SMTP 1025 / UI 8025 (or local SMTP).
Service URLs via env, e.g. `CATALOG_URL=http://localhost:8082`. No service discovery, no Docker.

## §3 Gateway routes (Spring Cloud Gateway)
| Path prefix | Target | Auth |
|---|---|---|
| /api/v1/auth/** | auth | public (login, register, refresh, forgot/reset); rest JWT |
| /api/v1/users/**, /api/v1/addresses/** | auth | JWT |
| /api/v1/products/**, /api/v1/categories/**, /api/v1/brands/** | catalog | GET public; write ADMIN |
| /api/v1/reviews/**, /api/v1/wishlist/** | catalog | GET reviews public; others JWT |
| /api/v1/inventory/** | inventory | ADMIN |
| /api/v1/cart/** | cart | JWT |
| /api/v1/coupons/** | promotion | GET /coupons/public public; others per §4.5 |
| /api/v1/orders/** | order | JWT; admin endpoints ADMIN |
| /api/v1/payments/** | payment | callback public; rest JWT |
| /internal/** | blocked at gateway | never exposed |
Gateway also: CORS, JWT validation, header injection (RULES R6), request-id header `X-Request-Id`.

## §4 Services

### §4.1 auth-service (auth_db)
Tables:
- `users`(id, email UNIQUE, password_hash, full_name, phone, gender, birth_date, avatar_url, role ENUM(CUSTOMER,ADMIN), status ENUM(ACTIVE,LOCKED), email_verified BOOL)
- `refresh_tokens`(id, user_id, token_hash UNIQUE, expires_at, revoked BOOL)
- `password_reset_tokens`(id, user_id, token_hash UNIQUE, expires_at, used BOOL)
- `addresses`(id, user_id, receiver_name, phone, province, district, ward, street, is_default BOOL)
Endpoints:
- POST /auth/register, /auth/login, /auth/refresh, /auth/logout, /auth/forgot-password, /auth/reset-password
- GET/PUT /users/me; PUT /users/me/password
- CRUD /addresses (own only); PUT /addresses/{id}/default
- ADMIN: GET /users (paged, filter email/status), PUT /users/{id}/status, GET /users/{id}
- Internal: GET /internal/v1/users/{id} (id, email, fullName, phone) for notification/order
Rules: email unique case-insensitive; password ≥ 8 chars with letter+digit; 5 failed logins → no lockout (out of scope); reset token valid 30 min, sent via notification-service; first ADMIN seeded by migration (email from env `ADMIN_EMAIL`, password `ADMIN_PASSWORD`); max 10 addresses per user; exactly one default address.

### §4.2 catalog-service (catalog_db)
Tables:
- `categories`(id, name, slug UNIQUE, parent_id NULL, sort_order, active)
- `brands`(id, name, slug UNIQUE, logo_url, active)
- `products`(id, name, slug UNIQUE, description TEXT, category_id, brand_id NULL, gender ENUM(MEN,WOMEN,UNISEX,KIDS), base_price, sale_price NULL, status ENUM(DRAFT,ACTIVE,HIDDEN), material, avg_rating DECIMAL(3,2), review_count, sold_count, deleted)
- `product_variants`(id, product_id, sku UNIQUE, color, color_hex, size, price_override NULL, active) — UNIQUE(product_id,color,size)
- `product_images`(id, product_id, color NULL, url, sort_order, is_primary)
- `reviews`(id, product_id, user_id, order_id, rating 1-5, comment, status ENUM(VISIBLE,HIDDEN)) — UNIQUE(user_id, product_id, order_id)
- `wishlist_items`(id, user_id, product_id) — UNIQUE(user_id, product_id)
Endpoints:
- Public: GET /products (filters: q, categoryId (includes children), brandId, gender, minPrice, maxPrice, size[], color[], sort=newest|priceAsc|priceDesc|bestSelling|rating), GET /products/{slug}, GET /products/{id}/related, GET /categories (tree), GET /brands
- ADMIN: POST/PUT/DELETE(soft) /products; POST /products/{id}/variants; PUT/DELETE /variants/{id}; POST /products/{id}/images (multipart); DELETE /images/{id}; CRUD /categories, /brands; PUT /reviews/{id}/status
- CUSTOMER: GET/POST/DELETE /wishlist; POST /reviews (only if verified purchase: calls order-service `GET /internal/v1/orders/{orderId}/purchased?userId&productId` → DELIVERED/COMPLETED); GET /products/{id}/reviews (public, paged)
- Internal: POST /internal/v1/variants/batch `{variantIds[]}` → variant + product snapshot (name, slug, sku, color, size, effective price, primary image, active); POST /internal/v1/products/sold-count `{items:[{productId,qty}]}` (increment, called after order DELIVERED).
Rules: effective price = variant.price_override ?? product.sale_price ?? product.base_price. Only ACTIVE, non-deleted products public. File storage via `StorageService` Strategy: `LocalStorageService` (default; local dir `./data/uploads` or env `UPLOAD_DIR`, served at `/uploads/**`); max 5 MB, jpg/png/webp. Slug auto-generated from name (unique suffix).

### §4.3 inventory-service (inventory_db)
Tables:
- `stocks`(variant_id PK, sku, quantity, reserved, version) — available = quantity − reserved
- `stock_reservations`(id, order_ref, variant_id, qty, status ENUM(RESERVED,COMMITTED,RELEASED), expires_at) — UNIQUE(order_ref, variant_id)
- `stock_movements`(id, variant_id, type ENUM(IN,OUT,ADJUST,RESERVE,RELEASE), qty, ref, note)
Endpoints:
- ADMIN: GET /inventory (paged, filter lowStock, sku), PUT /inventory/{variantId} (set/adjust qty with note), GET /inventory/{variantId}/movements
- Internal (idempotent by `orderRef`): POST /internal/v1/reservations `{orderRef, items:[{variantId,qty}], ttlMinutes}` (all-or-nothing, 409 `STOCK_INSUFFICIENT` with failing variantIds); POST /internal/v1/reservations/{orderRef}/commit; POST /internal/v1/reservations/{orderRef}/release; GET /internal/v1/availability?variantIds=1,2
- Internal: POST /internal/v1/stocks `{variantId, sku, quantity}` (create stock row when admin creates variant; catalog-service calls it)
Rules: reserve uses `SELECT ... FOR UPDATE` on stock rows ordered by variant_id; scheduler every 1 min releases expired RESERVED reservations; commit decrements `quantity` and `reserved`; release decrements `reserved`. Low stock threshold: 5.

### §4.4 cart-service (cart_db)
Tables: `carts`(id, user_id UNIQUE), `cart_items`(id, cart_id, variant_id, qty) UNIQUE(cart_id, variant_id).
Endpoints (CUSTOMER): GET /cart (enriched: calls catalog batch + inventory availability; flags `unavailable`, `priceChanged`), POST /cart/items `{variantId, qty}` (adds/increments), PUT /cart/items/{variantId} `{qty}` (0 = remove), DELETE /cart/items/{variantId}, DELETE /cart, POST /cart/merge `{items:[{variantId,qty}]}` (guest cart → server cart after login).
Internal: GET /internal/v1/carts/{userId} (raw items), DELETE /internal/v1/carts/{userId}.
Rules: qty 1..10 per item; qty capped by availability on read (flag, do not mutate). Price is never stored in cart.

### §4.5 promotion-service (promotion_db)
Tables:
- `coupons`(id, code UNIQUE, type ENUM(PERCENT,FIXED,FREE_SHIP), value, min_order_amount, max_discount NULL, usage_limit NULL, per_user_limit DEFAULT 1, used_count, start_at, end_at, status ENUM(ACTIVE,DISABLED), description)
- `coupon_usages`(id, coupon_id, user_id, order_ref, status ENUM(REDEEMED,RELEASED)) UNIQUE(coupon_id, order_ref)
Endpoints: GET /coupons/public (active list); POST /coupons/validate `{code, subtotal, shippingFee}` (JWT) → `{discount, shippingDiscount, valid, reason}`; ADMIN CRUD /coupons.
Internal (idempotent by orderRef): POST /internal/v1/coupons/redeem `{code, userId, orderRef, subtotal, shippingFee}`; POST /internal/v1/coupons/release `{orderRef}`.
Rules: `DiscountStrategy` per type; validation via Chain of Responsibility: exists → active → in date range → min order → global limit → per-user limit. PERCENT capped by max_discount. Discount never exceeds subtotal. One coupon per order.

### §4.6 order-service (order_db)
Tables:
- `orders`(id, order_no UNIQUE (format `FS` + yyMMdd + 6 random digits), user_id, status, subtotal, discount_amount, shipping_fee, total, coupon_code NULL, payment_method ENUM(COD,VNPAY), payment_status ENUM(UNPAID,PAID,FAILED,REFUNDED), receiver_name, receiver_phone, ship_province, ship_district, ship_ward, ship_street, note, idempotency_key UNIQUE, cancel_reason NULL)
- `order_items`(id, order_id, product_id, variant_id, product_name, sku, color, size, image_url, unit_price, qty, line_total) — snapshots
- `order_status_history`(id, order_id, from_status, to_status, actor_id, note, created_at)
- `outbox_events`(id, type, payload JSON, status ENUM(PENDING,SENT,FAILED), attempts, next_attempt_at)
Status machine (`OrderStatus`):
`PENDING_PAYMENT → CONFIRMED | CANCELLED`; `PENDING → CONFIRMED | CANCELLED` (COD initial = `PENDING`); `CONFIRMED → PROCESSING | CANCELLED`; `PROCESSING → SHIPPING`; `SHIPPING → DELIVERED`; `DELIVERED → COMPLETED | RETURN_REQUESTED`; `RETURN_REQUESTED → RETURNED | DELIVERED`. Terminal: CANCELLED, COMPLETED, RETURNED.
Customer may cancel only in PENDING, PENDING_PAYMENT, CONFIRMED. Auto-complete DELIVERED → COMPLETED after 7 days (scheduler). Auto-cancel PENDING_PAYMENT after 30 min unpaid (scheduler; releases stock/coupon).
Endpoints:
- CUSTOMER: POST /orders (header Idempotency-Key; body `{addressId | inline address, paymentMethod, couponCode?, note?}`; items come from server cart), GET /orders (own, paged, filter status), GET /orders/{orderNo}, POST /orders/{orderNo}/cancel `{reason}`, POST /orders/{orderNo}/return-request `{reason}`, POST /orders/preview `{addressId?, couponCode?}` → totals before placing
- ADMIN: GET /admin/orders (filter status, date range, q=orderNo/phone), GET /admin/orders/{orderNo}, PUT /admin/orders/{orderNo}/status `{status, note}`, GET /admin/stats/summary (revenue, order count by status, last 7/30 days; top 10 products by qty)
- Internal: GET /internal/v1/orders/{orderId}/purchased?userId&productId; POST /internal/v1/orders/{orderNo}/payment-result `{status: PAID|FAILED}` (from payment-service).
Shipping fee `ShippingFeeStrategy`: `FlatRate` 30,000 VND; `FreeOverThreshold` free if subtotal ≥ 500,000. Active strategy via config `shipping.strategy`.

### §4.7 payment-service (payment_db)
Tables: `payments`(id, order_no, user_id, method ENUM(COD,VNPAY), amount, status ENUM(PENDING,SUCCESS,FAILED,CANCELLED), txn_ref UNIQUE, provider_txn_no NULL, raw_response TEXT, paid_at)
`PaymentStrategy` impls: `CodPaymentStrategy` (creates PENDING record; marked SUCCESS when order DELIVERED), `VnPayPaymentStrategy` (HMAC-SHA512 sign per VNPay spec; sandbox URL from env), `MockPaymentStrategy` (profile `dev`: returns local URL that auto-succeeds).
Endpoints: POST /internal/v1/payments `{orderNo, userId, amount, method, returnUrl}` → `{paymentId, redirectUrl?}`; GET /payments/vnpay/return (browser redirect, verifies signature, redirects to frontend `/orders/{orderNo}/result`); GET /payments/vnpay/ipn (server callback; idempotent by txn_ref; verifies signature + amount; then calls order-service payment-result); GET /payments/order/{orderNo} (owner/admin); POST /internal/v1/payments/{orderNo}/cod-collected.

### §4.8 notification-service (notification_db)
Tables: `notifications`(id, user_id, email, type, subject, body, status ENUM(PENDING,SENT,FAILED), attempts, sent_at).
Internal endpoint: POST /internal/v1/notifications `{type, to, templateData}`; types: `WELCOME`, `RESET_PASSWORD`, `ORDER_CREATED`, `ORDER_STATUS_CHANGED`, `PAYMENT_SUCCESS`. Templates: Thymeleaf HTML in `resources/templates/mail/`. Send via `JavaMailSender` (MailHog in dev). Async via `@Async` + retry up to 3 (scheduler picks FAILED/PENDING). Callers: auth (WELCOME, RESET_PASSWORD) directly; order-service via outbox.

## §5 Screens (frontend)

### Customer (public layout: header with search/cart/account, footer)
| Route | Screen | Key features |
|---|---|---|
| / | Home | banner carousel (static config), category tiles, new arrivals, best sellers, sale products |
| /products | Product list | filters (category, brand, gender, price range, size, color), sort, pagination, search `q`, URL-synced query |
| /products/:slug | Product detail | image gallery by color, variant picker (color/size) with stock state, price/sale, add to cart, wishlist toggle, description, reviews list + form, related products, size guide modal |
| /cart | Cart | item list, qty edit, remove, subtotal, flags for unavailable/price change, proceed to checkout |
| /checkout | Checkout | address select/add, payment method, coupon input + validate, order summary via preview, place order |
| /orders/:orderNo/result | Order result | success/fail, order summary, retry payment link (VNPAY) |
| /login, /register, /forgot-password, /reset-password | Auth | forms with validation; guest cart merge on login |
| /account/profile | Profile | edit info, change password |
| /account/addresses | Address book | CRUD, set default |
| /account/orders | My orders | status tabs, pagination |
| /account/orders/:orderNo | Order detail | items, timeline (status history), cancel, return request, review items |
| /account/wishlist | Wishlist | list, remove, move to cart |
| /coupons | Coupons | public active coupons, copy code |
| /about, /contact, /policy, * | Static + 404 | static content |

### Admin (`/admin/*`, ADMIN only, sidebar layout)
| Route | Screen | Key features |
|---|---|---|
| /admin | Dashboard | revenue, orders by status, top products (from stats endpoint) |
| /admin/products | Products | table, search, status filter, create/edit/soft-delete |
| /admin/products/new, /:id | Product form | info, variants grid (color×size, sku), images upload per color, initial stock |
| /admin/categories | Categories | tree CRUD |
| /admin/brands | Brands | CRUD |
| /admin/inventory | Inventory | stock table, low-stock filter, adjust with note, movements drawer |
| /admin/orders | Orders | filters, status badge, bulk none |
| /admin/orders/:orderNo | Order detail | items, customer, address, payment, status change buttons (only valid next states), history |
| /admin/customers | Customers | list, lock/unlock |
| /admin/coupons | Coupons | CRUD |
| /admin/reviews | Reviews | list, hide/show |

## §6 Cross-service flows

### §6.1 Checkout saga (orchestrated by order-service, `CheckoutOrchestrator`)
Steps (each a `SagaStep` with `execute()` + `compensate()`):
1. Idempotency check (`Idempotency-Key`): if order exists → return it.
2. Load cart (cart-service); empty → 422 `CART_EMPTY`.
3. Load variant snapshots (catalog batch); inactive/missing → 422 `ITEM_UNAVAILABLE`.
4. Load address (auth-service internal) or use inline.
5. Compute subtotal, shipping fee (Strategy).
6. Reserve stock (inventory) with `orderRef = orderNo`, ttl 30 min. Compensate: release.
7. Redeem coupon if present (promotion). Compensate: release.
8. Persist order + items + history + outbox `ORDER_CREATED` in ONE transaction. Status: COD → `PENDING`; VNPAY → `PENDING_PAYMENT`.
9. Create payment (payment-service). VNPAY: return `redirectUrl`. Failure → compensate 7,6 and mark order CANCELLED.
10. Clear cart (best effort, failure ignored + logged).
Payment result callback: SUCCESS → order `CONFIRMED`, payment_status PAID, inventory commit; FAILED → order `CANCELLED`, release stock + coupon.
COD: stock committed when order moves to `CONFIRMED` (admin action). Cancel after commit → inventory `ADJUST` IN (internal endpoint `POST /internal/v1/reservations/{orderRef}/restock`).
On DELIVERED: call catalog sold-count; COD payment marked collected.

### §6.2 Events via outbox (order-service → notification-service)
`ORDER_CREATED`, `ORDER_STATUS_CHANGED`, `PAYMENT_SUCCESS`. Scheduler every 10s picks PENDING rows with `next_attempt_at <= now`, POSTs to notification-service; success → SENT; failure → attempts++, exponential backoff (max 5, then FAILED).

### §6.3 Error codes (common)
`VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `INTERNAL_ERROR`, `EMAIL_EXISTS`, `INVALID_CREDENTIALS`, `TOKEN_INVALID`, `PRODUCT_NOT_FOUND`, `SKU_EXISTS`, `STOCK_INSUFFICIENT`, `CART_EMPTY`, `ITEM_UNAVAILABLE`, `COUPON_INVALID`, `ORDER_INVALID_STATE`, `PAYMENT_SIGNATURE_INVALID`, `UPSTREAM_ERROR`.

## §7 Non-functional
- Every list endpoint paged. N+1 forbidden (use fetch joins/`@EntityGraph`/batch endpoints).
- OpenAPI available at `/swagger-ui.html` per service.
- Seed data (dev profile, Flyway `R__seed_dev.sql` or runner): 1 admin, 1 customer, 5 categories, 20 products with variants/images (placeholder URLs), 3 coupons.
- Logging: JSON-less plain; include `X-Request-Id` in MDC.
