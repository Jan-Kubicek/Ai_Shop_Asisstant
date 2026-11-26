#!/usr/bin/env bash
# Převede datový adresář vytvořený MongoDB 5+/7 do formátu MongoDB 4.4.
# Starý adresář ponechá jako zálohu se suffixem -backup-<timestamp>.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

DATA_DIR="${1:-data/mongo}"
DB="${DB:-shopassistant}"
MONGO7_IMAGE="${MONGO7_IMAGE:-mongo:7}"
MONGO44_IMAGE="${MONGO44_IMAGE:-mongo:4.4}"

set -a
source .env
set +a

ROOT_PASSWORD="${MONGO_INITDB_ROOT_PASSWORD:?Nastavte MONGO_INITDB_ROOT_PASSWORD v .env}"
APP_USER="${MONGO_APP_USER:-app}"
APP_PASSWORD="${MONGO_APP_PASSWORD:?Nastavte MONGO_APP_PASSWORD v .env}"

DATA="$(realpath "$DATA_DIR")"
INIT="$(realpath infra/mongo/init)"
DUMP="$(mktemp -d)"

wait_mongo() {
  local container="$1"; local shell="$2"; local i
  for i in $(seq 1 90); do
    if docker exec "$container" "$shell" --quiet -u root -p "$ROOT_PASSWORD" \
        --authenticationDatabase admin --eval "db.adminCommand('ping').ok" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "MongoDB v kontejneru $container se nestartovala." >&2
  return 1
}

cleanup() {
  docker rm -f mig-mongo7-src mig-mongo44-dst >/dev/null 2>&1 || true
  rm -rf "$DUMP"
}
trap cleanup EXIT

echo "1/4 Vytvářím dump z $DATA_DIR (MongoDB 7)..."
docker run -d --name mig-mongo7-src \
  -v "$DATA:/data/db" \
  -e MONGO_INITDB_ROOT_USERNAME=root \
  -e MONGO_INITDB_ROOT_PASSWORD="$ROOT_PASSWORD" \
  "$MONGO7_IMAGE" >/dev/null
wait_mongo mig-mongo7-src mongosh
docker exec mig-mongo7-src mongodump \
  --uri "mongodb://root:${ROOT_PASSWORD}@localhost:27017/?authSource=admin" \
  --db "$DB" --out /dump
docker cp mig-mongo7-src:/dump/. "$DUMP"
docker rm -f mig-mongo7-src >/dev/null

echo "2/4 Zálohuji původní adresář..."
BACKUP="${DATA}-backup-$(date +%Y%m%d-%H%M%S)"
mv "$DATA" "$BACKUP"
mkdir -p "$DATA"

echo "3/4 Restoruji do MongoDB 4.4..."
docker run -d --name mig-mongo44-dst \
  -v "$DATA:/data/db" \
  -v "$INIT:/docker-entrypoint-initdb.d:ro" \
  -v "$DUMP:/dump:ro" \
  -e MONGO_INITDB_ROOT_USERNAME=root \
  -e MONGO_INITDB_ROOT_PASSWORD="$ROOT_PASSWORD" \
  -e MONGO_INITDB_DATABASE="$DB" \
  -e MONGO_DATABASE="$DB" \
  -e MONGO_APP_USER="$APP_USER" \
  -e MONGO_APP_PASSWORD="$APP_PASSWORD" \
  "$MONGO44_IMAGE" >/dev/null
wait_mongo mig-mongo44-dst mongo
docker exec mig-mongo44-dst mongorestore \
  --uri "mongodb://root:${ROOT_PASSWORD}@localhost:27017/?authSource=admin" \
  --db "$DB" --noIndexRestore "/dump/$DB"

echo "4/4 hotovo."
echo "Záloha: $BACKUP"
echo "Nyní spusťte: docker compose up -d --build"
