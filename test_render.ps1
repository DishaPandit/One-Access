$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "=== Test Render Backend ===" -ForegroundColor Cyan
Write-Host ""

$url = Read-Host "Enter your Render URL (e.g., https://oneaccess-backend.onrender.com)"

Write-Host ""
Write-Host "Testing login endpoint..." -ForegroundColor Yellow

$response = curl.exe -s -X POST "$url/auth/login" -H "Content-Type: application/json" -d '{"email":"alice@acme.com"}'

Write-Host ""
Write-Host "Response:" -ForegroundColor Cyan
Write-Host $response

if ($response -match "accessToken") {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  SUCCESS! Backend is working!" -ForegroundColor Green -BackgroundColor Black
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Your backend URL is: $url" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Next: Update Android app with this URL" -ForegroundColor Yellow
} else {
    Write-Host ""
    Write-Host "[ERROR] Backend not responding correctly" -ForegroundColor Red
    Write-Host "Check Render dashboard for errors" -ForegroundColor Yellow
}

Write-Host ""
