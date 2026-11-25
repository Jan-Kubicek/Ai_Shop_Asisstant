#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

EMAIL="$1"
if [ -z "$EMAIL" ]; then
  EMAIL="admin@example.com"
fi

printf 'Heslo pro %s: ' "$EMAIL"
read -r -s PASSWORD
printf '\n'

docker compose exec -e "SEED_ADMIN_EMAIL=$EMAIL" -e "SEED_ADMIN_PASSWORD=$PASSWORD" backend java -jar /app/app.jar --seed-admin

unset PASSWORD
