# Script per eseguire il Basic Load Test
# Performance Test - BookSphere

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  BookSphere - Basic Load Test" -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Test Configuration:" -ForegroundColor Green
Write-Host "  - Users: 50 concurrent users" -ForegroundColor White
Write-Host "  - Ramp-up: 30 seconds" -ForegroundColor White
Write-Host "  - Duration: 2 minutes" -ForegroundColor White
Write-Host "  - Scenarios: Public API, Auth, User Operations" -ForegroundColor White
Write-Host ""

# Verifica che l'app sia in esecuzione
Write-Host "Checking if BookSphere is running..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/books?page=0&size=1" -Method GET -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✓ BookSphere is running!" -ForegroundColor Green
} catch {
    Write-Host "✗ ERROR: BookSphere is not running!" -ForegroundColor Red
    Write-Host "Please start the application first:" -ForegroundColor Red
    Write-Host "  mvn spring-boot:run" -ForegroundColor White
    exit 1
}

Write-Host ""
Write-Host "Starting Gatling test..." -ForegroundColor Yellow
Write-Host ""

# Esegui il test
mvn gatling:test -Dgatling.simulationClass=it.unipi.bookSphere.performance.BasicLoadTest

# Verifica se il test è completato con successo
if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host "  Test Completed Successfully!" -ForegroundColor Green
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host ""
    
    # Trova l'ultimo report generato
    $reportDir = Get-ChildItem -Path "target/gatling" -Directory | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    
    if ($reportDir) {
        $reportPath = Join-Path $reportDir.FullName "index.html"
        Write-Host "Report Location: $reportPath" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Opening report in browser..." -ForegroundColor Green
        Start-Process $reportPath
    }
} else {
    Write-Host ""
    Write-Host "Test Failed!" -ForegroundColor Red
    Write-Host "Check the console output above for errors." -ForegroundColor Red
}
