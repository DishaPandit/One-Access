$ErrorActionPreference = "Stop"

Write-Host "`n=== One-Access Time Tracking Test ===" -ForegroundColor Cyan

$base = "http://127.0.0.1:8000"

# 1. Login as Alice
Write-Host "`n1. Logging in as Alice..." -ForegroundColor Yellow
Set-Content -NoNewline -Path body.json -Value '{"email":"alice@acme.com"}'
$login = (curl.exe -s -X POST "$base/auth/login" -H "Content-Type: application/json" --data-binary "@body.json" | ConvertFrom-Json)
if (-not $login.accessToken) { throw "No accessToken" }
Write-Host "   ✓ Login successful" -ForegroundColor Green
$token = $login.accessToken

# 2. Check initial time summary
Write-Host "`n2. Checking initial time summary..." -ForegroundColor Yellow
$summary = (curl.exe -s -X GET "$base/time/summary" -H "Authorization: Bearer $token" | ConvertFrom-Json).summary
Write-Host "   Total Sessions: $($summary.totalSessions)" -ForegroundColor Cyan
Write-Host "   Today Sessions: $($summary.todaySessions)" -ForegroundColor Cyan
Write-Host "   Has Active Session: $($summary.hasActiveSession)" -ForegroundColor Cyan

# 3. Get QR token for building entry
Write-Host "`n3. Simulating ENTRY tap at BLD_ACME gate..." -ForegroundColor Yellow
$qrBody = @{ gateId='BLD_ACME'; readerNonce='ENTRY123456789'; deviceId='D1' } | ConvertTo-Json -Compress
Set-Content -NoNewline -Path tokenreq.json -Value $qrBody
$qr = (curl.exe -s -X POST "$base/qr/token" -H "Content-Type: application/json" -H "Authorization: Bearer $token" --data-binary "@tokenreq.json" | ConvertFrom-Json)
if (-not $qr.token) { throw "No token returned" }
Write-Host "   ✓ Token issued" -ForegroundColor Green

# 4. Verify with ENTRY direction
$verifyBody = @{ readerId='READER_1'; gateId='BLD_ACME'; token=$qr.token; doorOpened=$true; direction='ENTRY' } | ConvertTo-Json -Compress
Set-Content -NoNewline -Path verify.json -Value $verifyBody
$verify = (curl.exe -s -X POST "$base/access/verify" -H "Content-Type: application/json" --data-binary "@verify.json" | ConvertFrom-Json)
if ($verify.decision -ne "ALLOW") { throw "Expected ALLOW, got $($verify.decision) / $($verify.reason)" }
Write-Host "   ✓ Access granted (ENTRY)" -ForegroundColor Green

if ($verify.timeTracking) {
    Write-Host "   Time Tracking: $($verify.timeTracking.action)" -ForegroundColor Cyan
    Write-Host "   Session ID: $($verify.timeTracking.sessionId)" -ForegroundColor Cyan
}

# 5. Check current session
Write-Host "`n4. Checking current active session..." -ForegroundColor Yellow
$current = (curl.exe -s -X GET "$base/time/current" -H "Authorization: Bearer $token" | ConvertFrom-Json)
if ($current.activeSession) {
    Write-Host "   ✓ Active session found" -ForegroundColor Green
    Write-Host "   Entry Time: $($current.activeSession.entryTime)" -ForegroundColor Cyan
    Write-Host "   Current Duration: $($current.activeSession.currentDurationFormatted)" -ForegroundColor Cyan
    Write-Host "   Gate: $($current.activeSession.gateIdEntry)" -ForegroundColor Cyan
} else {
    Write-Host "   ✗ No active session (unexpected)" -ForegroundColor Red
}

# 6. Wait a few seconds to simulate time inside building
Write-Host "`n5. Waiting 5 seconds to simulate time spent inside..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# 7. Check session again
$current = (curl.exe -s -X GET "$base/time/current" -H "Authorization: Bearer $token" | ConvertFrom-Json)
if ($current.activeSession) {
    Write-Host "   Duration now: $($current.activeSession.currentDurationFormatted)" -ForegroundColor Cyan
}

