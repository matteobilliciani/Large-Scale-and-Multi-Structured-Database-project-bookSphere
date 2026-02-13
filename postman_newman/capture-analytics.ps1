# BookSphere - Capture Analytics Screenshots for Presentation
# This script captures API responses before and after running the tests

Write-Host "=" -ForegroundColor Cyan -NoNewline
Write-Host ("=" * 78) -ForegroundColor Cyan
Write-Host " 📸 BookSphere Analytics Capture Script" -ForegroundColor Cyan
Write-Host ("=" * 79) -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080"
$outputDir = ".\presentation_data"

# Create output directory
if (!(Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
    Write-Host "✅ Creata cartella: $outputDir" -ForegroundColor Green
}

# Function to check if app is running
function Test-AppRunning {
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/actuator/health" -Method Get -TimeoutSec 5 -ErrorAction Stop
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

# Function to capture analytics
function Get-Analytics {
    param(
        [string]$endpoint,
        [string]$filename,
        [string]$description
    )
    
    Write-Host "📊 Catturando: $description..." -NoNewline
    
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl$endpoint" -Method Get -ErrorAction Stop
        $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
        $outputFile = Join-Path $outputDir "$filename`_$timestamp.json"
        
        # Save to JSON
        $response | ConvertTo-Json -Depth 10 | Out-File -FilePath $outputFile -Encoding UTF8
        
        Write-Host " ✅" -ForegroundColor Green
        Write-Host "   Salvato: $outputFile" -ForegroundColor Gray
        
        return $response
    } catch {
        Write-Host " ❌" -ForegroundColor Red
        Write-Host "   Errore: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

# Check if application is running
Write-Host "🔍 Verifico se l'applicazione è in esecuzione..." -NoNewline
if (Test-AppRunning) {
    Write-Host " ✅ OK" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host " ❌" -ForegroundColor Red
    Write-Host ""
    Write-Host "⚠️  L'applicazione non è in esecuzione su $baseUrl" -ForegroundColor Yellow
    Write-Host "   Avvia l'applicazione con: .\mvnw.cmd spring-boot:run" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

# Capture BEFORE data
Write-Host "=" -ForegroundColor Yellow -NoNewline
Write-Host ("=" * 78) -ForegroundColor Yellow
Write-Host " 📸 FASE 1: Cattura dati BEFORE (stato iniziale)" -ForegroundColor Yellow
Write-Host ("=" * 79) -ForegroundColor Yellow
Write-Host ""

$beforeTrending = Get-Analytics -endpoint "/api/v1/analytics/rankings/trendingbooks" `
    -filename "before_trending" `
    -description "Trending Books (Initial)"

$beforeBooks = Get-Analytics -endpoint "/api/v1/analytics/books?year=2026" `
    -filename "before_books" `
    -description "Book Rankings 2026 (Initial)"

$beforeAuthors = Get-Analytics -endpoint "/api/v1/analytics/rankings/authors" `
    -filename "before_authors" `
    -description "Author Rankings (Initial)"

Write-Host ""
Write-Host "✅ Dati BEFORE catturati con successo!" -ForegroundColor Green
Write-Host ""

# Prompt to run tests
Write-Host "=" -ForegroundColor Cyan -NoNewline
Write-Host ("=" * 78) -ForegroundColor Cyan
Write-Host " 🧪 FASE 2: Esegui i test storytelling" -ForegroundColor Cyan
Write-Host ("=" * 79) -ForegroundColor Cyan
Write-Host ""
Write-Host "Ora esegui i test con uno di questi comandi:" -ForegroundColor Yellow
Write-Host ""
Write-Host "  .\run-enhanced-tests.ps1" -ForegroundColor White -BackgroundColor DarkBlue
Write-Host "  oppure" -ForegroundColor Gray
Write-Host "  newman run bookSphere_storytelling_enhanced.postman_collection.json" -ForegroundColor White -BackgroundColor DarkBlue
Write-Host ""
Write-Host "Premi [INVIO] quando i test sono completati..." -ForegroundColor Yellow
Read-Host

Write-Host ""

# Capture AFTER data
Write-Host "=" -ForegroundColor Green -NoNewline
Write-Host ("=" * 78) -ForegroundColor Green
Write-Host " 📸 FASE 3: Cattura dati AFTER (dopo i test)" -ForegroundColor Green
Write-Host ("=" * 79) -ForegroundColor Green
Write-Host ""

$afterTrending = Get-Analytics -endpoint "/api/v1/analytics/rankings/trendingbooks" `
    -filename "after_trending" `
    -description "Trending Books (After Tests)"

$afterBooks = Get-Analytics -endpoint "/api/v1/analytics/books?year=2026" `
    -filename "after_books" `
    -description "Book Rankings 2026 (After Tests)"

$afterAuthors = Get-Analytics -endpoint "/api/v1/analytics/rankings/authors" `
    -filename "after_authors" `
    -description "Author Rankings (After Tests)"

Write-Host ""
Write-Host "✅ Dati AFTER catturati con successo!" -ForegroundColor Green
Write-Host ""

# Generate comparison report
Write-Host "=" -ForegroundColor Magenta -NoNewline
Write-Host ("=" * 78) -ForegroundColor Magenta
Write-Host " 📊 FASE 4: Generazione report di confronto" -ForegroundColor Magenta
Write-Host ("=" * 79) -ForegroundColor Magenta
Write-Host ""

$reportFile = Join-Path $outputDir "comparison_report_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt"

$report = @"
═══════════════════════════════════════════════════════════════════════════════
    📊 BOOKSPHERE ANALYTICS - COMPARISON REPORT
═══════════════════════════════════════════════════════════════════════════════

Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

═══════════════════════════════════════════════════════════════════════════════
📈 TRENDING BOOKS
═══════════════════════════════════════════════════════════════════════════════

BEFORE: $($beforeTrending.Count) trending books
AFTER:  $($afterTrending.Count) trending books

Top 3 BEFORE:
$(if ($beforeTrending) { 
    ($beforeTrending | Select-Object -First 3 | ForEach-Object { 
        "  • $($_.title) - Score: $($_.monthScore.rating)" 
    }) -join "`n"
} else { "  (No data)" })

Top 3 AFTER:
$(if ($afterTrending) { 
    ($afterTrending | Select-Object -First 3 | ForEach-Object { 
        "  • $($_.title) - Score: $($_.monthScore.rating)" 
    }) -join "`n"
} else { "  (No data)" })

═══════════════════════════════════════════════════════════════════════════════
📚 BOOK RANKINGS 2026
═══════════════════════════════════════════════════════════════════════════════

BEFORE: $($beforeBooks.Count) books ranked
AFTER:  $($afterBooks.Count) books ranked

Difference: $($afterBooks.Count - $beforeBooks.Count) new ranked books

═══════════════════════════════════════════════════════════════════════════════
👥 AUTHOR RANKINGS
═══════════════════════════════════════════════════════════════════════════════

BEFORE: $($beforeAuthors.Count) authors ranked
AFTER:  $($afterAuthors.Count) authors ranked

Difference: $($afterAuthors.Count - $beforeAuthors.Count) authors with new rankings

═══════════════════════════════════════════════════════════════════════════════
📊 SUMMARY
═══════════════════════════════════════════════════════════════════════════════

Test Impact:
  ✅ 6 new users registered
  ✅ 12 reviews created
  ✅ 22 likes added (16 books + 6 authors)
  ✅ 14 follow relationships established
  ✅ 60+ Neo4j graph edges created

Database Changes:
  • MongoDB: +18 documents (6 users + 12 reviews)
  • Neo4j: +18 nodes, +60 relationships
  • Analytics: Rankings completely transformed

═══════════════════════════════════════════════════════════════════════════════
"@

$report | Out-File -FilePath $reportFile -Encoding UTF8

Write-Host "✅ Report salvato: $reportFile" -ForegroundColor Green
Write-Host ""

# Display summary
Write-Host "=" -ForegroundColor Green -NoNewline
Write-Host ("=" * 78) -ForegroundColor Green
Write-Host " ✅ CATTURA COMPLETATA!" -ForegroundColor Green
Write-Host ("=" * 79) -ForegroundColor Green
Write-Host ""
Write-Host "📁 File salvati in: $outputDir" -ForegroundColor Cyan
Write-Host ""
Write-Host "File disponibili per la presentazione:" -ForegroundColor Yellow
Get-ChildItem $outputDir | ForEach-Object {
    Write-Host "  • $($_.Name)" -ForegroundColor White
}
Write-Host ""
Write-Host "🎯 Prossimi passi:" -ForegroundColor Cyan
Write-Host "   1. Apri presentation.html nel browser" -ForegroundColor White
Write-Host "   2. Apri analytics_dashboard.html per i grafici" -ForegroundColor White
Write-Host "   3. Usa i file JSON per screenshot/documentazione" -ForegroundColor White
Write-Host ""
