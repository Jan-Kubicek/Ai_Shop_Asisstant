param(
  [Parameter(Mandatory = $true)][string]$Archive
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Test-Path $Archive)) {
  throw "Soubor zálohy neexistuje: $Archive"
}

$answer = Read-Host "Obnovení přepíše data v MongoDB. Napište ANO pro pokračování"
if ($answer -ne "ANO") {
  Write-Host "Zrušeno."
  exit 1
}

docker compose cp $Archive "mongo:/tmp/restore.archive.gz"
docker compose exec -T mongo sh -c 'mongorestore --archive=/tmp/restore.archive.gz --gzip --drop --username "$MONGO_INITDB_ROOT_USERNAME" --password "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin'
docker compose exec -T mongo rm -f /tmp/restore.archive.gz

Write-Host "Obnova dokončena."
