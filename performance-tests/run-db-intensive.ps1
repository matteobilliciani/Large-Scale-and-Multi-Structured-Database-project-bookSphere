# Script per eseguire il Database Intensive Test
# Performance Test - BookSphere

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  BookSphere - Database Intensive Test" -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Test Configuration:" -ForegroundColor Green
Write-Host "  - Users: 30 concurrent users" -ForegroundColor White
Write-Host "  - Duration: 10 minutes" -ForegroundColor White
Write-Host "  - Focus: MongoDB & Neo4j heavy operations" -ForegroundColor White
Write-Host "  - Tests: Aggregations, Complex queries, Consistency" -ForegroundColor White
Write-Host ""

# Verifica che l'app sia in esecuzione
Write-Host "Checking if BookSphere is running..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/books?page=0&size=1" -Method GET -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✓ BookSphere is running!" -ForegroundColor Green
} catch {
    Write-Host "✗ ERROR: BookSphere is not running!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Starting Database Intensive Test..." -ForegroundColor Yellow
Write-Host ""

# Esegui il test
mvn gatling:test -Dgatling.simulationClass=it.unipi.bookSphere.performance.DatabaseIntensiveTest

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host "  Database Test Completed!" -ForegroundColor Green
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host ""
    
    $reportDir = Get-ChildItem -Path "target/gatling" -Directory | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($reportDir) {
        $reportPath = Join-Path $reportDir.FullName "index.html"
        Write-Host "Opening report..." -ForegroundColor Green
        Start-Process $reportPath
    }
}
