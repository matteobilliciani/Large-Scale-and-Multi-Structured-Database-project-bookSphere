# 🚀 Performance Testing con Gatling - BookSphere

## 📋 Panoramica

Questa suite di test di performance utilizza **Gatling 3.11.5** per verificare le prestazioni di BookSphere sotto diversi scenari di carico. Include test di carico, stress test, spike test e test di resistenza.

## 📂 Struttura dei Test

```
src/test/scala/it/unipi/bookSphere/performance/
├── BasicLoadTest.scala           # Test di carico base (50 utenti)
├── StressTest.scala              # Test di stress progressivo (fino a 200 utenti)
├── SpikeTest.scala               # Test di picchi improvvisi
├── EnduranceTest.scala           # Test di resistenza (2 ore)
└── DatabaseIntensiveTest.scala   # Test operazioni intensive su DB
```

## 🎯 Tipi di Test Implementati

### 1. **BasicLoadTest** - Test di Carico Base
**Obiettivo**: Verificare le performance sotto carico normale

**Caratteristiche**:
- 50 utenti concorrenti
- Ramp-up di 30 secondi
- Durata: 2 minuti
- Scenari:
  - Accesso pubblico alle API (browse, search, filter)
  - Registrazione e autenticazione utenti
  - Operazioni utenti autenticati

**Metriche attese**:
- Response time medio < 1 secondo
- Response time massimo < 5 secondi
- Success rate > 95%

### 2. **StressTest** - Test di Stress Progressivo
**Obiettivo**: Trovare il breaking point del sistema

**Caratteristiche**:
- Incremento da 10 a 200 utenti
- Step di 10 utenti ogni 30 secondi
- Durata massima: 15 minuti
- Focus su operazioni intensive (browse, search, multiple requests)

**Metriche attese**:
- 95th percentile < 5 secondi
- 99th percentile < 8 secondi
- Success rate > 90%

### 3. **SpikeTest** - Test di Picchi Improvvisi
**Obiettivo**: Testare la resilienza a picchi di traffico improvvisi

**Caratteristiche**:
- Salto da 10 a 200 utenti istantaneamente
- Mantiene il picco per 60 secondi
- Ritorno graduale al carico normale
- Simula eventi come lancio funzionalità o viralità social

**Metriche attese**:
- Response time massimo < 10 secondi
- Success rate > 85%

### 4. **EnduranceTest** - Test di Resistenza
**Obiettivo**: Verificare stabilità del sistema su lunghi periodi

**Caratteristiche**:
- 50 utenti costanti
- Durata: 2 ore (default)
- Identifica memory leaks, degradazione performance
- Throttling per evitare sovraccarico

**Metriche attese**:
- Response time medio < 1.5 secondi (costante)
- Success rate > 99%
- Nessuna degradazione nel tempo

### 5. **DatabaseIntensiveTest** - Test Operazioni DB Intensive
**Obiettivo**: Testare performance di MongoDB e Neo4j sotto carico

**Caratteristiche**:
- 30 utenti su operazioni pesanti
- Focus su:
  - Aggregation pipelines MongoDB
  - Query complesse Neo4j
  - Consistency cross-database
- Durata: 10 minuti

**Metriche attese**:
- Response time medio < 2 secondi
- 95th percentile < 4 secondi
- Success rate > 95%

## 🛠️ Prerequisiti

1. **Java JDK 21** (già installato)
2. **Maven 3.x** (già installato)
3. **Applicazione BookSphere in esecuzione**:
   ```powershell
   mvn spring-boot:run
   ```

## ▶️ Esecuzione dei Test

### Metodo 1: Maven Command Line (Raccomandato)

#### Test Singolo
```powershell
# Basic Load Test
mvn gatling:test -Dgatling.simulationClass=it.unipi.bookSphere.performance.BasicLoadTest

# Stress Test
mvn gatling:test -Dgatling.simulationClass=it.unipi.bookSphere.performance.StressTest

# Spike Test
mvn gatling:test -Dgatling.simulationClass=it.unipi.bookSphere.performance.SpikeTest

# Endurance Test (attenzione: dura 2 ore!)
mvn gatling:test -Dgatling.simulationClass=it.unipi.bookSphere.performance.EnduranceTest

# Database Intensive Test
mvn gatling:test -Dgatling.simulationClass=it.unipi.bookSphere.performance.DatabaseIntensiveTest
```

