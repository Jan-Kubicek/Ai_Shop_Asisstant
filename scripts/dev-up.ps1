# Spustí vývojové závislosti (MongoDB) a vypíše další kroky.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

docker compose -f docker-compose.dev.yml up -d --wait

Write-Host ""
Write-Host "MongoDB běží na localhost:27017."
Write-Host "Mongo Express (volitelně): docker compose -f docker-compose.dev.yml --profile tools up -d mongo-express"
Write-Host ""
Write-Host "Backend:  cd backend; mvn spring-boot:run"
Write-Host "Frontend: cd frontend; npm install; npm run dev"
