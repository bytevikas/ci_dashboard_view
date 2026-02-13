#!/usr/bin/env bash
# Run the backend with SSL workaround for "PKIX path building failed" (corporate proxy / untrusted cert).
# Use this if: mvn spring-boot:run fails with certificate_unknown / unable to find valid certification path.
# If it still fails, see SSL_TROUBLESHOOTING.md in this directory.
#
 #Dev mode + Admin: set DEV_MODE=true and MONGODB_URI so Dev Login works and Admin page can load/save users and config.
# Example: MONGODB_URI='mongodb+srv://user:pass@cluster.../rcview?retryWrites=true&w=majority' ./run.sh

export MAVEN_OPTS="${MAVEN_OPTS:-} -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true -Dhttps.protocols=TLSv1.2"
exec mvn spring-boot:run "$@"
