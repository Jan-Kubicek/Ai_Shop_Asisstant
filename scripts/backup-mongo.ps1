param(
  [string]$OutDir = "backups",
  [int]$RetentionDays = 14
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$file = Join-Path $OutDir "mongo-$stamp.archive.gz"
$container = "mongo"

docker compose exec -T $container sh -c 'mongodump --archive=/tmp/backup.gz --gzip --username "$MONGO_INITDB_ROOT_USERNAME" --password "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin'
docker compose cp ($container + ":/tmp/backup.gz") $file
docker compose exec -T $container rm -f /tmp/backup.gz

Get-ChildItem $OutDir -Filter "mongo-*.archive.gz" |
  Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$RetentionDays) } |
  Remove-Item -Force

Write-Host "Záloha vytvořena: $file"
