#!/usr/bin/env bash
# Run the backend with SSL workaround for "PKIX path building failed" (corporate proxy / untrusted cert).
# If it still fails, see SSL_TROUBLESHOOTING.md in this directory.
#
# ── Run modes ──────────────────────────────────────────────────────────
#
# 1) Dev — no SSO, no DB (pure in-memory, zero setup):
#    DEV_MODE=true ./run.sh
#
# 2) Dev + MongoDB — no SSO, but uses MongoDB for cache, audit, users:
#    DEV_MODE=true DEV_USE_DB=true MONGODB_URI='mongodb+srv://...' ./run.sh
#
# 3) Production — Google SSO + MongoDB (set all env vars):
#    GOOGLE_CLIENT_ID=... GOOGLE_CLIENT_SECRET=... MONGODB_URI=... ./run.sh

export MAVEN_OPTS="${MAVEN_OPTS:-} -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true -Dhttps.protocols=TLSv1.2"
exec mvn spring-boot:run "$@"
