#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

docker compose -f docker-compose.dev.yml up -d --wait

printf '\nMongoDB běží na localhost:27017.\n'
printf 'Backend:  cd backend && mvn spring-boot:run\n'
printf 'Frontend: cd frontend && npm install && npm run dev\n'
