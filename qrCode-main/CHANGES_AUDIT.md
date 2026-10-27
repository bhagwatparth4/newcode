# Rush Hours QR Ordering: Change Audit

This file lists every change made while completing the project, and why.
Nothing was renamed, no table or column was dropped, and no working
module was replaced. The architecture (Spring Boot modular monolith +
React JavaScript + Tailwind) is unchanged.

Tip: compare against your original zip with any diff tool (for example
IntelliJ: right-click the folder, "Compare With...").

---

## 1. Backend: new files (4)

| File | Why |
|---|---|
| `service/order/OrderStatusTransitionPolicy.java` | One small class that decides who may change an order's status. Admin and Kitchen services both use it, so the rules cannot drift apart. Kitchen can never mark COMPLETED. |
| `service/order/OrderStatusMessages.java` | The customer message for Preparing / Ready / Completed, shared by Admin and Kitchen (it was duplicated before). |
| `service/payment/RazorpayPaymentDetails.java` | Reads the UPI RRN / bank reference from a Razorpay payment, so receipts and Excel can show it. |
| `src/test/.../OrderStatusTransitionPolicyTest.java` | Unit test for the status rules. No database needed. |

## 2. Backend: bugs and security gaps fixed

### Business mode (SMALL_CAFE / LARGE_CAFE / RESTAURANT)
- `CafeSettingServiceImpl`: the business type now comes from the `BUSINESS_TYPE` setting (vendor configuration per client). Before, it was read from the database with no way to set it. The old database column is kept and marked as unused (`CafeSetting.java` comment).
- `AdminUserServiceImpl`: **creating a Kitchen user is now rejected for SMALL_CAFE** (it was allowed before). Clear error messages instead of "Something went wrong".
- `AdminAuthServiceImpl`: Kitchen users cannot log in when the cafe is SMALL_CAFE. Password is checked before revealing whether an account is inactive. The login response now includes `businessType`.
- `CafeSettingController`: new read-only `GET /api/admin/settings/business-type`. There is no endpoint to change it (by design).

### Order status and permissions
- `AdminOrderServiceImpl` and `KitchenOrderServiceImpl` now use `OrderStatusTransitionPolicy`.
- Both now **lock the order row** while changing status (`findLockedById`), so two clicks at the same time cannot both succeed.
- Kitchen now only receives cooking information (no customer mobile, no payment IDs).
- The Kitchen order list uses a database query instead of loading every order ever placed.
- New `GET /api/admin/orders/active` for the Admin live board.
- After every status change, Admin and Kitchen screens get a realtime refresh signal (`/topic/admin/order-updates`, `/topic/kitchen/order-updates`).
- `SecurityConfig`: `/api/customer/marketing/**` now requires ADMIN (Kitchen could reach it before).

### Payments (Razorpay)
- `PaymentWebhookServiceImpl`:
  - **The coupon could be consumed twice.** This happened when a webhook arrived after the order had already moved past PAID. It is now fixed.
  - The webhook no longer overwrites payment data that `/verify` already saved. This was a race between the two.
  - Payments that don't belong to this app are acknowledged and ignored, instead of Razorpay retrying them forever.
  - A wrong-amount webhook never marks the order PAID. It is logged as an ALERT.
  - An invalid or missing signature now returns HTTP 400 instead of 500.
  - The UPI RRN is saved.
- `PaymentServiceImpl`:
  - The UPI RRN / bank reference is now saved.
  - `markOrderAsPaid` now restarts the preparation estimate from the moment of payment.
  - It notifies the customer for both the verify path and the webhook path. Before, the webhook path never told the customer.
  - It rings the right dashboard.
- Existing protections were kept unchanged:
  - Idempotency key.
  - Row locks.
  - Unique payment IDs.
  - Signature check.
  - "captured" check.
  - Amount check.
  - Coupon reservation.

### Orders
- `OrderServiceImpl`:
  - `getOrderById` loaded the whole `order_items` table. It now loads only that order's items.
  - Customer order details now include subtotal, discount, coupon, last update time and a **masked** mobile number (`******3210`), which is needed for the receipt.
  - Nickname and mobile are trimmed.
- `OrderCreateRequest`: the mobile number must be a valid 10-digit Indian mobile.
- `OrderController`, `PaymentController`: the rate limit went from 10 to 30 per minute per IP. All customers on the cafe Wi-Fi share one IP.

