param(
  [string] $DataDir = "data/mongo",
  [string] $Db = "shopassistant",
  [string] $RootPassword = "",
  [string] $AppUser = "app",
  [string] $AppPassword = "",
  [string] $Mongo7Image = "mongo:7",
  [string] $Mongo44Image = "mongo:4.4"
)

# Převede datový adresář vytvořený MongoDB 5+/7 do formátu MongoDB 4.4.
# Starý adresář ponechá jako zálohu se suffixem -backup-<timestamp>.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$vars = @{}
foreach ($line in Get-Content ".env") {
  if ($line -match '^\s*([^#][^=]*)=(.*)$') { $vars[$matches[1]] = $matches[2] }
}
if (-not $RootPassword) { $RootPassword = $vars['MONGO_INITDB_ROOT_PASSWORD'] }
if (-not $AppUser) { $AppUser = $vars['MONGO_APP_USER'] }
if (-not $AppPassword) { $AppPassword = $vars['MONGO_APP_PASSWORD'] }
if (-not $RootPassword -or -not $AppPassword) {
  throw "Nastavte MONGO_INITDB_ROOT_PASSWORD a MONGO_APP_PASSWORD v .env nebo parametry skriptu."
}

$data = (Resolve-Path $DataDir).Path
$init = (Resolve-Path "infra/mongo/init").Path
$dump = Join-Path ([IO.Path]::GetTempPath()) ("mongo-migrate-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force $dump | Out-Null

function Wait-Mongo([string] $container, [string] $password, [string] $shell) {
  for ($i = 0; $i -lt 90; $i++) {
    docker exec $container $shell --quiet -u root -p $password --authenticationDatabase admin `
      --eval "db.adminCommand('ping').ok" 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { return }
    Start-Sleep -Seconds 1
  }
  throw "MongoDB v kontejneru $container se nestartovala."
}

try {
  Write-Host "1/4 Vytvářím dump z $DataDir (MongoDB 7)..."
  docker run -d --name mig-mongo7-src `
    -v "${data}:/data/db" `
    -e MONGO_INITDB_ROOT_USERNAME=root `
    -e MONGO_INITDB_ROOT_PASSWORD=$RootPassword `
    $Mongo7Image | Out-Null
  Wait-Mongo "mig-mongo7-src" $RootPassword "mongosh"
  docker exec mig-mongo7-src mongodump `
    --uri "mongodb://root:${RootPassword}@localhost:27017/?authSource=admin" `
    --db $Db --out /dump
  docker cp mig-mongo7-src:/dump/. $dump
  docker rm -f mig-mongo7-src | Out-Null

  Write-Host "2/4 Zálohuji původní adresář..."
  $backup = "$data-backup-" + (Get-Date -Format yyyyMMdd-HHmmss)
  Rename-Item $data $backup
  New-Item -ItemType Directory -Force $data | Out-Null

  Write-Host "3/4 Restoruji do MongoDB 4.4..."
  docker run -d --name mig-mongo44-dst `
    -v "${data}:/data/db" `
    -v "${init}:/docker-entrypoint-initdb.d:ro" `
    -v "${dump}:/dump:ro" `
    -e MONGO_INITDB_ROOT_USERNAME=root `
    -e MONGO_INITDB_ROOT_PASSWORD=$RootPassword `
    -e MONGO_INITDB_DATABASE=$Db `
    -e MONGO_DATABASE=$Db `
    -e MONGO_APP_USER=$AppUser `
    -e MONGO_APP_PASSWORD=$AppPassword `
    $Mongo44Image | Out-Null
  Wait-Mongo "mig-mongo44-dst" $RootPassword "mongo"
  docker exec mig-mongo44-dst mongorestore `
    --uri "mongodb://root:${RootPassword}@localhost:27017/?authSource=admin" `
    --db $Db --noIndexRestore "/dump/$Db"

  Write-Host "4/4 hotovo."
  Write-Host "Záloha: $backup"
  Write-Host "Nyní spusťte: docker compose up -d --build"
} finally {
  docker rm -f mig-mongo7-src 2>$null | Out-Null
  docker rm -f mig-mongo44-dst 2>$null | Out-Null
  if (Test-Path $dump) { Remove-Item -Recurse -Force $dump }
}
