# Vehicle Health Report (RC View)

Full-stack application: **Java (Spring Boot)** backend + **React** frontend. Users sign in with **Google SSO**, search by vehicle registration number, and view Vahan API data (RC, owner, vehicle, insurance, PUC, loan, etc.) with quick filter chips. Admins manage users and app config (cache TTL, rate limits).

## Features

- **Google SSO** (company Google); admin enables SSO per user
- **Vehicle search** by registration number; data from Vahan API with **caching** (MongoDB, TTL configurable by admin, default 3 days)
- **Rate limiting**: per-second and per-day (configurable by admin); attack-resistant
- **Audit logs**: search, API calls, cache hits, user/login events
- **Admin**: add/remove users, set role (USER/ADMIN), enable SSO; configure cache TTL and daily/per-second limits
- **Super admin** `vikas.kumar8@cars24.com` cannot be removed or demoted
- **GA events**: search events (registration number, success, from_cache) when `VITE_GA_MEASUREMENT_ID` is set

## Tech stack

- **Backend**: Java 17, Spring Boot 3.2, Spring Security (OAuth2 + JWT), Spring Data MongoDB, Bucket4j (rate limit)
- **Frontend**: React 18, Vite, Tailwind CSS, React Router
- **DB**: MongoDB

## Prerequisites

- JDK 17+, Maven, Node 18+, MongoDB (local or Atlas)
- Google OAuth2 client (Client ID + Secret) for your domain
- Vahan API key (for vehicle search)

## Certificate / SSL errors (corporate network)

If `mvn spring-boot:run` fails with **"PKIX path building failed"** or **"unable to find valid certification path"**, your JDK doesn’t trust the certificate used by Maven Central (common with corporate proxies).

**Step-by-step fix:** see **`backend/FIX-CERTIFICATE-STEPS.md`** and follow it in order (get cert from Chrome or IT → import into JDK → clear Maven cache → run again).

**Should I change the backend language?**  
Switching to Node.js or Python doesn’t automatically fix this: **npm** and **pip** also use HTTPS and can hit the same certificate issue on the same network. The fix is still to trust the corporate CA (in the JDK for Java, or in the system/Node store for npm). In some environments Node uses the system certificate store, which may already include the corporate CA — so if you can’t fix the Java truststore, you could try a Node backend and run `npm install`; if that works, you can use Node for the backend instead. The frontend and API contract stay the same.

## Backend setup

```bash
cd backend
# Optional: set env or use application.yml
export MONGODB_URI=mongodb://localhost:27017/rcview
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret
export VAHAN_API_KEY=your-vahan-api-key
export JWT_SECRET=at-least-32-characters-secret-key-for-jwt-signing
export FRONTEND_URL=http://localhost:3000

mvn spring-boot:run
```

- Server runs at **http://localhost:8080**, context path **/api**
- First login with `vikas.kumar8@cars24.com` creates the super admin user; other users must be added by admin and SSO enabled

**Google OAuth**: In Google Cloud Console, create OAuth 2.0 credentials (Web application). Authorized redirect URI:  
`http://localhost:8080/api/login/oauth2/code/google` (or your backend base + `/api/login/oauth2/code/google`).

## Frontend setup

```bash
cd frontend
npm install
npm run dev
```

- App runs at **http://localhost:3000**
- Vite proxies `/api` to `http://localhost:8080` so API calls and OAuth redirects work

**Login flow**: User clicks “Sign in with Google” → redirect to backend `/api/oauth2/authorization/google` → Google → backend callback → backend issues JWT and redirects to `http://localhost:3000/#token=...` → frontend stores token and uses it for API calls.

## Environment variables

### Backend (env or `application.yml`)

| Variable | Description |
|---------|-------------|
| `MONGODB_URI` | MongoDB connection string (default: `mongodb://localhost:27017/rcview`) |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret |
| `VAHAN_API_KEY` | Vahan API key |
| `JWT_SECRET` | Min 32 chars for JWT signing |
| `FRONTEND_URL` | Frontend URL for post-login redirect (default: `http://localhost:3000`) |

### Frontend (`.env`)

| Variable | Description |
|---------|-------------|
| `VITE_GA_MEASUREMENT_ID` | Optional; GA measurement ID for search events |

## API overview

- `GET /api/auth/me` – current user (requires JWT)
- `POST /api/vehicle/search` – body `{ "registrationNumber": "MH12AB1234" }` (rate limited)
- `GET /api/vehicle/rate-limit` – remaining searches today
- `GET/POST/DELETE/PATCH /api/admin/users*` – admin user CRUD and role
- `GET/PUT /api/admin/config` – cache TTL, rate limits
- `GET /api/admin/audit-logs` – paginated audit logs

## Security

- Per-user rate limit (per second + per day) to prevent abuse
- JWT after OAuth; no session cookie across origins
- Super admin protected from removal/role change
- CORS configured for frontend origin(s)

## Project structure

```
backend/                 # Spring Boot
  src/main/java/.../     # config, entity, repository, service, controller, security
frontend/                # Vite + React
  src/
    api/                 # API client
    components/          # Layout, Header, Sidebar, VehicleResult, VehicleNoData
    context/             # Auth, Search
    pages/               # Login, Dashboard, Admin
    utils/               # vehicleSections (grouping), ga (events)
```
