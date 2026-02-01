$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "=== Deploy to FREE Backend - Step 1: GitHub ===" -ForegroundColor Cyan
Write-Host ""

# Check if git is initialized
if (-not (Test-Path ".git")) {
    Write-Host "[ERROR] Not a git repository!" -ForegroundColor Red
    Write-Host "Run: git init" -ForegroundColor Yellow
    exit 1
}

Write-Host "[1] Checking git status..." -ForegroundColor Yellow
git status

Write-Host ""
Write-Host "[2] Do you have a GitHub remote set up?" -ForegroundColor Yellow
$remotes = git remote -v
if ($remotes) {
    Write-Host "Current remotes:" -ForegroundColor Green
    Write-Host $remotes
} else {
    Write-Host "No remotes configured." -ForegroundColor Red
    Write-Host ""
    Write-Host "To add GitHub remote:" -ForegroundColor Yellow
    Write-Host "  1. Create repo at: https://github.com/new" -ForegroundColor Cyan
    Write-Host "  2. Run: git remote add origin https://github.com/YOUR_USERNAME/oneaccess-app.git" -ForegroundColor Cyan
    Write-Host ""
    $continue = Read-Host "Have you set up GitHub remote? (y/n)"
    if ($continue -ne "y") {
        Write-Host "Set up GitHub first, then run this script again." -ForegroundColor Yellow
        exit 0
    }
}

Write-Host ""
Write-Host "[3] Adding all files..." -ForegroundColor Yellow
git add .

Write-Host ""
Write-Host "[4] Committing..." -ForegroundColor Yellow
$commitMsg = Read-Host "Commit message (or press Enter for 'Ready for deployment')"
if (-not $commitMsg) {
    $commitMsg = "Ready for deployment"
}
git commit -m $commitMsg

Write-Host ""
Write-Host "[5] Pushing to GitHub..." -ForegroundColor Yellow
git push origin main

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  SUCCESS! Code is on GitHub" -ForegroundColor Green -BackgroundColor Black
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Go to: https://render.com" -ForegroundColor White
Write-Host "2. Sign up with GitHub (FREE)" -ForegroundColor White
Write-Host "3. Create New Web Service" -ForegroundColor White
Write-Host "4. Connect your repository" -ForegroundColor White
Write-Host ""
Write-Host "See: DEPLOY_FREE_NOW.md for detailed instructions!" -ForegroundColor Yellow
Write-Host ""
