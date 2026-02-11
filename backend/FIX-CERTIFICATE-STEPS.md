
# Step-by-step: Fix the certificate so Maven works

Do these steps **in order** on your Mac. You need the certificate file and the JDK that Maven uses.

**“No plugin found for prefix 'spring-boot'”**, **“maven-clean-plugin (or any plugin) could not be resolved”**, or **“Could not transfer artifact ... (certificate_unknown) PKIX path building failed”** are all the **same certificate issue**: Maven can’t download plugins or dependencies from Maven Central. It **cannot be fixed by changing the project**; you must fix the certificate (steps below), copy a full `~/.m2/repository` from another machine, or use the **workaround at the bottom**: build the JAR on another machine and run it here with `java -jar` (no Maven needed on this machine).

---

## Step 1 – See which Java Maven uses

In Terminal:

```bash
mvn -version
```

Note the line that says something like `Java version: 17.x.x, vendor: ...`. That is the JDK we will add the cert to.

Find that JDK’s home (often one of these):

```bash
/usr/libexec/java_home -v 17
# or
echo $JAVA_HOME
```

If `JAVA_HOME` is empty, set it from the path above, e.g.:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

---

## Step 2 – Get the certificate file

**Option A – From Chrome (easiest)**

1. Open Chrome and go to: **https://repo.maven.apache.org**
2. If you get a warning, click **Advanced** → **Proceed to repo.maven.apache.org**.
3. Click the **lock icon** (or “Not secure” / “Connection is secure”) in the address bar.
4. Click **Certificate** (or “Connection is secure” → “Certificate is valid”).
5. In the certificate window you’ll see a **chain** (e.g. “Maven Central”, then “DigiCert …”, then a **root** at the top).
6. Select the **top-level (root)** certificate in the chain.
7. At the bottom, drag that certificate icon to your **Desktop**. It will be saved as something like `*.cer` (e.g. `DigiCert Global Root G2.cer`).

**Option B – From IT**

Ask your IT team for the **corporate root CA certificate** used for HTTPS inspection (e.g. `corporate-root.crt` or `*.cer`). Use that file in the next steps.

---

## Step 3 – Import the certificate into that JDK

In Terminal, replace `~/Desktop/YourCert.cer` with the real path to the file you got in Step 2 (e.g. `~/Desktop/DigiCert\ Global\ Root\ G2.cer`).

**3a. Backup the JDK truststore (optional but recommended):**

```bash
sudo cp "$JAVA_HOME/lib/security/cacerts" "$JAVA_HOME/lib/security/cacerts.backup"
```

**3b. Import the certificate:**

```bash
sudo keytool -importcert -alias maven-central-ca -file ~/Desktop/YourCert.cer -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit
```

- It will ask “Trust this certificate?” → type **yes** and press Enter.
- If it says “alias already exists”, use a different alias, e.g. `maven-ca-2`:

  ```bash
  sudo keytool -importcert -alias maven-ca-2 -file ~/Desktop/YourCert.cer -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit
  ```

---

## Step 4 – Clear Maven’s failure cache

Maven caches failed downloads. Clear that cache so it will try again:

```bash
find ~/.m2/repository -name "*.lastUpdated" -delete
find ~/.m2/repository -name "*.repositories" -delete
```

---

## Step 5 – Run Maven again

```bash
cd /path/to/ci_api_view/backend
mvn spring-boot:run
```

(Use the real path to your project.)

If it still fails with the same certificate error:

- You may need the **exact** root CA your company uses. Try getting that from IT and repeat Step 3 with that file.
- Or use **Option 3** in `SSL_TROUBLESHOOTING.md`: run `mvn dependency:go-offline` on another machine (e.g. home) and copy that machine’s `~/.m2/repository` to this Mac.

---

## If you don’t have admin (sudo) rights

You can’t change the JDK’s default `cacerts` without sudo. Options:

1. Ask IT to add the corporate root CA to the JDK truststore, or to give you a JDK that already trusts it.
2. Use another machine/network (e.g. home) to run `mvn dependency:go-offline` and then copy `~/.m2/repository` to this Mac (see Option 3 in `SSL_TROUBLESHOOTING.md`).
3. Try a Node.js backend instead (see main README): `npm` sometimes uses the system certificate store, which may already trust your corporate proxy.

---

## Other ways to get unblocked

- **Internal Maven/artifact repository**  
  Ask IT if your company has an internal Maven repository (e.g. Artifactory, Nexus) that mirrors Maven Central. If yes, they can give you a `settings.xml` or a repo URL. You put that in `~/.m2/settings.xml` so Maven uses the internal repo instead of Central. Internal repos often use certs your machine already trusts.

- **Build in the cloud (e.g. GitHub Actions)**  
  Push the project to GitHub, add a workflow that runs `mvn clean package`, and upload the JAR as an artifact. Then download the JAR from the Actions run and run it locally with `java -jar`. The build runs on GitHub’s network (no corporate proxy).

- **Try a Node.js backend instead**  
  This repo’s backend is Java; the frontend is React. If you add a small Node/Express backend that exposes the same API, you can use `npm install` and `node` to run it. On some setups, Node uses the system certificate store (e.g. macOS keychain), which may already trust your corporate proxy. If `npm install` works on this machine, you can develop and run without Maven.

- **Different network**  
  If you can use another network (e.g. home Wi‑Fi, mobile hotspot, or a VPN that doesn’t do SSL inspection), try running Maven there. If the certificate error goes away, the issue is the corporate proxy on the original network.

- **Ask IT**  
  Ask IT to either: (1) add the corporate root CA to the JDK truststore on your machine, or (2) provide a way to build/run the app (e.g. internal Maven mirror, approved build environment, or a pre-built JAR).

---

## Workaround: Build elsewhere, run here with `java -jar` (no Maven on this machine)

If you can’t fix the certificate and can’t copy the whole `.m2/repository`, you can **build on another machine** (e.g. home or a colleague’s laptop) where Maven works, then run the app on this machine with only Java:

**On a machine where Maven can reach Maven Central:**

```bash
cd /path/to/ci_api_view/backend
mvn clean package -DskipTests
```

This creates `target/ci-api-view-backend-1.0.0-SNAPSHOT.jar` (a runnable “fat” JAR).

Copy that JAR to your corporate Mac (e.g. via USB, shared drive, or email). Then on the corporate Mac you only need **Java 17** (no Maven):

```bash
java -jar ci-api-view-backend-1.0.0-SNAPSHOT.jar
```

Or with env vars (e.g. for Vahan API key and dev mode):

```bash
export VAHAN_API_KEY=your-key
export DEV_MODE=true
java -jar ci-api-view-backend-1.0.0-SNAPSHOT.jar
```

The app will run on port 8080 with context path `/api`. You still need MongoDB running (or use dev mode and in-memory cache as implemented). This way you never run Maven on the corporate machine.
