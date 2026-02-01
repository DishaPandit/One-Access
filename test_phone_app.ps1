$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "=== Test Phone App with Production Backend ===" -ForegroundColor Cyan
Write-Host ""

$base = "https://oneaccess-backend.onrender.com"

# 1. Login
Write-Host "[1] Logging in as Alice..." -ForegroundColor Yellow
Set-Content -NoNewline -Path body.json -Value '{"email":"alice@acme.com"}'
$login = (curl.exe -s -X POST "$base/auth/login" -H "Content-Type: application/json" --data-binary "@body.json" | ConvertFrom-Json)
if (-not $login.accessToken) { 
    Write-Host "[ERROR] Login failed - backend might be sleeping, wait 30 sec..." -ForegroundColor Red
    Start-Sleep -Seconds 30
    $login = (curl.exe -s -X POST "$base/auth/login" -H "Content-Type: application/json" --data-binary "@body.json" | ConvertFrom-Json)
}
$token = $login.accessToken
Write-Host "   [OK] Login successful" -ForegroundColor Green

# 2. ENTRY
Write-Host ""
Write-Host "[2] Simulating ENTRY at building door..." -ForegroundColor Yellow
$entryBody = @{ gateId='BLD_ACME'; readerNonce='PHONE_ENTRY_001'; deviceId='PHONE' } | ConvertTo-Json -Compress
Set-Content -NoNewline -Path tokenreq.json -Value $entryBody
$entry = (curl.exe -s -X POST "$base/qr/token" -H "Content-Type: application/json" -H "Authorization: Bearer $token" --data-binary "@tokenreq.json" | ConvertFrom-Json)
$verifyEntry = @{ readerId='DOOR1'; gateId='BLD_ACME'; token=$entry.token; doorOpened=$true; direction='ENTRY' } | ConvertTo-Json -Compress
Set-Content -NoNewline -Path verify.json -Value $verifyEntry
$result1 = (curl.exe -s -X POST "$base/access/verify" -H "Content-Type: application/json" --data-binary "@verify.json" | ConvertFrom-Json)

if ($result1.decision -eq "ALLOW") {
    Write-Host "   [OK] Entry granted!" -ForegroundColor Green
    Write-Host "   >> Time tracking started" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "   *** CHECK YOUR PHONE NOW ***" -ForegroundColor Yellow -BackgroundColor DarkBlue
    Write-Host "   Go to Time tab - Status should show: INSIDE BUILDING" -ForegroundColor Yellow
    Write-Host ""
} else {
    Write-Host "   [ERROR] Entry denied: $($result1.reason)" -ForegroundColor Red
}

# 3. Wait
Write-Host "[3] Simulating 15 seconds inside building..." -ForegroundColor Gray
for ($i = 1; $i -le 15; $i++) {
    Start-Sleep -Seconds 1
    Write-Host "   Time inside: $i seconds..." -ForegroundColor Gray
}

# 4. EXIT
Write-Host ""
Write-Host "[4] Simulating EXIT at building door..." -ForegroundColor Yellow
$exitBody = @{ gateId='BLD_ACME'; readerNonce='PHONE_EXIT_001'; deviceId='PHONE' } | ConvertTo-Json -Compress
Set-Content -NoNewline -Path tokenreq.json -Value $exitBody
$exit = (curl.exe -s -X POST "$base/qr/token" -H "Content-Type: application/json" -H "Authorization: Bearer $token" --data-binary "@tokenreq.json" | ConvertFrom-Json)
$verifyExit = @{ readerId='DOOR1'; gateId='BLD_ACME'; token=$exit.token; doorOpened=$true; direction='EXIT' } | ConvertTo-Json -Compress
Set-Content -NoNewline -Path verify.json -Value $verifyExit
$result2 = (curl.exe -s -X POST "$base/access/verify" -H "Content-Type: application/json" --data-binary "@verify.json" | ConvertFrom-Json)

if ($result2.decision -eq "ALLOW" -and $result2.timeTracking) {
    Write-Host "   [OK] Exit granted!" -ForegroundColor Green
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  SESSION COMPLETED!" -ForegroundColor Green -BackgroundColor Black
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "   Total Time: $($result2.timeTracking.durationFormatted)" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "   *** CHECK YOUR PHONE NOW ***" -ForegroundColor Yellow -BackgroundColor DarkBlue
    Write-Host "   Go to Time tab - Status should show: OUTSIDE" -ForegroundColor Yellow
    Write-Host "   Statistics should be updated!" -ForegroundColor Yellow
    Write-Host ""
} else {
    Write-Host "   [ERROR] Exit issue" -ForegroundColor Red
}

Write-Host "Test complete! Check your phone's Time tab." -ForegroundColor Green
Write-Host ""
