# Fix: "PKIX path building failed" / "unable to find valid certification path"

This happens when your JDK doesn’t trust the certificate used by **https://repo.maven.apache.org** (common on corporate networks with SSL inspection).

**Note:** The project includes **local copies** of the Spring Boot parent POMs in `backend/local-maven-repo/`. That avoids downloading the parent from Maven Central. You may still see the certificate error when Maven downloads plugins or dependency JARs. Errors like *"No plugin found for prefix 'dependency'"* mean Maven couldn’t download the plugin (same SSL issue), not that you’re in the wrong directory. You must fix the certificate (Option 2) or copy a full `~/.m2/repository` from another machine (Option 3); there is no way to build or run `go-offline` on this machine until then.

## Option 1: Use the run script (try first)

```bash
cd backend
chmod +x run.sh
./run.sh
```

If it still fails, use Option 2 or 3.

---

## Option 2: Add the missing certificate to your JDK

Your network (or proxy) is using a **custom root CA**. You need to add that CA to Java’s truststore.

### Step 1 – Get the certificate

**From Chrome (macOS):**

1. Open **https://repo.maven.apache.org** in Chrome.
2. Click the lock icon → **Connection is secure** → **Certificate is valid**.
3. In the certificate window, select the **root** certificate at the top of the chain.
4. Drag that certificate to the Desktop (saved as `.cer`).

**Or** ask your IT for the **corporate root CA** file (e.g. `corporate-root.crt`).

### Step 2 – Find your JDK

```bash
# Example: Java 17
echo $JAVA_HOME
# e.g. /Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
```

If `JAVA_HOME` is not set, find it with:

```bash
/usr/libexec/java_home -v 17
```

### Step 3 – Import the certificate

Use the **same JDK** that Maven uses (run `mvn -version` to confirm).

```bash
# Backup first (optional)
sudo cp $JAVA_HOME/lib/security/cacerts $JAVA_HOME/lib/security/cacerts.backup

# Import (replace /path/to/cert.cer with your certificate path)
sudo keytool -importcert -alias maven-central-ca -file /path/to/cert.cer \
  -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit
```

Type `yes` when asked to trust the certificate.

### Step 4 – Clear Maven’s failure cache (important)

Maven caches failed transfers. After fixing the cert, clear those so it will retry:

```bash
# Remove cached failures and lock files under your local repo
find ~/.m2/repository -name "*.lastUpdated" -delete
find ~/.m2/repository -name "*.repositories" -delete
```

(On macOS/Linux. If you use a custom `localRepository` in `~/.m2/settings.xml`, run the same `find` against that path.)

### Step 5 – Run Maven again

```bash
cd backend
mvn spring-boot:run
```

---

## Option 3: Build on another network and copy the repo

If you can’t change the JDK truststore:

**Important:** Step 1 must run on a **different** machine or network (e.g. home Wi‑Fi) where Maven can reach Maven Central. You cannot run step 1 on the corporate machine that has the SSL error.

1. On a machine/network **where Maven Central works** (no SSL error), from the **project root**:
   ```bash
   cd /path/to/ci_api_view/backend
   mvn dependency:go-offline
   ```
   (If you see "No plugin found for prefix 'dependency'", you are in the wrong directory — you must be inside **backend**, where the `pom.xml` is.)

2. Copy the whole Maven local repo from that machine to the machine that has the SSL error:
   - From the machine where step 1 ran: copy `~/.m2/repository` (entire folder).
   - On the machine with SSL issues: put it at `~/.m2/repository` (create `~/.m2` if needed).

3. On the machine that had the SSL error, run:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   Maven will use the copied artifacts and avoid downloading from Maven Central.
