# OneAccess Icon Generator
# Generates all required Android app icons from SVG source

Write-Host "OneAccess Icon Generator" -ForegroundColor Cyan
Write-Host "========================" -ForegroundColor Cyan
Write-Host ""

$svgSource = "logo_simple.svg"
$androidRes = "..\android\app\src\main\res"

# Check if source SVG exists
if (-not (Test-Path $svgSource)) {
    Write-Host "ERROR: $svgSource not found!" -ForegroundColor Red
    Write-Host "Make sure you're in the 'design' folder" -ForegroundColor Yellow
    exit 1
}

# Check for ImageMagick
$magickCmd = Get-Command magick -ErrorAction SilentlyContinue
if (-not $magickCmd) {
    Write-Host "ImageMagick not found!" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Please use one of these methods instead:" -ForegroundColor White
    Write-Host "1. Android Studio: Right-click res > New > Image Asset" -ForegroundColor Green
    Write-Host "2. Online: https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html" -ForegroundColor Green
    Write-Host ""
    Write-Host "Or install ImageMagick:" -ForegroundColor White
    Write-Host "  winget install ImageMagick.ImageMagick" -ForegroundColor Cyan
    Write-Host ""
    exit 1
}

Write-Host "Found ImageMagick! Generating icons..." -ForegroundColor Green
Write-Host ""

# Icon sizes for different densities
$sizes = @{
    "mdpi"    = 48
    "hdpi"    = 72
    "xhdpi"   = 96
    "xxhdpi"  = 144
    "xxxhdpi" = 192
}

$iconTypes = @("ic_launcher", "ic_launcher_round")

foreach ($iconType in $iconTypes) {
    Write-Host "Generating $iconType..." -ForegroundColor Yellow
    
    foreach ($density in $sizes.Keys) {
        $size = $sizes[$density]
        $folder = Join-Path $androidRes "mipmap-$density"
        
        # Create folder if it doesn't exist
        if (-not (Test-Path $folder)) {
            New-Item -ItemType Directory -Path $folder -Force | Out-Null
        }
        
        $output = Join-Path $folder "$iconType.png"
        
        Write-Host "  $density (${size}x${size})..." -NoNewline
        
        # Generate PNG
        & magick convert $svgSource -resize "${size}x${size}" $output
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host " ✓" -ForegroundColor Green
        } else {
            Write-Host " ✗" -ForegroundColor Red
        }
    }
    Write-Host ""
}

Write-Host "✓ Icon generation complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Rebuild the app: cd ..\android && .\gradlew clean assembleDebug" -ForegroundColor White
Write-Host "2. Install on device" -ForegroundColor White
Write-Host "3. Check the home screen - your new icon should appear!" -ForegroundColor White
Write-Host ""
