param(
  [string]$Email = "admin@example.com"
)

# Vytvoří prvního admina. Backend musí implementovat parametr --seed-admin
# jako ApplicationRunner, který se spustí pouze při prázdné kolekci users.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$secure = Read-Host "Heslo pro $Email" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
  $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
  docker compose exec -e "SEED_ADMIN_EMAIL=$Email" -e "SEED_ADMIN_PASSWORD=$password" backend java -jar /app/app.jar --seed-admin
} finally {
  [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
  Remove-Variable password -ErrorAction SilentlyContinue
}
