#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

OUT_DIR="$1"
if [ -z "$OUT_DIR" ]; then
  OUT_DIR="backups"
fi

RETENTION_DAYS="$2"
if [ -z "$RETENTION_DAYS" ]; then
  RETENTION_DAYS="14"
fi

mkdir -p "$OUT_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
FILE="$OUT_DIR/mongo-$STAMP.archive.gz"

docker compose exec -T mongo sh -c 'mongodump --archive=/tmp/backup.gz --gzip --username "$MONGO_INITDB_ROOT_USERNAME" --password "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin'
docker compose cp mongo:/tmp/backup.gz "$FILE"
docker compose exec -T mongo rm -f /tmp/backup.gz

find "$OUT_DIR" -name 'mongo-*.archive.gz' -mtime +"$RETENTION_DAYS" -delete

printf 'Záloha vytvořena: %s\n' "$FILE"
