# Script per eseguire tutti i test in sequenza
# Performance Test Suite - BookSphere

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  BookSphere - Complete Performance Test Suite" -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "This script will run ALL performance tests:" -ForegroundColor Green
Write-Host "  1. Basic Load Test (2 min)" -ForegroundColor White
Write-Host "  2. Spike Test (3 min)" -ForegroundColor White
Write-Host "  3. Database Intensive Test (10 min)" -ForegroundColor White
Write-Host "  4. Stress Test (15 min)" -ForegroundColor White
Write-Host ""
Write-Host "Total estimated time: ~30 minutes" -ForegroundColor Yellow
Write-Host "Note: Endurance Test is excluded (takes 2 hours)" -ForegroundColor Gray
Write-Host ""

$confirmation = Read-Host "Do you want to continue? (y/n)"
if ($confirmation -ne 'y') {
    Write-Host "Test suite cancelled." -ForegroundColor Yellow
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
    Write-Host "Please start the application first:" -ForegroundColor Red
    Write-Host "  mvn spring-boot:run" -ForegroundColor White
    exit 1
}

$startTime = Get-Date
$results = @()

# Test 1: Basic Load Test
Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  [1/4] Running Basic Load Test..." -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Cyan
mvn gatling:test -Dgatling.simulationClass=it.unipi.bookSphere.performance.BasicLoadTest
$results += @{Name="Basic Load Test"; Success=($LASTEXITCODE -eq 0)}
Start-Sleep -Seconds 10

# Test 2: Spike Test
Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  [2/4] Running Spike Test..." -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Cyan
mvn gatling:test -Dgatling.simulationClass=it.unipi.bookSphere.performance.SpikeTest
$results += @{Name="Spike Test"; Success=($LASTEXITCODE -eq 0)}
Start-Sleep -Seconds 10

# Test 3: Database Intensive Test
Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  [3/4] Running Database Intensive Test..." -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Cyan
mvn gatling:test -Dgatling.simulationClass=it.unipi.bookSphere.performance.DatabaseIntensiveTest
$results += @{Name="Database Intensive Test"; Success=($LASTEXITCODE -eq 0)}
Start-Sleep -Seconds 10

# Test 4: Stress Test
Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  [4/4] Running Stress Test..." -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Cyan
mvn gatling:test -Dgatling.simulationClass=it.unipi.bookSphere.performance.StressTest
$results += @{Name="Stress Test"; Success=($LASTEXITCODE -eq 0)}

# Summary
$endTime = Get-Date
$duration = $endTime - $startTime

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Test Suite Completed!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Summary:" -ForegroundColor Yellow
Write-Host "--------" -ForegroundColor Yellow

foreach ($result in $results) {
    $status = if ($result.Success) { "✓ PASSED" } else { "✗ FAILED" }
    $color = if ($result.Success) { "Green" } else { "Red" }
    Write-Host "  $($result.Name): " -NoNewline
    Write-Host $status -ForegroundColor $color
}

Write-Host ""
Write-Host "Total Duration: $($duration.ToString('hh\:mm\:ss'))" -ForegroundColor Yellow
Write-Host ""
Write-Host "All reports are available in: target/gatling/" -ForegroundColor Cyan
Write-Host ""

# Apri la cartella dei report
$reportsPath = "target\gatling"
if (Test-Path $reportsPath) {
    Write-Host "Opening reports folder..." -ForegroundColor Green
    Start-Process $reportsPath
}
