# ================================================
# BookSphere - Performance Testing Script
# ================================================
# Run Gatling performance tests against the application
#
# PREREQUISITI:
# 1. Avviare MongoDB (replica set su porte 27017-27019)
# 2. Avviare Neo4j (porta 7687)
# 3. Avviare l'applicazione Spring Boot
#
# USO:
#   .\run-performance-tests.ps1 [test-name]
#
# TEST DISPONIBILI:
#   - basic           : BasicLoadTest (100 utenti, 60s)
#   - database        : DatabaseIntensiveTest (30 utenti, 2min)
#   - spike           : SpikeTest (10->100 utenti spike)
#   - stress          : StressTest (crescita progressiva fino a 30 utenti)
#   - endurance       : EnduranceTest (10 utenti per 2 minuti)
#   - all-light       : Esegue basic, database e spike in sequenza
#

param(
    [Parameter(Position=0)]
    [string]$TestName = "basic"
)

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "BookSphere Performance Testing" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Check if server is running
Write-Host "Checking if server is running on http://localhost:8080..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/books?page=0&size=1" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✓ Server is responding (Status: $($response.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "✗ Server is NOT running!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please start the Spring Boot application first:" -ForegroundColor Yellow
    Write-Host "  .\mvnw.cmd spring-boot:run" -ForegroundColor White
    Write-Host ""
    exit 1
}

Write-Host ""

# Test configurations
$tests = @{
    "basic" = @{
        "class" = "it.unipi.bookSphere.performance.BasicLoadTest"
        "description" = "BasicLoadTest - 100 users, 60s duration with text search"
    }
    "database" = @{
        "class" = "it.unipi.bookSphere.performance.DatabaseIntensiveTest"
        "description" = "DatabaseIntensiveTest - 30 users, 2min, intensive DB operations"
    }
    "spike" = @{
        "class" = "it.unipi.bookSphere.performance.SpikeTest"
        "description" = "SpikeTest - 10->100 users spike, 30s sustained"
    }
    "stress" = @{
        "class" = "it.unipi.bookSphere.performance.StressTest"
        "description" = "StressTest - Progressive load increase"
    }
    "endurance" = @{
        "class" = "it.unipi.bookSphere.performance.EnduranceTest"
        "description" = "EnduranceTest - 10 users for 2 minutes (default)"
    }
}

function Run-GatlingTest {
    param (
        [string]$TestClass,
        [string]$Description
    )
    
    Write-Host "Running: $Description" -ForegroundColor Cyan
    Write-Host "Class: $TestClass" -ForegroundColor Gray
    Write-Host ""
    
    $startTime = Get-Date
    
    & .\mvnw.cmd gatling:test "-Dgatling.simulationClass=$TestClass"
    
    $exitCode = $LASTEXITCODE
    $duration = (Get-Date) - $startTime
    
    Write-Host ""
    if ($exitCode -eq 0) {
        Write-Host "✓ Test completed successfully in $($duration.ToString('mm\:ss'))" -ForegroundColor Green
    } else {
        Write-Host "✗ Test failed with exit code $exitCode" -ForegroundColor Red
    }
    Write-Host ""
    
    return $exitCode
}

# Execute tests
if ($TestName -eq "all-light") {
    Write-Host "Running all LIGHT tests in sequence..." -ForegroundColor Cyan
    Write-Host ""
    
    $lightTests = @("basic", "database", "spike")
    $results = @{}
    
    foreach ($test in $lightTests) {
        $config = $tests[$test]
        $exitCode = Run-GatlingTest -TestClass $config.class -Description $config.description
        $results[$test] = $exitCode
        
        if ($test -ne $lightTests[-1]) {
            Write-Host "Waiting 10 seconds before next test..." -ForegroundColor Yellow
            Start-Sleep -Seconds 10
        }
    }
    
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Cyan
    Write-Host "Test Results Summary" -ForegroundColor Cyan
    Write-Host "=====================================" -ForegroundColor Cyan
    foreach ($test in $results.Keys) {
        $status = if ($results[$test] -eq 0) { "PASS" } else { "FAIL" }
        $color = if ($results[$test] -eq 0) { "Green" } else { "Red" }
        Write-Host "$test : $status" -ForegroundColor $color
    }
    
} elseif ($tests.ContainsKey($TestName)) {
    $config = $tests[$TestName]
    Run-GatlingTest -TestClass $config.class -Description $config.description
} else {
    Write-Host "Unknown test: $TestName" -ForegroundColor Red
    Write-Host ""
    Write-Host "Available tests:" -ForegroundColor Yellow
    foreach ($key in $tests.Keys) {
        Write-Host "  - $key : $($tests[$key].description)" -ForegroundColor White
    }
    Write-Host "  - all-light : Run basic, database, and spike tests" -ForegroundColor White
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "Reports are available in: target\gatling\" -ForegroundColor Cyan
Write-Host ""