### Web Push
- `OrderNotificationServiceImpl`: Web Push was never actually sent. It now is (after the database commit):
  - TABLE: "Your order for Table T5 is ready."
  - COUNTER: "Your order is ready. Please collect it from the counter."
  - Failures never break ordering.
- `WebPushService(Impl)`: `isConfigured()` / `getPublicKey()`. Without VAPID keys, push is simply skipped.
- `PushSubscriptionController`: new `GET /api/customer/push/public-key` (the public key is safe to share).
- `PushSubscriptionServiceImpl` + `PushSubscriptionRequest`: subscribing now requires the order's secret token. Before, anyone could subscribe to any order ID.

### Menu, categories, tables, coupons (clear errors, safe deletes)
- `MenuItemServiceImpl`:
  - Validation for name, category, price ≥ ₹1 and VEG/NON_VEG.
  - Deleting an item used in old orders now says "mark it unavailable instead" instead of crashing.
  - New `getPublicMenuItems()` hides items of hidden categories. `PublicMenuController` uses it.
- `CategoryServiceImpl` + `CategoryRequest`:
  - Name validation and a duplicate-name message.
  - Optional `active` flag to hide or show a category.
  - Safe delete.
- `TableServiceImpl`:
  - Table number validation and a duplicate message.
  - Safe delete ("deactivate instead").
- `CounterQrServiceImpl`, `CustomerMarketingServiceImpl`: "not found" now returns 404 instead of 500.
- `CouponServiceImpl`:
  - Proper 400/404/409 errors.
  - The coupon preview uses the same "must pay at least ₹1" rule as order creation, so the customer sees the exact amount.

### Reports and Excel
- `OrderItemRepository`: top items and VEG/NON-VEG now count all **paid** orders (PAID, PREPARING, READY, COMPLETED). This is the same set of orders used for revenue. Before, only COMPLETED orders were counted.
- `AdminSalesServiceImpl`:
  - "Today's orders" no longer counts abandoned checkouts.
  - Report ranges are limited to one year.
- `AdminExcelExportService`:
  - Newest orders first, with separate date and time columns.
  - New columns: Subtotal, Coupon, Discount and UPI RRN.
  - A bold header row.
  - It no longer fails on servers without fonts.
  - The file name is still `RushHour_Orders.xlsx`.
- `AdminOrderResponse`: added subtotal, discount, coupon and updatedAt (needed for admin receipts). `AdminOrderServiceImpl` lists newest first.
- Controllers: date parameters are now explicitly ISO (`2026-09-23` / `2026-09-23T00:00:00`).

### General
- `GlobalExceptionHandler`: unknown URLs return 404, wrong or missing parameters return 400, and "forbidden" returns 403. Before, all of these returned 500. Internal details are still never shown.
- `BackendApplication`: sets the time zone (default `Asia/Kolkata`, env `APP_TIMEZONE`). Without this, the customer countdown would be 5½ hours wrong on a cloud server running in UTC.
- `application.yaml`:
  - Database URL and user can be set by env.
  - Web Push and SMS keys are optional.
  - New settings: `BUSINESS_TYPE`, `CAFE_NAME`.
- `.env.example`: documents every setting with placeholders. No real credentials anywhere.

## 3. Frontend (new, `frontend/`)

React (JavaScript, no TypeScript) + Tailwind + Vite. Only 4 runtime libraries: react, react-dom, react-router-dom and @stomp/stompjs.

| Area | Files |
|---|---|
| Customer flow | `customer/MenuPage.jsx` (QR → nickname → menu, search, VEG/NON-VEG/ALL kept for the session), `ItemSheet.jsx`, `CartPage.jsx` (coupon, mobile, Razorpay), `OrderPage.jsx` (printer animation → receipt → live tracking + countdown + Web Push) |
| Admin | `admin/AdminLayout.jsx`, `DashboardPage`, `LiveOrdersPage`, `OrdersPage` (filters + Excel + receipts), `MenuAdminPage`, `TablesPage` (QR download), `CouponsPage`, `CustomersPage` (opt-out + offers), `SettingsPage` |
| Kitchen | `kitchen/KitchenBoard.jsx` (tablet cards, bell, only Preparing/Ready buttons) |
| Receipt | `lib/receipt.js` (real PDF with logo, no extra library), `components/ReceiptPaper.jsx`, `components/ThermalPrinter.jsx` |
| Realtime | `lib/useRealtime.js` (one connection, re-fetches on reconnect), `lib/useOrderBell.js` (bell once per order, browser sound unlock) |
| Branding | `config/brand.js` + colours in `index.css` + logo files in `public/` |

