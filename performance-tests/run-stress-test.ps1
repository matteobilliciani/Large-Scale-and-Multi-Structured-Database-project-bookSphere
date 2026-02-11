# Script per eseguire lo Stress Test
# Performance Test - BookSphere

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  BookSphere - Stress Test" -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Test Configuration:" -ForegroundColor Green
Write-Host "  - Load: 10 to 200 users (progressive)" -ForegroundColor White
Write-Host "  - Increment: +10 users every 30 seconds" -ForegroundColor White
Write-Host "  - Max Duration: 15 minutes" -ForegroundColor White
Write-Host "  - Goal: Find system breaking point" -ForegroundColor White
Write-Host ""

Write-Host "WARNING: This test will stress your system!" -ForegroundColor Red
Write-Host "Make sure MongoDB and Neo4j are running properly." -ForegroundColor Yellow
Write-Host ""

$confirmation = Read-Host "Do you want to continue? (y/n)"
if ($confirmation -ne 'y') {
    Write-Host "Test cancelled." -ForegroundColor Yellow
    exit 0
}

# Verifica che l'app sia in esecuzione
Write-Host ""
Write-Host "Checking if BookSphere is running..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/books?page=0&size=1" -Method GET -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✓ BookSphere is running!" -ForegroundColor Green
} catch {
    Write-Host "✗ ERROR: BookSphere is not running!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Starting Stress Test..." -ForegroundColor Yellow
Write-Host "This will take approximately 15 minutes." -ForegroundColor White
Write-Host ""

# Esegui il test
mvn gatling:test -Dgatling.simulationClass=it.unipi.bookSphere.performance.StressTest

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host "  Stress Test Completed!" -ForegroundColor Green
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host ""
    
    $reportDir = Get-ChildItem -Path "target/gatling" -Directory | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($reportDir) {
        $reportPath = Join-Path $reportDir.FullName "index.html"
        Write-Host "Opening report..." -ForegroundColor Green
        Start-Process $reportPath
    }
}
