#!/usr/bin/env bash
# Exportuje OpenAPI spec z běžícího backendu (dev profil).
set -euo pipefail
curl -fsS http://localhost:8080/v3/api-docs \
  | python3 -m json.tool > docs/openapi.json
echo "Saved docs/openapi.json"
