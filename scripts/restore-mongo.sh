#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

ARCHIVE="$1"
if [ -z "$ARCHIVE" ]; then
  printf 'Použití: %s <soubor-zalohy>\n' "$0"
  exit 1
fi

if [ ! -f "$ARCHIVE" ]; then
  printf 'Soubor zálohy neexistuje: %s\n' "$ARCHIVE"
  exit 1
fi

printf 'Obnovení přepíše data v MongoDB. Napište ANO pro pokračování: '
read -r ANSWER
if [ "$ANSWER" != "ANO" ]; then
  printf 'Zrušeno.\n'
  exit 1
fi

docker compose cp "$ARCHIVE" mongo:/tmp/restore.archive.gz
docker compose exec -T mongo sh -c 'mongorestore --archive=/tmp/restore.archive.gz --gzip --drop --username "$MONGO_INITDB_ROOT_USERNAME" --password "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin'
docker compose exec -T mongo rm -f /tmp/restore.archive.gz

printf 'Obnova dokončena.\n'
