#!/usr/bin/env pwsh
# Script per compilare rapidamente la documentazione LaTeX

Set-Location -Path (Join-Path $PSScriptRoot "documentation")

Write-Host "📄 Compilazione documentazione LaTeX..." -ForegroundColor Cyan

# Prima compilazione
pdflatex -synctex=1 -interaction=nonstopmode main.tex | Out-Null

# Seconda compilazione per riferimenti
pdflatex -synctex=1 -interaction=nonstopmode main.tex | Out-Null

if (Test-Path "main.pdf") {
    $fileInfo = Get-Item "main.pdf"
    Write-Host "✅ PDF generato con successo!" -ForegroundColor Green
    Write-Host "📊 Dimensione: $([math]::Round($fileInfo.Length/1KB, 2)) KB" -ForegroundColor Yellow
    Write-Host "📁 Percorso: $($fileInfo.FullName)" -ForegroundColor Gray
    
    # Apri il PDF
    Start-Process $fileInfo.FullName
} else {
    Write-Host "❌ Errore nella compilazione" -ForegroundColor Red
    exit 1
}
