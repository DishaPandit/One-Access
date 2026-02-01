Write-Host "=== OneAccess Crash Log Collector ===" -ForegroundColor Cyan
Write-Host ""

# Find adb from common Android Studio locations
$possiblePaths = @(
    "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    "$env:PROGRAMFILES\Android\Android Studio\platform-tools\adb.exe",
    "${env:PROGRAMFILES(X86)}\Android\Android Studio\platform-tools\adb.exe",
    "C:\Android\sdk\platform-tools\adb.exe",
    "$env:USERPROFILE\AppData\Local\Android\Sdk\platform-tools\adb.exe"
)

$adb = $null
foreach ($path in $possiblePaths) {
    if (Test-Path $path) {
        $adb = $path
        Write-Host "Found adb: $adb" -ForegroundColor Green
        break
    }
}

if (-not $adb) {
    Write-Host "ERROR: Could not find adb.exe" -ForegroundColor Red
    Write-Host ""
    Write-Host "ALTERNATIVE: Use Logcat in Android Studio" -ForegroundColor Yellow
    Write-Host "1. In Android Studio, click View > Tool Windows > Logcat" -ForegroundColor White
    Write-Host "2. At top of Logcat, select your device from dropdown" -ForegroundColor White
    Write-Host "3. In the filter box, type: 'AndroidRuntime'" -ForegroundColor White
    Write-Host "4. Open app on phone and click Time tab" -ForegroundColor White
    Write-Host "5. Copy all RED text that appears" -ForegroundColor White
    Write-Host ""
    Write-Host "Or try wireless debugging:" -ForegroundColor Yellow
    Write-Host "1. On phone: Settings > Developer Options > Wireless Debugging" -ForegroundColor White
    Write-Host "2. Note the IP and port (e.g., 192.168.1.100:5555)" -ForegroundColor White
    Write-Host "3. In Android Studio: Pair using QR code or pairing code" -ForegroundColor White
    exit 1
}

Write-Host ""
Write-Host "Checking connected devices..." -ForegroundColor Yellow
$devices = & $adb devices
Write-Host $devices

if ($devices -match "unauthorized") {
    Write-Host ""
    Write-Host "DEVICE UNAUTHORIZED!" -ForegroundColor Red
    Write-Host "Check your phone for a USB debugging permission popup!" -ForegroundColor Yellow
    exit 1
}

if (-not ($devices -match "device$")) {
    Write-Host ""
    Write-Host "NO DEVICE DETECTED!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Enable USB Debugging:" -ForegroundColor Yellow
    Write-Host "1. On phone: Settings > About Phone" -ForegroundColor White
    Write-Host "2. Tap 'Build Number' 7 times (enables Developer Options)" -ForegroundColor White
    Write-Host "3. Go back to Settings > Developer Options" -ForegroundColor White
    Write-Host "4. Enable 'USB Debugging'" -ForegroundColor White
    Write-Host "5. Reconnect USB cable and allow the popup" -ForegroundColor White
    Write-Host ""
    Write-Host "If still not detected, try:" -ForegroundColor Yellow
    Write-Host "- Different USB cable" -ForegroundColor White
    Write-Host "- Different USB port on computer" -ForegroundColor White
    Write-Host "- Restart phone and computer" -ForegroundColor White
    exit 1
}

Write-Host ""
Write-Host "Device connected! Starting crash log capture..." -ForegroundColor Green
Write-Host "Now:" -ForegroundColor Yellow
Write-Host "1. Open OneAccess app on your phone" -ForegroundColor White
Write-Host "2. Click the Time tab (let it crash)" -ForegroundColor White
Write-Host "3. Watch below for crash details..." -ForegroundColor White
Write-Host ""
Write-Host "Waiting for crash... (Press Ctrl+C to stop)" -ForegroundColor Cyan
Write-Host "=" * 70

# Clear old logs and start capturing
& $adb logcat -c
& $adb logcat AndroidRuntime:E *:S
