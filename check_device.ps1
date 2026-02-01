Write-Host "Checking for connected Android devices..." -ForegroundColor Cyan

# Check if adb is available
$adbPath = "adb"
try {
    $null = & $adbPath version 2>&1
} catch {
    Write-Host "ERROR: adb not found. Make sure Android SDK is installed." -ForegroundColor Red
    exit 1
}

Write-Host "`nConnected devices:" -ForegroundColor Yellow
& $adbPath devices -l

Write-Host "`n"
Write-Host "If you see 'unauthorized', check your phone for USB debugging prompt!" -ForegroundColor Green
Write-Host "If you see nothing, try these steps:" -ForegroundColor Yellow
Write-Host "  1. On phone: Settings > About Phone > Tap 'Build Number' 7 times" -ForegroundColor White
Write-Host "  2. On phone: Settings > Developer Options > Enable 'USB Debugging'" -ForegroundColor White
Write-Host "  3. Reconnect USB cable and allow the prompt" -ForegroundColor White
