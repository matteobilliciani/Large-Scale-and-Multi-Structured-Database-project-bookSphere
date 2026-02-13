# BookSphere Storytelling Tests
# Script per eseguire i test funzionali con narrativa

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "   📚 BookSphere - Storytelling Journey Tests" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Verifica che Newman sia installato
Write-Host "🔍 Verifico l'installazione di Newman..." -ForegroundColor Yellow
$newmanInstalled = Get-Command newman -ErrorAction SilentlyContinue

if (-not $newmanInstalled) {
    Write-Host "❌ Newman non è installato!" -ForegroundColor Red
    Write-Host "📦 Installo Newman globalmente..." -ForegroundColor Yellow
    npm install -g newman
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Errore nell'installazione di Newman" -ForegroundColor Red
        Write-Host "💡 Prova manualmente: npm install -g newman" -ForegroundColor Yellow
        exit 1
    }
    Write-Host "✅ Newman installato con successo!" -ForegroundColor Green
} else {
    Write-Host "✅ Newman è già installato" -ForegroundColor Green
}

Write-Host ""

# Verifica che l'applicazione sia in esecuzione
Write-Host "🔍 Verifico che l'applicazione sia in esecuzione su localhost:8080..." -ForegroundColor Yellow

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/analytics/rankings/trendingbooks" -Method GET -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✅ Applicazione in esecuzione!" -ForegroundColor Green
} catch {
    Write-Host "❌ L'applicazione non risponde su localhost:8080" -ForegroundColor Red
    Write-Host "" -ForegroundColor Red
    Write-Host "🚀 Per avviare l'applicazione:" -ForegroundColor Yellow
    Write-Host "   1. Apri un nuovo terminale PowerShell" -ForegroundColor White
    Write-Host "   2. Esegui: .\mvnw.cmd spring-boot:run" -ForegroundColor White
    Write-Host "   3. Attendi che si avvii completamente" -ForegroundColor White
    Write-Host "   4. Riesegui questo script" -ForegroundColor White
    Write-Host ""
    
    $startApp = Read-Host "Vuoi che provi ad avviare l'applicazione ora? (y/n)"
    if ($startApp -eq 'y' -or $startApp -eq 'Y') {
        Write-Host "🚀 Avvio l'applicazione in un nuovo terminale..." -ForegroundColor Yellow
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\..'; .\mvnw.cmd spring-boot:run"
        Write-Host "⏳ Attendo 30 secondi per l'avvio dell'applicazione..." -ForegroundColor Yellow
        Start-Sleep -Seconds 30
    } else {
        exit 1
    }
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "   🎭 Inizia la Storia..." -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Path alla collection
$collectionPath = Join-Path $PSScriptRoot "bookSphere_storytelling_tests.postman_collection.json"

if (-not (Test-Path $collectionPath)) {
    Write-Host "❌ Collection non trovata: $collectionPath" -ForegroundColor Red
    exit 1
}

# Esegui Newman con output verboso
Write-Host "📖 Esecuzione della collection con storytelling..." -ForegroundColor Yellow
Write-Host ""

newman run $collectionPath `
    --reporters cli `
    --reporter-cli-no-assertions `
    --reporter-cli-no-console `
    --color on `
    --timeout-request 10000 `
    --bail

$exitCode = $LASTEXITCODE

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan

if ($exitCode -eq 0) {
    Write-Host "   ✅ TUTTI I TEST SONO PASSATI!" -ForegroundColor Green
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "📊 Il viaggio è stato completato con successo!" -ForegroundColor Green
    Write-Host "💾 Il database ora contiene tutte le interazioni simulate" -ForegroundColor Green
    Write-Host "📈 Le analytics sono state aggiornate" -ForegroundColor Green
} else {
    Write-Host "   ❌ ALCUNI TEST SONO FALLITI" -ForegroundColor Red
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "🔍 Controlla i log sopra per i dettagli degli errori" -ForegroundColor Yellow
    Write-Host "💡 Possibili cause:" -ForegroundColor Yellow
    Write-Host "   - Dati mancanti nel database" -ForegroundColor White
    Write-Host "   - Timeout nelle richieste" -ForegroundColor White
    Write-Host "   - Errori nell'applicazione" -ForegroundColor White
}

Write-Host ""
Write-Host "📝 Per maggiori dettagli, esegui:" -ForegroundColor Cyan
Write-Host "   newman run $collectionPath --verbose" -ForegroundColor White
Write-Host ""

exit $exitCode
