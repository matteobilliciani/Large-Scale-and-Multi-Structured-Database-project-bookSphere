# Pre-Test Verification Script for clusterWSL Profile
# Verifica connessioni database prima di eseguire i performance test

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Database Connection Verification - clusterWSL" -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$allOk = $true

# 1. Verifica MongoDB Cluster (3 nodi)
Write-Host "[1/4] Checking MongoDB Cluster..." -ForegroundColor Yellow
$mongoNodes = @(27017, 27018, 27019)
$mongoOk = 0

foreach ($port in $mongoNodes) {
    try {
        $connection = New-Object System.Net.Sockets.TcpClient
        $connection.Connect("localhost", $port)
        $connection.Close()
        Write-Host "  ✓ MongoDB node on port $port is reachable" -ForegroundColor Green
        $mongoOk++
    } catch {
        Write-Host "  ✗ MongoDB node on port $port is NOT reachable" -ForegroundColor Red
        $allOk = $false
    }
}

if ($mongoOk -eq 3) {
    Write-Host "  ✓ MongoDB Cluster: All 3 nodes are UP!" -ForegroundColor Green
} elseif ($mongoOk -gt 0) {
    Write-Host "  ⚠ MongoDB Cluster: Only $mongoOk/3 nodes are UP" -ForegroundColor Yellow
} else {
    Write-Host "  ✗ MongoDB Cluster: NO nodes are reachable!" -ForegroundColor Red
}

Write-Host ""

# 2. Verifica Neo4j su WSL IP
Write-Host "[2/4] Checking Neo4j (WSL)..." -ForegroundColor Yellow

# Leggi l'IP dal file di configurazione
$configFile = "src\main\resources\application-clusterWSL.properties"
if (Test-Path $configFile) {
    $neo4jUri = Select-String -Path $configFile -Pattern "spring.neo4j.uri.*neo4j://([^:]+):(\d+)" | ForEach-Object { $_.Matches.Groups[1].Value }
    $neo4jPort = Select-String -Path $configFile -Pattern "spring.neo4j.uri.*neo4j://[^:]+:(\d+)" | ForEach-Object { $_.Matches.Groups[2].Value }
    
    if ($neo4jUri -and $neo4jPort) {
        Write-Host "  Testing connection to: $neo4jUri:$neo4jPort" -ForegroundColor Gray
        try {
            $connection = New-Object System.Net.Sockets.TcpClient
            $connection.Connect($neo4jUri, $neo4jPort)
            $connection.Close()
            Write-Host "  ✓ Neo4j is reachable at $neo4jUri:$neo4jPort" -ForegroundColor Green
        } catch {
            Write-Host "  ✗ Neo4j is NOT reachable at $neo4jUri:$neo4jPort" -ForegroundColor Red
            Write-Host "  Note: WSL IP changes on restart. Current IP from config: $neo4jUri" -ForegroundColor Yellow
            $allOk = $false
        }
    } else {
        Write-Host "  ⚠ Could not parse Neo4j URI from config file" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ✗ Config file not found: $configFile" -ForegroundColor Red
    $allOk = $false
}

Write-Host ""

# 3. Verifica che l'applicazione possa partire
Write-Host "[3/4] Checking if application can start..." -ForegroundColor Yellow
Write-Host "  Compiling project..." -ForegroundColor Gray

$compileOutput = mvn clean compile -q 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "  ✓ Project compiles successfully" -ForegroundColor Green
} else {
    Write-Host "  ✗ Compilation failed!" -ForegroundColor Red
    Write-Host "  Run 'mvn clean compile' to see detailed errors" -ForegroundColor Yellow
    $allOk = $false
}

Write-Host ""

# 4. Verifica Spring Profile
Write-Host "[4/4] Checking Spring Profile configuration..." -ForegroundColor Yellow
$appProperties = "src\main\resources\application.properties"
if (Test-Path $appProperties) {
    $profile = Select-String -Path $appProperties -Pattern "spring.profiles.active=(.+)" | ForEach-Object { $_.Matches.Groups[1].Value }
    if ($profile -eq "clusterWSL") {
        Write-Host "  ✓ Spring Profile is set to: clusterWSL" -ForegroundColor Green
    } else {
        Write-Host "  ⚠ Spring Profile is set to: $profile (expected: clusterWSL)" -ForegroundColor Yellow
        Write-Host "    The application will use clusterWSL profile settings anyway" -ForegroundColor Gray
    }
} else {
    Write-Host "  ⚠ application.properties not found" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan

if ($allOk) {
    Write-Host "  ✅ All checks passed! Ready for performance testing" -ForegroundColor Green
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "  1. Start the application: mvn spring-boot:run -Dspring-boot.run.profiles=clusterWSL" -ForegroundColor White
    Write-Host "  2. Run tests: cd performance-tests && .\run-basic-load.ps1" -ForegroundColor White
    exit 0
} else {
    Write-Host "  ❌ Some checks failed - Please fix issues before running tests" -ForegroundColor Red
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Common issues:" -ForegroundColor Yellow
    Write-Host "  • MongoDB Cluster not running: Start it with your cluster script" -ForegroundColor White
    Write-Host "  • Neo4j not running: Start Neo4j service in WSL" -ForegroundColor White
    Write-Host "  • WSL IP changed: Update spring.neo4j.uri in application-clusterWSL.properties" -ForegroundColor White
    Write-Host ""
    Write-Host "To get current WSL IP, run in WSL:" -ForegroundColor Yellow
    Write-Host "  ip addr show eth0 | grep 'inet ' | awk '{print `$2}' | cut -d/ -f1" -ForegroundColor White
    exit 1
}
