$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "=== Alice's Office Day Demo ===" -ForegroundColor Cyan
Write-Host ""

$base = "http://127.0.0.1:8000"

# 1. Login as Alice
Write-Host "[Step 1] Alice logs into the system..." -ForegroundColor Yellow
Set-Content -NoNewline -Path body.json -Value '{"email":"alice@acme.com"}'
$login = (curl.exe -s -X POST "$base/auth/login" -H "Content-Type: application/json" --data-binary "@body.json" | ConvertFrom-Json)
if (-not $login.accessToken) { throw "Login failed" }
$token = $login.accessToken
Write-Host "   [OK] Login successful" -ForegroundColor Green
Write-Host ""

# 2. Alice arrives at office - TAP IN
Write-Host "[9:00 AM] Alice arrives and taps IN at building door..." -ForegroundColor Yellow
$entryBody = @{ 
    gateId = 'BLD_ACME'
    readerNonce = 'MORNING_ENTRY_001'
    deviceId = 'ALICE_PHONE'
} | ConvertTo-Json -Compress
Set-Content -NoNewline -Path tokenreq.json -Value $entryBody

$entry = (curl.exe -s -X POST "$base/qr/token" -H "Content-Type: application/json" -H "Authorization: Bearer $token" --data-binary "@tokenreq.json" | ConvertFrom-Json)
if (-not $entry.token) { throw "Token request failed" }
Write-Host "   [OK] Entry token obtained" -ForegroundColor Green

# Verify ENTRY
$verifyEntry = @{
    readerId = 'FRONT_DOOR_READER'
    gateId = 'BLD_ACME'
    token = $entry.token
    doorOpened = $true
    direction = 'ENTRY'
} | ConvertTo-Json -Compress
Set-Content -NoNewline -Path verify.json -Value $verifyEntry

$result1 = (curl.exe -s -X POST "$base/access/verify" -H "Content-Type: application/json" --data-binary "@verify.json" | ConvertFrom-Json)
if ($result1.decision -ne "ALLOW") { throw "Entry denied" }

Write-Host "   [OK] Door opened - Alice entered building" -ForegroundColor Green
if ($result1.timeTracking) {
    Write-Host "   >> Time tracking started!" -ForegroundColor Cyan
    Write-Host "   >> Session ID: $($result1.timeTracking.sessionId)" -ForegroundColor Cyan
}
Write-Host ""
Write-Host "   *** CHECK THE TIME TAB NOW ***" -ForegroundColor Yellow -BackgroundColor DarkBlue
Write-Host "   Status should show: INSIDE BUILDING" -ForegroundColor Yellow
Write-Host ""

# 3. Alice works all day (simulated by waiting)
Write-Host "[...Alice works all day...]" -ForegroundColor Gray
Write-Host "   In real life: 9 hours would pass" -ForegroundColor Gray
Write-Host "   In this demo: waiting 15 seconds..." -ForegroundColor Gray
Write-Host ""

# Show live status updates
for ($i = 1; $i -le 15; $i++) {
    Start-Sleep -Seconds 1
    Write-Host "   Time in office: $i seconds..." -ForegroundColor Gray
}

Write-Host ""

# 4. Check current session before leaving
Write-Host "[Checking current session...]" -ForegroundColor Yellow
$current = (curl.exe -s -X GET "$base/time/current" -H "Authorization: Bearer $token" | ConvertFrom-Json)
if ($current.activeSession) {
    Write-Host "   Active session confirmed:" -ForegroundColor Green
    Write-Host "   Entry Time: $($current.activeSession.entryTime)" -ForegroundColor Cyan
    Write-Host "   Current Duration: $($current.activeSession.currentDurationFormatted)" -ForegroundColor Cyan
}
Write-Host ""

# 5. Alice leaves office - TAP OUT
Write-Host "[6:00 PM] Alice leaves and taps OUT at building door..." -ForegroundColor Yellow
$exitBody = @{
    gateId = 'BLD_ACME'
    readerNonce = 'EVENING_EXIT_001'
    deviceId = 'ALICE_PHONE'
} | ConvertTo-Json -Compress
Set-Content -NoNewline -Path tokenreq.json -Value $exitBody

$exit = (curl.exe -s -X POST "$base/qr/token" -H "Content-Type: application/json" -H "Authorization: Bearer $token" --data-binary "@tokenreq.json" | ConvertFrom-Json)
if (-not $exit.token) { throw "Exit token request failed" }
Write-Host "   [OK] Exit token obtained" -ForegroundColor Green

# Verify EXIT
$verifyExit = @{
    readerId = 'FRONT_DOOR_READER'
    gateId = 'BLD_ACME'
    token = $exit.token
    doorOpened = $true
    direction = 'EXIT'
} | ConvertTo-Json -Compress
Set-Content -NoNewline -Path verify.json -Value $verifyExit

$result2 = (curl.exe -s -X POST "$base/access/verify" -H "Content-Type: application/json" --data-binary "@verify.json" | ConvertFrom-Json)
if ($result2.decision -ne "ALLOW") { throw "Exit denied" }

Write-Host "   [OK] Door opened - Alice exited building" -ForegroundColor Green
Write-Host ""

# 6. Show results
Write-Host "========================================" -ForegroundColor Green
Write-Host "   WORKDAY COMPLETED!" -ForegroundColor Green -BackgroundColor Black
Write-Host "========================================" -ForegroundColor Green
if ($result2.timeTracking) {
    Write-Host ""
    Write-Host "   Entry Time: $($result2.timeTracking.entryTime.Substring(11,8))" -ForegroundColor Cyan
    Write-Host "   Exit Time:  $($result2.timeTracking.exitTime.Substring(11,8))" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "   >>> TOTAL TIME IN OFFICE: $($result2.timeTracking.durationFormatted) <<<" -ForegroundColor Yellow -BackgroundColor DarkGreen
    Write-Host ""
    Write-Host "   (In real scenario: would be 9h 0m 0s)" -ForegroundColor Gray
}
Write-Host ""

# 7. Show summary
Write-Host "[Fetching time summary...]" -ForegroundColor Yellow
$summary = (curl.exe -s -X GET "$base/time/summary" -H "Authorization: Bearer $token" | ConvertFrom-Json).summary
Write-Host ""
Write-Host "Today's Statistics:" -ForegroundColor Cyan
Write-Host "   Sessions: $($summary.todaySessions)" -ForegroundColor White
Write-Host "   Total Time: $($summary.todayTimeFormatted)" -ForegroundColor White
Write-Host ""
Write-Host "All Time Statistics:" -ForegroundColor Cyan
Write-Host "   Total Sessions: $($summary.totalSessions)" -ForegroundColor White
Write-Host "   Total Time: $($summary.totalTimeFormatted)" -ForegroundColor White
Write-Host "   Average Time: $($summary.averageTimeFormatted)" -ForegroundColor White
Write-Host ""

Write-Host "*** CHECK THE TIME TAB IN THE APP ***" -ForegroundColor Yellow -BackgroundColor DarkBlue
Write-Host "Status should now show: OUTSIDE BUILDING" -ForegroundColor Yellow
Write-Host "Statistics should be updated with the new session!" -ForegroundColor Yellow
Write-Host ""

