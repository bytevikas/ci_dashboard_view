#!/usr/bin/env bash
# Run the backend with LOCAL MongoDB (no Atlas). Use this when Atlas gives
# "Service Unavailable" / "Received fatal alert: internal_error" (e.g. on corporate network).
#
# Requires: Docker. Starts a Mongo container if one isn't already on port 27017.
# Set VAHAN_API_KEY and optionally DEV_MODE=true in the same shell before running.

set -e
MONGO_PORT=27017
CONTAINER_NAME="rcview-mongo"

# Start local MongoDB if not already running on port
if nc -z localhost $MONGO_PORT 2>/dev/null; then
  echo "MongoDB already running on port $MONGO_PORT."
else
  echo "Starting local MongoDB in Docker (port $MONGO_PORT)..."
  docker run -d --name "$CONTAINER_NAME" -p $MONGO_PORT:27017 mongo:latest 2>/dev/null || docker start "$CONTAINER_NAME" 2>/dev/null || true
  sleep 3
fi

# Do NOT set MONGODB_URI – backend will use mongodb://localhost:27017/rcview
unset MONGODB_URI
export DEV_MODE="${DEV_MODE:-true}"

echo "Starting backend (MongoDB: localhost:$MONGO_PORT)..."
exec mvn spring-boot:run "$@"
