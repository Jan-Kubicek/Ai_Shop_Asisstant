#!/usr/bin/env bash
# Načte .env do prostředí a spustí backend. Klíče nevypisuje.
set -a
source "$(dirname "$0")/../.env"
set +a
JAR="$(dirname "$0")/../backend/target/shop-assistant-0.1.0-SNAPSHOT.jar"
if [ ! -f "$JAR" ]; then
  echo "Backend není sestavený: cd backend && ./mvnw package -DskipTests" >&2
  exit 1
fi
echo "Starting backend (AI_PROVIDER_TEXT=$AI_PROVIDER_TEXT, AI_PROVIDER_IMAGE=$AI_PROVIDER_IMAGE)"
exec java -jar "$JAR" "$@"
