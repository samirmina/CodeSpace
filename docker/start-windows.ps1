# Chronos Docker Compose Startup Script
# Run this with PowerShell to start PostgreSQL + web app containers
# Place in Windows Startup folder: shell:startup

$ErrorActionPreference = "Stop"

$projectRoot = "C:\CodeSpace"  # Update this to your actual project path
$dockerDir = Join-Path $projectRoot "docker"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Chronos - Starting Docker Compose" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Check if Docker is running
try {
    docker info > $null 2>&1
} catch {
    Write-Host "ERROR: Docker is not running. Please start Docker Desktop first." -ForegroundColor Red
    Write-Host "Then re-run this script." -ForegroundColor Red
    exit 1
}

# Navigate to docker directory and start containers
Set-Location $dockerDir

Write-Host "Pulling latest images..." -ForegroundColor Yellow
docker compose pull

Write-Host "Starting PostgreSQL and Chronos web app..." -ForegroundColor Yellow
docker compose up -d

Write-Host "Waiting for PostgreSQL to be ready..." -ForegroundColor Yellow
$maxAttempts = 30
$attempt = 0
while ($attempt -lt $maxAttempts) {
    $result = docker compose exec -T postgres pg_isready -U chronos -d chronos 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "PostgreSQL is ready!" -ForegroundColor Green
        break
    }
    $attempt++
    Start-Sleep -Seconds 2
}

if ($attempt -eq $maxAttempts) {
    Write-Host "WARNING: PostgreSQL did not become ready in time." -ForegroundColor Yellow
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Chronos is running!" -ForegroundColor Green
Write-Host "  Web app:  http://localhost:8080" -ForegroundColor Green
Write-Host "  PostgreSQL: localhost:5432" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
