# BookSphere Enhanced Storytelling Tests Runner
# Esegue la versione potenziata con 6 utenti e 40+ interazioni

Write-Host "`n================================================" -ForegroundColor Cyan
Write-Host "   📚 BookSphere Enhanced Tests (HIGH IMPACT!)" -ForegroundColor Cyan
Write-Host "================================================`n" -ForegroundColor Cyan

# 1. Check Newman
Write-Host "🔍 Verifica Newman..." -ForegroundColor Yellow
try {
    $newmanVersion = newman --version
    Write-Host "✅ Newman trovato: v$newmanVersion`n" -ForegroundColor Green
} catch {
    Write-Host "❌ Newman non trovato! Installa con: npm install -g newman`n" -ForegroundColor Red
    exit 1
}

# 2. Check Application
Write-Host "🔍 Verifica applicazione su http://localhost:8080..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/analytics/rankings/trendingbooks" -UseBasicParsing -TimeoutSec 5
    if ($response.StatusCode -eq 200) {
        Write-Host "✅ Applicazione attiva e risponde!`n" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ Applicazione non risponde su localhost:8080!" -ForegroundColor Red
    Write-Host "   Avvia prima l'applicazione con: .\mvnw.cmd spring-boot:run`n" -ForegroundColor Yellow
    exit 1
}

# 3. Run Enhanced Collection
Write-Host "🚀 ESECUZIONE TEST ENHANCED..." -ForegroundColor Cyan
Write-Host "   • 6 utenti registrati" -ForegroundColor White
Write-Host "   • 12 recensioni create" -ForegroundColor White
Write-Host "   • 17 book likes" -ForegroundColor White
Write-Host "   • 6 author likes" -ForegroundColor White
Write-Host "   • 15 follow relationships" -ForegroundColor White
Write-Host "   • Analytics massivamente impattate!`n" -ForegroundColor White

$collectionPath = Join-Path $PSScriptRoot "bookSphere_storytelling_enhanced.postman_collection.json"

if (-not (Test-Path $collectionPath)) {
    Write-Host "❌ Collection non trovata: $collectionPath`n" -ForegroundColor Red
    exit 1
}

newman run $collectionPath `
    --reporters cli `
    --reporter-cli-no-assertions `
    --reporter-cli-no-console `
    --color on `
    --timeout-request 15000

$exitCode = $LASTEXITCODE

Write-Host "`n================================================" -ForegroundColor Cyan
if ($exitCode -eq 0) {
    Write-Host "   ✅ TESTS COMPLETATI CON SUCCESSO!" -ForegroundColor Green
    Write-Host "================================================`n" -ForegroundColor Cyan
    
    Write-Host "📊 CONTROLLA LE ANALYTICS:" -ForegroundColor Yellow
    Write-Host "   http://localhost:8080/api/v1/analytics/rankings/trendingbooks" -ForegroundColor Cyan
    Write-Host "   http://localhost:8080/api/v1/analytics/books?year=2026" -ForegroundColor Cyan
    Write-Host "   http://localhost:8080/api/v1/analytics/rankings/authors`n" -ForegroundColor Cyan
} else {
    Write-Host "   ⚠️  Tests completati con alcuni warning" -ForegroundColor Yellow
    Write-Host "================================================`n" -ForegroundColor Cyan
}

Write-Host "💡 TIP: " -NoNewline -ForegroundColor Yellow
Write-Host "Esegui più volte per aumentare l'impatto sulle analytics!`n" -ForegroundColor White

exit $exitCode
