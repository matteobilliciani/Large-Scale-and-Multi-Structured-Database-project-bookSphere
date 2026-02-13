@echo off
cd documentation
echo Compilazione documentazione...
pdflatex -synctex=1 -interaction=nonstopmode main.tex > nul
pdflatex -synctex=1 -interaction=nonstopmode main.tex > nul
if exist main.pdf (
    echo PDF generato con successo!
    start main.pdf
) else (
    echo Errore nella compilazione!
    pause
)
