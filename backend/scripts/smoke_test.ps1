$ErrorActionPreference = "Stop"

Write-Host "One-Access smoke test" -ForegroundColor Cyan

$base = "http://127.0.0.1:8000"

Set-Content -NoNewline -Path body.json -Value '{"email":"alice@acme.com"}'
$login = (curl.exe -s -X POST "$base/auth/login" -H "Content-Type: application/json" --data-binary "@body.json" | ConvertFrom-Json)
if (-not $login.accessToken) { throw "No accessToken" }
Write-Host "Login OK" -ForegroundColor Green

$qrBody = @{ gateId='MAIN_GATE'; readerNonce='0123456789ABCDEF'; deviceId='D1' } | ConvertTo-Json -Compress
Set-Content -NoNewline -Path tokenreq.json -Value $qrBody
$qr = (curl.exe -s -X POST "$base/qr/token" -H "Content-Type: application/json" -H "Authorization: Bearer $($login.accessToken)" --data-binary "@tokenreq.json" | ConvertFrom-Json)
if (-not $qr.token) { throw "No token returned" }
Write-Host "Issue QR token OK" -ForegroundColor Green

$verifyBody = @{ readerId='READER_1'; gateId='MAIN_GATE'; token=$qr.token; doorOpened=$true } | ConvertTo-Json -Compress
Set-Content -NoNewline -Path verify.json -Value $verifyBody
$verify = (curl.exe -s -X POST "$base/access/verify" -H "Content-Type: application/json" --data-binary "@verify.json" | ConvertFrom-Json)
if ($verify.decision -ne "ALLOW") { throw "Expected ALLOW, got $($verify.decision) / $($verify.reason)" }
Write-Host "Verify OK" -ForegroundColor Green

Write-Host "All OK." -ForegroundColor Green

