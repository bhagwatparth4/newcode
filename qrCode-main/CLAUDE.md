# Cafe QR Ordering Platform

Freelance project. This file is the permanent source of truth for this project — always follow it.

## Primary Goal

Production-quality cafe ordering system: customers scan a QR code, browse the menu, place orders,
pay online via Razorpay, and receive live order-status updates. The admin/owner manages the entire
cafe operation from a simple tablet-friendly dashboard.

**Deadline: 7 days.** Implementation must stay practical and focused — working core functionality
over extra features.

The developer is learning while building this project. Important Java/Spring/backend concepts
must be explained clearly, not hidden behind generated code.

---

## Locked Technology Stack

### Backend
- Java 21
- Spring Boot
- Maven
- Spring Web / REST APIs
- Spring Data JPA + Hibernate
- PostgreSQL
- Spring Validation
- Spring Security
- WebSocket/STOMP for real-time updates
- Redis only where genuinely useful/required
- Razorpay integration

### Frontend
- React
- **JavaScript only — never TypeScript**
- Tailwind CSS
- Mobile-first customer UI
- Tablet-friendly admin UI

### Infrastructure
- Docker + Docker Compose
- PostgreSQL must run through Docker (no native Windows install)
- Cloud deployment (later phase)
- S3-compatible object storage where required
- Git/GitHub

### Architecture
- Modular monolith — **not** microservices
- Clear separation of business modules
- Controller → Service → Repository
- DTOs at API boundaries where appropriate
- Business logic stays in service/domain layers
- Keep it understandable and maintainable

---

## Development Rules (must follow throughout)

1. Never create duplicate files.
2. Never create duplicate folders.
3. Never create nested duplicate projects.
4. Never create `backend/backend`.
5. Never create `frontend/frontend`.
6. Never create another project directory inside the project root.
7. Never create duplicate configuration files.
8. Never create duplicate classes with similar responsibilities.
9. Never create an alternative implementation when one already exists.
10. Before creating any file, inspect whether it already exists.
11. Before creating any directory, inspect whether it already exists.
12. Reuse existing files whenever appropriate.
13. Never overwrite existing work without first inspecting it.
14. Never delete files/folders without explicit authorization.
15. Never run destructive commands without explicit authorization.
16. Never install unnecessary dependencies.
17. Never introduce microservices.
18. Never use TypeScript.
19. Never hardcode secrets.
20. Never expose API keys or passwords.
21. Never implement future features unless explicitly requested.
22. Never generate the entire application in one step.
23. Work feature-by-feature.
24. Keep one source of truth for configuration.
25. Do not create unnecessary abstractions.
26. Do not create placeholder code just to make the project look complete.
27. Do not modify unrelated files.

### Before each major feature
1. Inspect the existing implementation.
2. Explain the problem.
3. Explain the concept.
4. Explain the proposed design.
5. Identify files to create/modify.
6. Explain important architectural decisions.
7. Implement only the requested feature.
8. Test the implementation.
9. Report files changed.
10. Explain what was implemented.
11. Suggest the appropriate Git commit.

---

## Customer Flow

### A) Table Customer
Each table has its own unique QR code.

```
Table QR → scan → system identifies table
 → customer opens mobile menu → browses/searches
 → selects dietary mode (VEG / NON_VEG / ALL)
 → selects items → customization if required → add to cart
 → review cart → checkout → Razorpay payment
 → backend verifies payment → order becomes PAID
 → admin gets real-time notification → starts preparing
 → customer gets real-time PREPARING update
 → admin marks READY → customer gets real-time READY update
 → admin completes order → order becomes COMPLETED
```

### B) Walk-in / Standing Customer
One separate common "Counter / Order Here" QR, not tied to a table.

```
Counter QR → scan → order type = WALK_IN
 → customer enters nickname/display name (e.g. "Kittu")
 → browses menu → cart → checkout → Razorpay
 → backend verifies payment → order becomes PAID
 → admin notified → starts preparing → customer gets PREPARING update
 → admin marks READY → customer told to collect from counter, identified by nickname
 → admin completes order
```

Staff view example: `Order #1042 · WALK_IN · Customer: Kittu · Status: READY`

The nickname is **not** a verified permanent identity.

---

## QR Code Requirements

- Every table has a unique QR; scanning it lets the backend determine the table.
- A separate common Counter/Order Here QR exists for walk-ins.
- Backend is the **source of truth** for table identity — never duplicate this logic in the frontend.
- Admin can eventually: create/manage tables, activate/deactivate, generate/download/print/regenerate QR codes.