# 8. Get QR token for EXIT
Write-Host "`n6. Simulating EXIT tap at BLD_ACME gate..." -ForegroundColor Yellow
$qrBody = @{ gateId='BLD_ACME'; readerNonce='EXIT123456789'; deviceId='D1' } | ConvertTo-Json -Compress
Set-Content -NoNewline -Path tokenreq.json -Value $qrBody
$qr = (curl.exe -s -X POST "$base/qr/token" -H "Content-Type: application/json" -H "Authorization: Bearer $token" --data-binary "@tokenreq.json" | ConvertFrom-Json)
if (-not $qr.token) { throw "No token returned" }
Write-Host "   ✓ Token issued" -ForegroundColor Green

# 9. Verify with EXIT direction
$verifyBody = @{ readerId='READER_1'; gateId='BLD_ACME'; token=$qr.token; doorOpened=$true; direction='EXIT' } | ConvertTo-Json -Compress
Set-Content -NoNewline -Path verify.json -Value $verifyBody
$verify = (curl.exe -s -X POST "$base/access/verify" -H "Content-Type: application/json" --data-binary "@verify.json" | ConvertFrom-Json)
if ($verify.decision -ne "ALLOW") { throw "Expected ALLOW, got $($verify.decision) / $($verify.reason)" }
Write-Host "   ✓ Access granted (EXIT)" -ForegroundColor Green

if ($verify.timeTracking) {
    Write-Host "   Time Tracking: $($verify.timeTracking.action)" -ForegroundColor Cyan
    Write-Host "   Session ID: $($verify.timeTracking.sessionId)" -ForegroundColor Cyan
    Write-Host "   Entry Time: $($verify.timeTracking.entryTime)" -ForegroundColor Cyan
    Write-Host "   Exit Time: $($verify.timeTracking.exitTime)" -ForegroundColor Cyan
    Write-Host "   Total Duration: $($verify.timeTracking.durationFormatted)" -ForegroundColor Green -BackgroundColor Black
}

# 10. Check current session (should be none)
Write-Host "`n7. Verifying session ended..." -ForegroundColor Yellow
$current = (curl.exe -s -X GET "$base/time/current" -H "Authorization: Bearer $token" | ConvertFrom-Json)
if ($current.activeSession) {
    Write-Host "   ✗ Active session still exists (unexpected)" -ForegroundColor Red
} else {
    Write-Host "   ✓ No active session (correct)" -ForegroundColor Green
}

# 11. Get session history
Write-Host "`n8. Fetching session history..." -ForegroundColor Yellow
$sessions = (curl.exe -s -X GET "$base/time/sessions?limit=5" -H "Authorization: Bearer $token" | ConvertFrom-Json).sessions
Write-Host "   Recent Sessions:" -ForegroundColor Cyan
foreach ($session in $sessions | Select-Object -First 3) {
    Write-Host "   - Entry: $($session.entryTime.Substring(11,8)) | Duration: $($session.durationFormatted) | Gate: $($session.gateIdEntry)" -ForegroundColor White
}

# 12. Final summary
Write-Host "`n9. Final time summary..." -ForegroundColor Yellow
$summary = (curl.exe -s -X GET "$base/time/summary" -H "Authorization: Bearer $token" | ConvertFrom-Json).summary
Write-Host "   Total Sessions: $($summary.totalSessions)" -ForegroundColor Cyan
Write-Host "   Total Time: $($summary.totalTimeFormatted)" -ForegroundColor Cyan
Write-Host "   Average Time: $($summary.averageTimeFormatted)" -ForegroundColor Cyan
Write-Host "   Today Sessions: $($summary.todaySessions)" -ForegroundColor Cyan
Write-Host "   Today Time: $($summary.todayTimeFormatted)" -ForegroundColor Cyan

Write-Host ""
Write-Host "=== All Time Tracking Tests Passed! ===" -ForegroundColor Green -BackgroundColor Black