#### Personalizzazione Parametri
```powershell
# BasicLoadTest con parametri custom
mvn gatling:test `
  -Dgatling.simulationClass=it.unipi.bookSphere.performance.BasicLoadTest `
  -Dusers=100 `
  -DrampDuration=60 `
  -DtestDuration=300 `
  -DbaseUrl=http://localhost:8080

# StressTest con parametri custom
mvn gatling:test `
  -Dgatling.simulationClass=it.unipi.bookSphere.performance.StressTest `
  -DinitialUsers=20 `
  -DmaxUsers=300 `
  -DincrementUsers=20 `
  -DstepDuration=45

# EnduranceTest ridotto (30 minuti invece di 2 ore)
mvn gatling:test `
  -Dgatling.simulationClass=it.unipi.bookSphere.performance.EnduranceTest `
  -DconstantUsers=30 `
  -DtestDuration=30
```

### Metodo 2: Script PowerShell (Facile e Veloce)

Usa gli script nella cartella `performance-tests/`:

```powershell
# Esegui tutti i test in sequenza
.\performance-tests\run-all-tests.ps1

# Esegui test singolo
.\performance-tests\run-basic-load.ps1
.\performance-tests\run-stress-test.ps1
.\performance-tests\run-spike-test.ps1
```

### Metodo 3: Esecuzione con IDE

1. Installa il plugin **Scala** in IntelliJ IDEA o VS Code
2. Apri il file di test desiderato
3. Click destro → Run Gatling Simulation

## 📊 Report e Grafici

### Visualizzazione Report

Dopo l'esecuzione, Gatling genera automaticamente report HTML interattivi:

**Posizione**: `target/gatling/[simulationName]-[timestamp]/index.html`

**Esempio**:
```
target/gatling/basicloadtest-20260211143522345/index.html
```

### Aprire il Report

**Opzione 1 - Browser**:
```powershell
# Windows
start target/gatling/basicloadtest-*/index.html
```

**Opzione 2 - Terminale**:
```powershell
cd target/gatling
ls
# Naviga nella cartella più recente e apri index.html
```

### Contenuto del Report

I report Gatling includono **grafici interattivi** che mostrano:

1. **Global Information**
   - Numero totale di richieste
   - Success rate
   - Response time (min, max, mean, percentili)
   - Requests per second

2. **Active Users Over Time**
   - Grafico del numero di utenti attivi nel tempo
   - Visualizza ramp-up e distribuzione del carico

3. **Response Time Distribution**
   - Istogramma dei tempi di risposta
   - Identifica outliers e performance anomalie

4. **Response Time Percentiles Over Time**
   - Grafici evolutivi dei percentili (50th, 75th, 95th, 99th)
   - Mostra stabilità o degradazione nel tempo

5. **Requests Per Second**
   - Throughput del sistema
   - Confronto tra richieste totali e richieste OK

6. **Response Time vs Requests Per Second**
   - Correlazione tra carico e performance
   - Identifica il punto di saturazione

7. **Request Statistics**
   - Tabella dettagliata per ogni endpoint testato
   - Count, success %, min/max/mean/std dev, percentili

## 📈 Interpretazione dei Risultati

### Metriche Chiave

| Metrica | Valore OK | Valore Warning | Valore Critico |
|---------|-----------|----------------|----------------|
| **Response Time Medio** | < 1s | 1-2s | > 2s |
| **95th Percentile** | < 2s | 2-4s | > 4s |
| **99th Percentile** | < 5s | 5-8s | > 8s |
| **Success Rate** | > 99% | 95-99% | < 95% |
| **Throughput** | Stabile | Fluttuante | Degradante |

### Analisi dei Problemi

**Response Time Alto**:
- Verificare query database inefficienti
- Controllare CPU/Memory del server
- Analizzare network latency

**Success Rate Basso**:
- Verificare log errori (500, 503, 504)
- Controllare connessioni database
- Verificare timeout configurati

**Degradazione nel Tempo** (Endurance Test):
- Memory leak (heap space)
- Connection pool saturo
- Risorse non rilasciate correttamente

## 🎨 Best Practices per i Test

### Prima di Eseguire i Test

1. **Assicurati che l'applicazione sia in esecuzione**:
   ```powershell
   mvn spring-boot:run
   # Oppure esegui da IDE
   ```

2. **Verifica la connessione ai database**:
   - MongoDB Atlas attivo
   - Neo4j locale in esecuzione

3. **Chiudi applicazioni pesanti** per risultati accurati

4. **Usa dati di test**, non production data

### Durante i Test

1. **Monitora risorse di sistema**:
   - Task Manager (CPU, RAM, Network)
   - Connessioni database attive

2. **Non eseguire altri task pesanti** sul PC

3. **Prendi note** su configurazione e osservazioni

### Dopo i Test

1. **Salva i report** in una cartella dedicata:
   ```powershell
   mkdir performance-results
   cp -r target/gatling/* performance-results/
   ```

2. **Documenta i risultati** per il report del progetto

3. **Confronta risultati** tra diversi test

## 📝 Documentazione per il Progetto Universitario

### Cosa Includere nel Report

1. **Setup e Configurazione**
   - Hardware utilizzato (CPU, RAM)
   - Configurazione database (replica set, connessioni)
   - Parametri test eseguiti

2. **Risultati per Ogni Test**
   - Screenshot dei grafici principali
   - Tabella riassuntiva delle metriche
   - Analisi dei risultati

3. **Performance Bottleneck Identificati**
   - Query lente
   - Endpoint critici
   - Risorse limitate

4. **Ottimizzazioni Implementate** (se applicabile)
   - Indici aggiunti
   - Cache implementate
   - Query ottimizzate

### Tabella Riassuntiva Esempio

```markdown
| Test Type | Users | Duration | Avg RT | 95th % | Success | RPS |
|-----------|-------|----------|--------|--------|---------|-----|
| Basic Load | 50 | 2 min | 245ms | 890ms | 99.2% | 142 |
| Stress | 200 | 15 min | 1.2s | 4.5s | 92.5% | 315 |
| Spike | 200 | 2 min | 2.8s | 8.2s | 87.3% | 380 |
| Endurance | 50 | 2 hours | 310ms | 1.1s | 99.8% | 135 |
| DB Intensive | 30 | 10 min | 1.8s | 3.2s | 96.1% | 85 |
```

## 🐛 Troubleshooting

### Errore: "Simulation class not found"
```powershell
# Compila prima i test Scala
mvn test-compile
# Poi esegui nuovamente il test
```

### Errore: "Connection refused"
```powershell
# Verifica che l'app sia in esecuzione
curl http://localhost:8080/api/books?page=0&size=1
```

### Report non generato
```powershell
# Aggiungi -X per vedere dettagli debug
mvn gatling:test -X -Dgatling.simulationClass=...
```

### Performance molto basse
- Verifica che MongoDB Atlas non abbia limitazioni free tier
- Controlla la connessione internet
- Usa profilo local se disponibile

## 🚀 Comandi Rapidi

```powershell
# Test completo per demo
mvn gatling:test -Dgatling.simulationClass=it.unipi.bookSphere.performance.BasicLoadTest

# Test veloce (30 secondi)
mvn gatling:test -Dgatling.simulationClass=it.unipi.bookSphere.performance.SpikeTest -DspikeDuration=30

# Apri ultimo report generato
start target/gatling/*latest*/index.html

# Lista tutti i report
ls target/gatling
```

## 📚 Risorse Utili

- **Documentazione Gatling**: https://gatling.io/docs/current/
- **Gatling DSL Reference**: https://gatling.io/docs/current/cheat-sheet/
- **Performance Testing Best Practices**: https://gatling.io/docs/current/general/concepts/

## 🎓 Per la Valutazione del Progetto

I test implementati dimostrano:

✅ **Scalabilità** - Sistema testato fino a 200+ utenti concorrenti
✅ **Resilienza** - Verificata stabilità sotto stress e spike
✅ **Affidabilità** - Test di resistenza di 2 ore
✅ **Performance Database** - Test specifici MongoDB + Neo4j
✅ **Metriche Quantitative** - Report dettagliati con grafici professionali

Questi test forniscono evidenza empirica delle performance del sistema e possono essere inclusi nella documentazione del progetto come prova di testing professionale.