---

## Menu Requirements

Each menu item: ID, name, description, price, image, category, food type, availability,
created/updated timestamps.

**Food type is required:** `VEG` or `NON_VEG`.

Customer dietary modes:
- **VEG** — only VEG items
- **NON_VEG** — only NON_VEG items
- **ALL** — both

The selected mode should persist across browsing/ordering where practical. Each item clearly
shows its dietary type (e.g. 🟢 VEG / 🔴 NON_VEG).

### Availability
- Admin marks items `AVAILABLE` / `UNAVAILABLE`.
- Unavailable items are not orderable; frontend communicates this clearly.
- **Backend independently validates availability — never trust the frontend.**
- If an item becomes unavailable after being cart-added, backend revalidates at order creation.

---

## Cart

Customer can add/remove items, change quantity, view subtotal, review, checkout. Customizations
must be represented in the order.

**Backend calculates the authoritative order total.** Never trust frontend price, subtotal, tax,
or total — backend fetches current menu data and computes the amount.

### Historical order price (critical business rule)
Order items preserve the **purchase-time price**. If the menu price changes later, past orders
must keep showing the price paid at the time of purchase — historical orders never change due to
current menu price changes.

---

## Order Requirements

Each order has a unique human-readable order number (e.g. `#1042`) and includes: order ID/number,
customer display name (where applicable), order type, table reference (where applicable), items,
quantity, purchase-time unit price, subtotal, tax, discount, final total, payment status, order
status, Razorpay payment/reference info, created/updated timestamps.

**Order types:** `TABLE`, `WALK_IN`

**Order status:** `PAYMENT_PENDING`, `PAID`, `PREPARING`, `READY`, `COMPLETED`, `PAYMENT_FAILED`,
`CANCELLED` (where applicable)

Status transitions are controlled by backend business rules.

---

## Payment — Razorpay

**Frontend payment success must never directly mark an order as PAID — backend must verify.**

```
Customer creates checkout request
 → backend validates cart/order
 → backend creates payment/order with Razorpay
 → frontend opens Razorpay checkout → customer pays
 → Razorpay returns payment info
 → backend verifies signature/payment
 → webhook handling where appropriate
 → backend confirms payment → order becomes PAID → admin notified
```

Must account for: signature verification, webhook verification, duplicate webhook events,
idempotency, payment failure, payment retry, controlled cancellation, refund handling where
required.

Razorpay secret credentials are never exposed to the frontend — they come from environment
configuration only.

---

## Admin Dashboard

Tablet-friendly. Navigation: Dashboard, Orders, Menu, Tables, Payments, Sales, Customers/Guests,
Settings.

Dashboard shows: today's sales, today's orders, successful/failed payments, orders by status,
average order value, recent orders.

### Admin order management
New PAID order → real-time notification (order number, table or WALK_IN, customer nickname if
available, items, quantity, total, payment status/reference) with notification sound.

Admin action flow: `PAID → PREPARING → READY → COMPLETED`, simple and tablet-friendly.

### Admin menu management
Add/edit item, change price/description/category, upload/change image, set VEG/NON_VEG, mark
AVAILABLE/UNAVAILABLE, manage categories. Prefer deactivation over destructive deletion when
historical orders depend on the item.

### Table management
Add table, rename, activate/deactivate, generate/regenerate/download/print QR. Each table QR
remains uniquely tied to its table.

### Payment history
Order number, customer display name, order type, amount, payment status, Razorpay payment ID,
Razorpay order/reference ID, payment timestamp. **Never display payment secrets.**

### Sales history
Filters: today, yesterday, this week, this month, custom range. Metrics: total sales, total
orders, average order value, successful/failed payments, refunds where applicable. Searchable/
filterable order history.

### Customer/guest history
No mandatory accounts. Display names attach to orders but are not verified identities. Admin can
see display name, order count/history, and spending where meaningful — without misrepresenting a
nickname as a verified identity.

### Analytics
Daily/weekly/monthly sales, orders per day, top-selling items, sales by item/category, table-wise
sales, walk-in sales, orders by time period. Keep it practical — no BI system.

### Export
Where practical: order number, date, time, customer display name, order type, table, items,
quantity, subtotal, tax, discount, total, payment status, payment ID, order status.

---

## Real-Time Updates

WebSocket/STOMP where appropriate.

