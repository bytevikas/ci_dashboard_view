# How to run the application

## Quick start (testing without Google SSO or MongoDB)

Use **Dev Mode** so you can run with only the **Vahan API key** (no Google OAuth, no audit logging, in-memory cache).

### 1. Where to set the API key and options

**Backend** – set environment variables **or** edit `backend/src/main/resources/application.yml`:

| What | Where | Example |
|------|--------|--------|
| **Vahan API key** (required for search) | Env: `VAHAN_API_KEY` or in `application.yml` under `vahan.api.api-key` | (your key) |
| **Dev mode** (bypass SSO + DB logging) | Env: `DEV_MODE=true` or in `application.yml`: `app.dev-mode: true` | `export DEV_MODE=true` |
| MongoDB (optional in dev mode for cache; app may still need it to start) | Env: `MONGODB_URI` or `application.yml` → `spring.data.mongodb.uri` | `mongodb://localhost:27017/rcview` |

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

### 2. Start the backend

```bash
cd backend
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

- Backend runs at **http://localhost:8080**
- API base path: **/api** (e.g. health: http://localhost:8080/api/actuator/health if you add actuator)

If you see MongoDB connection errors and you only want to test: keep **DEV_MODE=true**; the app still needs MongoDB for Spring Data to start unless you exclude it. Easiest is to run MongoDB locally (e.g. Docker: `docker run -d -p 27017:27017 mongo:latest`).

### 3. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

- Frontend runs at **http://localhost:3000**
- Vite proxies `/api` to `http://localhost:8080`, so the app talks to your backend.

### 4. Log in (dev mode)

1. Open **http://localhost:3000**
2. You’ll be sent to the login page.
3. Click **“Dev Login (no SSO, no DB)”**.
4. You’re in as a dev user (admin). You can search by registration number and use the Admin page.

**Admin in dev mode:** User list and app config are stored in MongoDB. Start MongoDB (e.g. `docker run -d -p 27017:27017 mongo:latest`) so Admin user management and config save/load work.

---

## Full setup (with Google SSO and MongoDB)

When **not** using dev mode you need:

1. **MongoDB** running (e.g. `mongodb://localhost:27017/rcview`).
2. **Google OAuth**: create a client in Google Cloud Console, set redirect URI to  
   `http://localhost:8080/api/login/oauth2/code/google`, then set:
   - `GOOGLE_CLIENT_ID`
   - `GOOGLE_CLIENT_SECRET`
3. **VAHAN_API_KEY** (same as above).
4. **JWT_SECRET** (min 32 characters).
5. **FRONTEND_URL** (e.g. `http://localhost:3000`).

Run backend with:

```bash
export MONGODB_URI=mongodb://localhost:27017/rcview
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
