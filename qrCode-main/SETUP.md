# Rush Hours QR Ordering: Setup Guide

## 1. Requirements
- Java 21, Maven (or the included `mvnw`)
- Node.js 20+ (for the frontend)
- Docker Desktop (for PostgreSQL + Redis)

## 2. Start the database and Redis
```bash
cp .env.example .env        # then edit .env
docker compose up -d
```

## 3. Run the backend
Spring Boot does **not** read `.env` by itself. Put the same values into
your IntelliJ Run Configuration → *Environment variables* (or export them
in your terminal).

Minimum required:
```
POSTGRES_PASSWORD=...
JWT_SECRET=...        (64+ characters: openssl rand -base64 64)
RAZORPAY_KEY_ID=rzp_test_...
RAZORPAY_KEY_SECRET=...
RAZORPAY_WEBHOOK_SECRET=...
BUSINESS_TYPE=SMALL_CAFE      (or LARGE_CAFE / RESTAURANT)
```
Then:
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
With the `dev` profile, a first admin is created:
`admin@rushhour.com` / `Admin@12345`. **Change this before going live.**

Run the unit test for the status rules (no database needed):
```bash
./mvnw -Dtest=OrderStatusTransitionPolicyTest test
```
Full `./mvnw clean test` also needs Postgres + Redis running and the env variables set.

## 4. Run the frontend
```bash
cd frontend
cp .env.example .env
npm install
npm run dev          # http://localhost:5173
npm run build        # production files in frontend/dist
```

## 5. First-time setup (as Admin)
1. Log in at `http://localhost:5173/login`.
2. **Menu** → add categories, then items (price, VEG/NON-VEG, photo link).
3. **Tables & QR** → add tables (T1, T2…), download each QR and print it. Create and download the **Counter QR**.
4. **Settings** → set the preparation time. For LARGE_CAFE / RESTAURANT, create the kitchen login.
5. Scan a QR with your phone (both devices on the same network, and `FRONTEND_URL` set to your computer's IP, e.g. `http://192.168.1.10:5173`).

## 6. Razorpay
- Use **Test Mode** keys first and pay with the test UPI/card details from the Razorpay docs.
- Dashboard → Settings → **Payment capture: Automatic** (the backend only accepts *captured* payments).
- Dashboard → Webhooks → URL `https://YOUR-BACKEND/api/payments/razorpay/webhook`, events `payment.captured` and `payment.failed`, and the same secret as `RAZORPAY_WEBHOOK_SECRET`.
- The webhook needs a public HTTPS URL, so test it after deployment or with a tunnel (e.g. ngrok).

## 7. Web Push (optional)
```bash
npx web-push generate-vapid-keys
```
Put the keys in `VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY`, and set `VAPID_SUBJECT=mailto:you@example.com`.
Web Push only works on HTTPS (or localhost). Without keys, everything else still works.

## 8. New client (re-branding)
1. Replace `frontend/public/logo.png`, `logo-receipt.jpg`, `favicon.png`, `icon-192.png`.
2. Frontend `.env`: `VITE_CAFE_NAME`, `VITE_CAFE_TAGLINE`, `VITE_CAFE_ADDRESS`, `VITE_CAFE_PHONE`.
3. Colours: the `@theme` block at the top of `frontend/src/index.css`.
4. Backend env: `BUSINESS_TYPE`, `CAFE_NAME`, the client's Razorpay keys, and `FRONTEND_URL`.

## 9. Before going live (checklist)
- [ ] Real Razorpay **live** keys + webhook configured and tested with one real ₹1 order
- [ ] Strong `JWT_SECRET`, and the default admin password changed
- [ ] `FRONTEND_URL` = the real https domain (CORS + QR codes use it). **Reprint the QRs** if it changes.
- [ ] HTTPS on both frontend and backend
- [ ] Remove or block `payment-test.html` and `websocket-test.html`
- [ ] Set `show-sql: false` in `application.yaml` for production logs
- [ ] Database backups enabled

## 10. Menu & category photos (storage)
Choose with `STORAGE_PROVIDER` in the backend environment. Photos are saved in storage, and only their URL goes into the database.

### Development: `local` (default)
Nothing to set up. Photos are saved in `backend/uploads/`.
If you open the site from a phone, set `BACKEND_PUBLIC_URL=http://YOUR-PC-IP:8080`.
**Do not use `local` in production:** many hosts delete these files on redeploy.

### AWS S3: `STORAGE_PROVIDER=s3`
1. AWS Console → S3 → **Create bucket** (e.g. `rushhour-photos`, region `ap-south-1` Mumbai).
2. In the bucket → Permissions:
   - Turn **off** "Block all public access" for this bucket.
   - Add this bucket policy (read-only public access to photos):
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [{
       "Effect": "Allow",
       "Principal": "*",
       "Action": "s3:GetObject",
       "Resource": ["arn:aws:s3:::rushhour-photos/menu/*", "arn:aws:s3:::rushhour-photos/category/*"]
     }]
   }
   ```
3. IAM → create a user with only `s3:PutObject` permission on `arn:aws:s3:::rushhour-photos/*`, then create an access key.
4. Env:
   ```
   STORAGE_PROVIDER=s3
   S3_BUCKET=rushhour-photos
   S3_REGION=ap-south-1
   S3_ACCESS_KEY=...
   S3_SECRET_KEY=...
   ```

### Cloudinary: `STORAGE_PROVIDER=cloudinary`
1. Create a Cloudinary account. The **Dashboard** shows your Cloud name, API Key and API Secret.
2. Env:
   ```
   STORAGE_PROVIDER=cloudinary
   CLOUDINARY_CLOUD_NAME=...
   CLOUDINARY_API_KEY=...
   CLOUDINARY_API_SECRET=...
   ```

### Test it
Admin → Menu → Add item → **Upload photo** → Save. Then open the customer menu: the photo should appear.
Add a header photo to a category, and a banner appears above that category on the customer menu.
