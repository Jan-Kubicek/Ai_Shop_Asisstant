param(
  [string[]] $Arguments = @()
)

# Načte proměnné z lokálního .env (nikdy se necommituje) a spustí backend.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env"
if (-not (Test-Path $envFile)) {
  throw "Soubor .env neexistuje. Zkopírujte .env.example -> .env."
}

Get-Content $envFile | ForEach-Object {
  if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
    [Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
  }
}

$jar = Join-Path $root "backend\target\shop-assistant-0.1.0-SNAPSHOT.jar"
if (-not (Test-Path $jar)) {
  throw "Backend není sestavený. Spusťte: cd backend; .\mvnw.cmd package -DskipTests"
}

Write-Host "Starting backend (AI_PROVIDER_TEXT=$env:AI_PROVIDER_TEXT, AI_PROVIDER_IMAGE=$env:AI_PROVIDER_IMAGE)"
& java -jar $jar @Arguments