### How the frontend handles payment safety
- The same cart reuses the same idempotency key. Double tap, refresh, retry and back button all return the same order, never a new one.
- "Paid" is shown only after the backend confirms. If `/verify` fails but the money was taken, the order page waits for the webhook confirmation.
- Prices and discounts shown are previews. The server recalculates everything.

## 4. What was verified, and what was NOT

| Check | Result |
|---|---|
| All 173 Java files parsed with the Java 21 compiler | 0 errors |
| Our own Java code checked for wrong method/field names (Spring libraries were not available offline) | 0 errors |
| Status rules executed for every business type / role / status | Exactly matches the spec |
| Whole frontend bundled with esbuild | 0 errors, all imports resolve |
| PDF receipt generated and rendered | Correct |
| `mvn clean test` / `npm install && npm run build` | **Not run** (no internet in my environment). Run them yourself (see SETUP.md). |
| Real Razorpay payment, webhook, Web Push, SMS | **Not tested**. Needs real keys. Test with Razorpay Test Mode first. |

## 5. Known limitations (kept honest)
- Razorpay order creation is not "exactly once" on Razorpay's side. Our database reuses the existing Razorpay order for the same cafe order, which prevents duplicates in practice.
- Admin "Order history" loads the chosen date range at once. That is fine for a cafe, but for very long ranges use Excel export.
- `payment-test.html` and `websocket-test.html` (dev test pages) are still publicly reachable. Remove them or block them before going live.
- The existing `BackendApplicationTests.contextLoads` test needs Postgres, Redis and env variables to be running.

---

## 6. Update: photo upload + category header photos

**Rule followed:** only additions. No existing API, field, table or feature
was removed or renamed. The `imageUrl` fields on menu items and categories
work exactly as before; they are simply filled by the upload now.

### Backend (new files)
| File | Why |
|---|---|
| `service/storage/ImageStorage.java` | Interface: "store this photo, give me its public URL". |
| `service/storage/ImageUploadService.java` | Safety checks. Only JPG, PNG and WEBP, detected from the real file bytes (SVG, HTML and renamed files are rejected). Max 5 MB, a random file name and allowed folders only. |
| `service/storage/LocalImageStorage.java` | `STORAGE_PROVIDER=local` (default): saves on the server. Development only. |
| `service/storage/S3ImageStorage.java` | `STORAGE_PROVIDER=s3`: AWS S3. |
| `service/storage/CloudinaryImageStorage.java` | `STORAGE_PROVIDER=cloudinary`: Cloudinary signed upload. |
| `config/LocalUploadsWebConfig.java` | In local mode only, serves saved photos read-only at `/uploads/...`. |
| `controller/admin/AdminImageUploadController.java` | `POST /api/admin/uploads/image?folder=menu\|category`. ADMIN only, rate-limited. |

Only one storage class is active at a time, chosen by `STORAGE_PROVIDER`.

### Backend (small edits)
- `SecurityConfig`: public **GET** for `/uploads/**` (read-only photos in local mode).
- `GlobalExceptionHandler`: a clear "Photo is too large" message.
- `application.yaml`: 5 MB upload limit, plus the storage settings (all from env).
- `pom.xml`: AWS SDK `s3` dependency, used only in S3 mode.
- `.env.example`: the storage settings. `backend/.gitignore`: `uploads/`.

### Frontend
- New `components/ImageUpload.jsx`: an "Upload photo" box with preview, "Change photo" and "Remove". "Use a link instead" is still available.
- New `lib/prepareImage.js`:
  - Resizes the photo in the browser (max 1600 px, JPEG), so an 8 MB phone photo becomes about 300 KB.
  - Removes hidden photo data, including the GPS location.
- `api/client.js`: new `uploadImage()` function (existing functions unchanged).
- `admin/MenuAdminPage.jsx`:
  - The item form uses the upload box instead of the URL text box.
  - The category form gets a "Header photo".
  - The category chips show a small thumbnail.
- `customer/MenuPage.jsx`: if a category has a header photo, the customer sees a banner with the category name on top. Without a photo, the heading looks exactly as before.

### Verified here
- Java: 180 files parsed with 0 errors, and our own code checked with 0 errors.
- Photo type check actually executed:
  - JPG, PNG and WEBP are accepted.
  - SVG and a fake `.jpg` are rejected.
- Frontend: bundle builds with 0 errors.

### Not tested here (needs your accounts)
- Real upload to AWS S3 or Cloudinary. Test once with your keys (SETUP.md §10).
