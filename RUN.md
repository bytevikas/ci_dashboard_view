# How to run the application

## Final steps to run (summary)

1. **Backend**
   - Set `VAHAN_API_KEY` (required for vehicle search).
   - For **Dev mode** (no Google SSO): set `DEV_MODE=true`.
   - For **Admin in dev mode**: set `MONGODB_URI` (local or Atlas) so user list and app config load/save work.
   - From repo root: `cd backend && export DEV_MODE=true && export MONGODB_URI='your-uri-if-using-admin' && mvn spring-boot:run`  
     (Or use `./run.sh` if you hit SSL/certificate errors; set env vars in the same shell.)
2. **Frontend**
   - `cd frontend && npm install && npm run dev`
3. **Use the app**
   - Open **http://localhost:3000** (or the port Vite shows, e.g. 3011 if 3000 is in use) → Login → **Dev Login (no SSO, no DB)**.
   - You are logged in as an **admin** user: use **Dashboard** for vehicle search and **Admin** (sidebar) for users and app config.

### MongoDB Atlas: "Service Unavailable" / "Received fatal alert: internal_error" (SSL)

If you see `SSLException: (internal_error) Received fatal alert: internal_error` or **"Timed out after 10000 ms"** when connecting to Atlas, the JVM’s TLS setup doesn’t match what Atlas expects (common on corporate networks or older JDKs). The app now **forces TLS 1.2 and 1.3** at startup, which fixes this in most cases. Restart the backend after pulling the change. If it still fails:

- Try from a different network (e.g. mobile hotspot) to rule out corporate proxy/firewall.
- **Quick fix:** Use local MongoDB instead of Atlas. From `backend`: run `./run-with-local-mongo.sh` (requires Docker; set `VAHAN_API_KEY` first). Or run `docker run -d -p 27017:27017 mongo:latest` then start the backend **without** setting `MONGODB_URI`.

### If the backend is "stuck" at startup (with MongoDB / Atlas)

The app connects to MongoDB at startup. If the URI points to Atlas (or any remote DB) and the connection is slow or unreachable, startup can hang. We use a **10-second** connection timeout so it fails fast instead of hanging for 30+ seconds. Fix by:

- Checking your `MONGODB_URI` (and Atlas network / allow list).
- Or **running without remote MongoDB**: use a local MongoDB and the default URI:
  - Start local Mongo: `docker run -d -p 27017:27017 mongo:latest`
  - Do **not** set `MONGODB_URI` (backend will use `mongodb://localhost:27017/rcview`).
  - Then start the backend as usual. Dev Login and vehicle search work; Admin (users/config) uses the local DB.

### If you see "Request timed out" or "Cannot reach the backend"

- **Start the backend first.** The frontend proxies `/api` to **http://localhost:8081**. If nothing is running there, requests will time out or fail.
- In a terminal: `cd backend && export DEV_MODE=true && export MONGODB_URI='...' && mvn spring-boot:run` (or `./run.sh` with the same env vars). Wait until you see "Started RcViewApplication".
- If your backend runs on another port (e.g. 8082), start the frontend with: `VITE_BACKEND_PORT=8082 npm run dev` so the proxy targets the correct port.

---

## Quick start (testing without Google SSO or MongoDB)

Use **Dev Mode** so you can run with only the **Vahan API key** (no Google OAuth, no audit logging, in-memory cache). **Admin flow is enabled in dev mode**: the Dev Login user has the ADMIN role, so you can open the Admin page; MongoDB is required for Admin API (user list, config save/load).

### 1. Where to set the API key and options

**Backend** – set environment variables **or** edit `backend/src/main/resources/application.yml`:

| What | Where | Example |
|------|--------|--------|
| **Vahan API key** (required for search) | Env: `VAHAN_API_KEY` or in `application.yml` under `vahan.api.api-key` | (your key) |
| **Dev mode** (bypass SSO + DB logging) | Env: `DEV_MODE=true` or in `application.yml`: `app.dev-mode: true` | `export DEV_MODE=true` |
| MongoDB (optional in dev mode for cache; app may still need it to start) | Env: `MONGODB_URI` or `application.yml` → `spring.data.mongodb.uri` | Local: `mongodb://localhost:27017/rcview`; Atlas: see MongoDB Atlas dashboard for your connection string |

**Minimal for dev:** set only **VAHAN_API_KEY** and **DEV_MODE=true**.  
In `application.yml` you can set:

```yaml
# In backend/src/main/resources/application.yml
vahan:
  api:
    api-key: YOUR_VAHAN_API_KEY

app:
  dev-mode: true
```

Or use env vars (no file change):

```bash
export VAHAN_API_KEY=your-vahan-api-key
export DEV_MODE=true
```

**Important:** Set `VAHAN_API_KEY` in the **same terminal** and **before** running `mvn spring-boot:run`. If you add or change the key later, you must **restart** the backend for it to take effect.

### 2. Start the backend

**For Dev Login to work**, the backend must be started with `DEV_MODE=true` in the environment. **For Admin page** (users + config), set **MONGODB_URI** so the backend can reach MongoDB (local or Atlas).

