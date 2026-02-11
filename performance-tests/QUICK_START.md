# ⚡ Quick Start - Performance Testing

Guida rapida per eseguire i performance test su BookSphere.

## 🚀 Setup Rapido (5 minuti)

### Step 1: Avvia l'Applicazione
```powershell
# In un terminale, avvia BookSphere
mvn spring-boot:run

# Oppure esegui da IntelliJ/VS Code
```

### Step 2: Verifica che sia attivo
```powershell
# Apri browser e vai su:
http://localhost:8080/api/books?page=0&size=1

# Dovresti vedere una risposta JSON
```

### Step 3: Esegui il tuo primo test
```powershell
# Apri un NUOVO terminale e vai nella cartella del progetto
cd performance-tests

# Esegui il test base (2 minuti)
.\run-basic-load.ps1

# Il report si aprirà automaticamente nel browser!
```

## 🎯 Test Disponibili

### Test Veloci (2-5 minuti)
```powershell
# Basic Load Test - Perfetto per iniziare
.\run-basic-load.ps1

# Spike Test - Test di picchi improvvisi
.\run-spike-test.ps1
```

### Test Medi (10-15 minuti)
```powershell
# Database Intensive Test - Testa MongoDB + Neo4j
.\run-db-intensive.ps1

# Stress Test - Trova il breaking point
.\run-stress-test.ps1
```

### Suite Completa (~30 minuti)
```powershell
# Esegue tutti i test in sequenza
.\run-all-tests.ps1
```

## 📊 Come Vedere i Risultati

### I report si aprono automaticamente!

Se vuoi riaprirli manualmente:
```powershell
# Naviga nella cartella dei report
cd target\gatling

# Lista tutti i report
ls

# Apri il report più recente
start {nome-folder}\index.html
```

### Cosa vedrai nei report:
- 📈 **Grafici interattivi** di response time
- 👥 **Utenti attivi** nel tempo
- ⚡ **Throughput** (richieste/secondo)
- 📊 **Percentili** (50th, 95th, 99th)
- ✅ **Success rate** e errori
- 📉 **Distribuzione** dei tempi di risposta

## 🎨 Personalizzazione

### Modifica i parametri del test:
```powershell
# BasicLoadTest con più utenti
mvn gatling:test `
  -Dgatling.simulationClass=it.unipi.bookSphere.performance.BasicLoadTest `
  -Dusers=100 `
  -DtestDuration=300

# StressTest più aggressivo
mvn gatling:test `
  -Dgatling.simulationClass=it.unipi.bookSphere.performance.StressTest `
  -DmaxUsers=300
```

## 🐛 Problemi Comuni

### "Errore: Cannot connect to localhost:8080"
```powershell
# Soluzione: Avvia l'applicazione prima!
mvn spring-boot:run
```

### "Test molto lenti"
```powershell
# Causa possibile: MongoDB Atlas Free Tier lento
# Soluzione: Usa connessione locale o riduci il numero di utenti
-Dusers=25
```

### "Errori 500 o 503"
```powershell
# Causa: Database non disponibile o troppi utenti
# Soluzione: Verifica che MongoDB e Neo4j siano attivi
```

## 📝 Per il Report del Progetto

### 1. Esegui i test
```powershell
.\run-all-tests.ps1
```

### 2. Salva i report
```powershell
# Copia tutti i report in una cartella dedicata
mkdir ..\performance-results
cp -r ..\target\gatling\* ..\performance-results\
```

### 3. Prendi screenshot
- Response Time Distribution
- Active Users Over Time
- Request Statistics Table

### 4. Compila il template
- Apri: `RESULTS_TEMPLATE.md`
- Inserisci i tuoi risultati
- Aggiungi screenshot
- Aggiungi analisi

## 🎓 Tips per Ottimi Risultati

✅ **Esegui i test quando il PC non è sotto carico**
✅ **Chiudi applicazioni pesanti** (Chrome con tante tab, ecc.)
✅ **Verifica la connessione internet** se usi MongoDB Atlas
✅ **Aspetta tra un test e l'altro** (10 secondi) per stabilizzare il sistema
✅ **Esegui ogni test 2-3 volte** per avere risultati consistenti
✅ **Salva TUTTI i report** per confronti

## 📚 Documentazione Completa

Per informazioni dettagliate su ogni test, consulta:
- [README.md](README.md) - Documentazione completa
- [RESULTS_TEMPLATE.md](RESULTS_TEMPLATE.md) - Template per report finale

## 🚀 Next Steps

Dopo i test base, prova:
1. Modificare i parametri per simulare diversi scenari
2. Confrontare performance con/senza indici database
3. Testare con dataset più grandi
4. Analizzare i bottleneck identificati
5. Implementare ottimizzazioni e ri-testare

---

**Domande?** Consulta il README completo o la documentazione Gatling: https://gatling.io/docs/current/
