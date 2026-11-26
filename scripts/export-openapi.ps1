$ErrorActionPreference = "Stop"
$spec = Invoke-RestMethod -Uri "http://localhost:8080/v3/api-docs"
$spec | ConvertTo-Json -Depth 100 | Set-Content -Path "docs/openapi.json" -Encoding UTF8
Write-Host "Saved docs/openapi.json"