```bash
cd backend
export DEV_MODE=true
export MONGODB_URI='mongodb://localhost:27017/rcview'   # or your Atlas URI with /rcview database
mvn spring-boot:run
```

If you get **SSL/certificate errors** (`PKIX path building failed` / `unable to find valid certification path`) — common on corporate networks — use the script that relaxes SSL checks for Maven:

```bash
cd backend
chmod +x run.sh
./run.sh
```

Or set the same options and run Maven yourself:

```bash
cd backend
export MAVEN_OPTS="-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true -Dhttps.protocols=TLSv1.2"
mvn spring-boot:run
```

**If you still get the same certificate error:** the JVM is rejecting the HTTPS connection to Maven Central (common on corporate networks). See **`backend/SSL_TROUBLESHOOTING.md`** for fixing it (add your corporate CA to the JDK truststore, or build elsewhere and copy the Maven repo).

- Backend runs at **http://localhost:8081** (default; use `PORT=8080` to use 8080)
- API base path: **/api** (e.g. health: http://localhost:8081/api/actuator/health if you add actuator)

If you see MongoDB connection errors and you only want to test: keep **DEV_MODE=true**; the app still needs MongoDB for Spring Data to start unless you exclude it. Easiest is to run MongoDB locally (e.g. Docker: `docker run -d -p 27017:27017 mongo:latest`).

**If you see "Port … already in use":** The default is 8081. To free it, run: `kill $(lsof -t -i :8081) 2>/dev/null` (or `kill -9 $(lsof -t -i :8081)` if it doesn’t exit), then start the backend again. Or use another port: `PORT=8082 mvn spring-boot:run` and `VITE_BACKEND_PORT=8082` when starting the frontend.

### 3. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

- Frontend runs at **http://localhost:3000**
- Vite proxies `/api` to `http://localhost:8081`, so the app talks to your backend.

### 4. Log in (dev mode)

1. Open **http://localhost:3000**
2. You’ll be sent to the login page.
3. Click **“Dev Login (no SSO, no DB)”**.
4. You’re in as a **dev user with ADMIN role**. You can search by registration number and use the **Admin** page (sidebar icon).

**Admin in dev mode:** The Admin flow is enabled: the dev user has the ADMIN role, so the **Admin** link appears in the sidebar and you can open it. User list and app config are stored in MongoDB, so set **MONGODB_URI** (e.g. Atlas or local) and ensure the backend can connect; otherwise the Admin page will load but user list/config API calls may fail.

### 5. How to test Admin in dev mode

1. Start backend with **DEV_MODE=true** and **MONGODB_URI** (Atlas or local MongoDB, e.g. `docker run -d -p 27017:27017 mongo:latest`).
2. Start frontend; open **http://localhost:3000** and click **Dev Login (no SSO, no DB)**.
3. In the sidebar, click the **admin_panel_settings** (Admin) icon. You should see the Admin page.
4. **Users tab:** List users (from MongoDB), add a user (email + name), or change role/SSO. The dev user (`dev@test.com`) appears in the list if persisted; in dev mode the JWT still grants ADMIN so the UI and APIs work.
5. **Config tab:** View or edit app config (cache TTL, rate limits, etc.) and save. Changes are stored in MongoDB.
6. **Dashboard:** Use the vehicle search by registration number to confirm the rest of the app works.

---

## Full setup (with Google SSO and MongoDB)

When **not** using dev mode you need:

1. **MongoDB** running (e.g. `mongodb://localhost:27017/rcview`).
2. **Google OAuth**: create a client in Google Cloud Console, set redirect URI to  
   `http://localhost:8081/api/login/oauth2/code/google`, then set:
   - `GOOGLE_CLIENT_ID`
   - `GOOGLE_CLIENT_SECRET`
3. **VAHAN_API_KEY** (same as above).
4. **JWT_SECRET** (min 32 characters).
5. **FRONTEND_URL** (e.g. `http://localhost:3000`).

Run backend with:

```bash
# MongoDB: use local URI or Atlas (include database name: .../rcview?...)
export MONGODB_URI="<your-atlas-connection-string>"   # get from MongoDB Atlas → Connect → Drivers
export GOOGLE_CLIENT_ID=...
export GOOGLE_CLIENT_SECRET=...
export VAHAN_API_KEY=...
export JWT_SECRET=your-32-char-secret
export FRONTEND_URL=http://localhost:3000
cd backend && mvn spring-boot:run
```

Then run the frontend as above. Use **“Sign in with Google”** to log in.

---

## Summary: where to enter the API key

- **Vahan API key**: in **backend** only:
  - **Option A:** Environment variable **`VAHAN_API_KEY`** when running `mvn spring-boot:run`.
  - **Option B:** In **`backend/src/main/resources/application.yml`**, set:
    ```yaml
    vahan:
      api:
        api-key: YOUR_VAHAN_API_KEY_HERE
    ```
- No API key is required in the frontend; all external API calls go through the backend.