- Admin presses **Start Preparing** → customer gets: *"Your order #1042 has started preparing."*
- Admin presses **Ready** → customer gets: *"Your order #1042 is ready."*
  - For WALK_IN: *"Your order #1042 is ready. Please collect it from the counter. Name: Kittu."*

Customer has an order tracking screen/status component.

---

## Database

PostgreSQL runs through Docker only (no native Windows install). Docker Compose for local dev.
Credentials from environment configuration — never commit real credentials.

Potential domain entities/modules (design feature-by-feature, not all at once): Admin/User, Role,
Table, MenuCategory, MenuItem, Order, OrderItem, Payment, OrderStatusHistory (where useful),
Customer/Guest info (where justified), Coupon/Discount (only if required), Notification (where
persistence is required).

---

## Security

Admin APIs protected; customer menu/order APIs expose only what's required. Never expose database
credentials, Razorpay secret, JWT secret, cloud credentials, or internal infrastructure details.
Validate all incoming data. Authorize all admin operations.

---

## Docker

PostgreSQL runs in Docker from the start. Don't add unnecessary containers — start with
PostgreSQL only. Add Redis only when its actual use case is implemented. Spring Boot may run
directly on the dev machine initially; containerize backend/frontend later if needed for
deployment.

---

## Cloud (later phase)

Target: Spring Boot backend, PostgreSQL, Redis (if needed), React frontend, object storage for
images, HTTPS, domain. Don't over-engineer deployment during initial development — it comes after
the core system works.

---

## Customer Experience Priority

Scan QR → see menu → choose VEG/NON_VEG/ALL → browse/search → add items → checkout → pay → track
order, with as few steps as possible. Mobile-first, fast, clear, touch-friendly, simple, visually
polished. Avoid unnecessary login/signup.

## Admin Experience Priority

No technical knowledge required. Common tasks in very few clicks (new order → view → start
preparing; ready → mark ready; menu item unavailable → toggle; new menu item → add; sales →
select date → view). Must work comfortably on a tablet.

---

## Development Phases

1. **Foundation** — repository, CLAUDE.md, backend foundation, Docker, PostgreSQL
2. **Database/domain foundation** — schema design, JPA entities, migrations if selected, base config
3. **Menu** — categories, items, VEG/NON_VEG, availability, admin CRUD, customer menu APIs
4. **Tables + QR** — table management, unique QR identity, counter QR, table/order context
5. **Cart + Orders** — cart, order creation, server-side totals, order states, historical pricing
6. **Razorpay** — payment order, checkout, signature verification, webhooks, idempotency, failure/retry
7. **Real-time** — WebSocket, admin notifications + sound, preparing/ready, customer tracking
8. **Admin dashboard** — dashboard, orders, menu, tables, payments, sales, history, analytics
9. **Frontend polish** — mobile UX, tablet admin UX, loading/error/empty states, responsive design
10. **Testing + deployment** — backend tests, API tests, payment tests, security review, Docker, cloud deployment, production config


Given the 7-day deadline, prioritize working core functionality over unnecessary features.

---

## Feature: Cash payment (in progress)
- Customer picks ONLINE (existing Razorpay flow, unchanged) or CASH at cart.
- Order gets new fields: paymentMethod (ONLINE/CASH), paymentStatus (PENDING/PAID), paidAt, paidBy. Old orders = ONLINE + PAID.
- CASH order skips Razorpay, goes to kitchen immediately.
- Setting toggle: cash orders enabled ON/OFF (per client).
- Admin Live Orders: "CASH – UNPAID" badge, "Mark as Paid" button (records paidAt, paidBy). WebSocket updates customer.
- Menu page "today's orders": CASH+PENDING = "Pay ₹X at counter" (orange), CASH+PAID = "Paid · Cash" + receipt.
- Receipt final only when PAID; shows payment method. Admin has "Print Receipt".
- Order token saved in localStorage so customer can reopen receipt.
- Cancel: ONLY CASH + UNPAID. Enforced in backend too. Online paid = no cancel, no refund.
- On cancel: kitchen card turns red "CANCELLED" + sound, stays until staff taps OK.
- If order is PREPARING, admin gets confirm warning before cancel.
- Show "Online paid orders cannot be cancelled or refunded" near Pay Online; add refund policy page.
- Rule: additive changes only, existing online flow must not change.

## Git Workflow

Small, focused commits. Examples:

```
chore: initialize project
chore: add docker postgres
feat: add menu module
feat: add table qr management
feat: add order creation
feat: integrate razorpay
feat: add realtime order updates
feat: add admin dashboard
test: add order service tests
```

No giant commits containing unrelated features.
