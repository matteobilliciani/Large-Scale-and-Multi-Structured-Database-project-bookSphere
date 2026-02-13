# 📚 BookSphere - Postman/Newman Functional Tests

Questo folder contiene i **functional tests con storytelling** per il sistema BookSphere.

## 📁 File Disponibili

### Collections Postman

| File | Descrizione | Utenti | Requests | Status |
|------|-------------|--------|----------|--------|
| **`bookSphere_storytelling_enhanced.postman_collection.json`** | 🚀 **VERSIONE ENHANCED** (Recommended) | **6** | **62** | ✅ **TUTTI PASSATI** |
| `bookSphere_functional_tests.postman_collection.json` | Versione base originale | 3 | ~35 | ⚠️ Deprecata |

### Script di Esecuzione

| File | Descrizione |
|------|-------------|
| **`run-enhanced-tests.ps1`** | 🚀 **Script per versione Enhanced** (Recommended) |
| `run-storytelling-tests.ps1` | Script per versione originale (deprecato) |

### Documentazione

| File | Descrizione |
|------|-------------|
| **`ENHANCED_TEST_RESULTS.md`** | 📊 Risultati dettagliati versione Enhanced |
| `README_STORYTELLING.md` | Documentazione versione originale |
| **`README.md`** | Questo file |

---

## 🚀 Quick Start - VERSIONE ENHANCED (Recommended)

### 1. Prerequisiti

```bash
# Installa Newman (se non già installato)
npm install -g newman

# Verifica installazione
newman --version
# Output atteso: 6.2.2 o superiore
```

### 2. Avvia Applicazione

```bash
# Dalla root del progetto BookSphere
.\mvnw.cmd spring-boot:run

# Aspetta che l'app sia pronta (circa 30 secondi)
# Output atteso: "Started BookSphere in X seconds"
```

### 3. Esegui i Test

**Opzione A: Usa lo script PowerShell (raccomandato)**
```powershell
cd postman_newman
.\run-enhanced-tests.ps1
```

**Opzione B: Usa Newman direttamente**
```bash
cd postman_newman
newman run bookSphere_storytelling_enhanced.postman_collection.json --color on
```

### 4. Risultati Attesi

```
✅ iterations:          1 executed, 0 failed
✅ requests:           62 executed, 0 failed  
✅ test-scripts:       36 executed, 0 failed
✅ assertions:         36 passed, 0 failed
```

**Output finale**:
```
🎯 ==========================================
     IMPATTO ANALYTICS - SUMMARY
   ==========================================

   📊 NUMERI FINALI:
      • 6 utenti registrati
      • 12 recensioni create
      • 17 book likes
      • 6 author likes
      • 14 relazioni follow
      • 4 libri con attività massiva

   ✨ MISSIONE COMPLETATA! ✨
```

---

## 📖 Storytelling Overview - Versione Enhanced

### 🎭 Struttura Narrativa (6 Atti)

La collection simula un **viaggio realistico** di 6 utenti che scoprono e interagiscono con BookSphere:

#### 🎬 PROLOGO - Database State
Raccoglie lo stato iniziale delle analytics per confronto finale.

#### 🎭 ATTO I - Registrazioni (6 Utenti)
**6 utenti** da paesi diversi si registrano al sistema:
- 👤 **Alice** (USA) - Lettrice vorace di romanzi d'amore
- 👤 **Bob** (UK) - Critico letterario, amante del thriller
- 👤 **Charlie** (Italy) - Bibliofilo, letture filosofiche
- 👤 **Dave** (France) - Appassionato di biografie
- 👤 **Eve** (Germany) - Ama i classici
- 👤 **Frank** (Spain) - Scopre nuovi autori

#### 🔍 ATTO II - Discovery (4 Libri Diversi)
Ogni utente cerca e scopre libri dal database:
- 📖 Alice cerca "Love" → Trova libro romantico
- 📖 Bob cerca "Night" → Trova thriller notturno
- 📖 Charlie cerca "Life" → Trova filosofia di vita
- 📖 Dave cerca "The" → Trova classico

#### ✍️ ATTO III - Reviews (12 Recensioni!)
**12 recensioni dettagliate** su 4 libri diversi:
- Ogni libro riceve **3 recensioni** da utenti diversi
- Rating realistici: **84-95/100**
- Testi lunghi e significativi (~150 parole)
- **Impatto MongoDB**: `averageRating`, `monthScore`, trending aggiornati

#### ❤️ ATTO IV - Likes Explosion (22+ Likes!)
**Interazioni massive sul social graph**:
- **16 book likes** distribuiti strategicamente
- **6 author likes** (tutti seguono lo stesso autore)
- **Impatto Neo4j**: 22 nuove relazioni LIKES

#### 🤝 ATTO V - Social Network Denso (14 Follow!)
**Social network connesso**:
- **14 relazioni FOLLOWS** tra i 6 utenti
- **Alice è l'hub centrale** (seguita da tutti)
- Connessioni reciproche e bidirezionali
- **Impatto Neo4j**: Grafo sociale denso per recommendations

#### 🎬 EPILOGO - Analytics Trasformate!
**Verifica impatto finale**:
- 📊 Trending books completamente modificati
- 📈 Book rankings aggiornati significativamente
- 👥 Author rankings riflettono le 12 nuove recensioni
- ✨ **Confronto Before/After**

---

## 💾 Impatto sui Database

### MongoDB (Document Store)
- ✅ **6 nuovi users** (con credenziali, JWT tokens, metadata)
- ✅ **12 nuove reviews** (rating, text, summary, timestamps)
- ✅ **4 books aggiornati** (stats, averageRating, monthScore, ratingCount)
- ✅ **Analytics trasformate** (trending, rankings, revaluated)

### Neo4j (Graph Database)
- ✅ **6 User nodes** creati
- ✅ **12 ReviewNode** creati
- ✅ **14 FOLLOWS relationships** (social network)
- ✅ **16 LIKES relationships** per books
- ✅ **6 LIKES relationships** per author
- ✅ **24 review-related relationships** (HAS_REVIEWED, REVIEWS)
- ✅ **Totale: 60+ nuove relazioni nel grafo**

---

## 🎯 Obiettivi dei Test

### ✅ Functional Testing
- Verifica che **tutte le API principali** funzionino correttamente
- Test delle **autenticazioni JWT** (register, login)
- Test delle **operazioni CRUD** (reviews, likes, follows)
- Test delle **query analytics** (trending, rankings)

### ✅ Integration Testing
- Verifica **sincronizzazione MongoDB ↔️ Neo4j**
- Test delle **transazioni multi-database**
- Verifica **consistency** tra document store e graph

### ✅ Data Quality Testing
- **Validazione** dei dati inseriti (country codes ISO, ratings 0-100)
- **Aggregazioni corrette** (averageRating, totalRatings)
- **MonthScore** aggiornato in real-time

### ✅ Analytics Impact Testing
- **Trending books** modificati dalle nuove interazioni
- **Rankings** aggiornati con le nuove recensioni
- **Author statistics** aggiornate
- **Before/After comparison**

### ✅ Social Graph Testing
- **Network density** sufficiente per recommendations
- **Follow relationships** bidirezionali
- **Likes distribution** realistica
- **Hub detection** (Alice come nodo centrale)

---

## 📊 Confronto Versioni

| Metrica | Versione Originale | Versione Enhanced | Incremento |
|---------|-------------------|-------------------|-----------|
| **Utenti** | 3 | **6** | +100% |
| **Recensioni** | 3-6 | **12** | +100-300% |
| **Book Likes** | ~7 | **16** | +128% |
| **Author Likes** | 0 | **6** | ∞ |
| **Follow Relations** | ~6 | **14** | +133% |
| **Libri diversi** | 2 | **4** | +100% |
| **Total Requests** | ~35 | **62** | +77% |
| **Durata** | ~5s | ~9s | +80% |
| **Errori** | 20 (401/400) | **0** | -100% ✅ |
| **Impatto Analytics** | Basso | **ALTO** | Massivo |

---

## 🔧 Troubleshooting

### ❌ Errore: "Newman non trovato"
```bash
npm install -g newman

# Se errori di permessi su Windows
npm install -g newman --force
```

### ❌ Errore: "Applicazione non risponde"
```bash
# Verifica che l'app sia avviata
curl http://localhost:8080/api/v1/analytics/rankings/trendingbooks

# Se non risponde, riavvia l'app
.\mvnw.cmd spring-boot:run
```

### ❌ Errore: "400 Bad Request - Invalid country code"
**Causa**: Uso di nomi paese completi invece di codici ISO  
**Soluzione**: La versione Enhanced usa già i codici corretti (US, GB, IT, FR, DE, ES)

### ❌ Errore: "401 Unauthorized"
**Causa**: Token JWT non salvato correttamente nelle variabili  
**Soluzione**: Verifica che i test script salvino `pm.collectionVariables.set('alice_token', r.token)`

### ❌ Errore: "Book not found"
**Causa**: Ricerca di libri che non esistono nel database  
**Soluzione**: La versione Enhanced usa termini generici ("Love", "Night", "Life", "The") che garantiscono risultati

### ⚠️ Warning: "Deprecation Warning"
**Messaggio**: `fs.F_OK is deprecated`  
**Impatto**: **NESSUNO** - È solo un warning di Node.js, i test funzionano correttamente

---

## 📖 API Testate

### Authentication
- ✅ `POST /api/v1/auth/register` - Registrazione utente
- ✅ `POST /api/v1/auth/login` - Login (implicito tramite token da register)

### Books
- ✅ `GET /api/v1/books?title={query}` - Ricerca libri
- ✅ `GET /api/v1/books/{id}` - Dettagli libro (implicito)

### Reviews
- ✅ `POST /api/v1/me/reviews` - Creazione recensione
- ✅ `GET /api/v1/me/reviews` - Elenco recensioni utente (implicito)

### Likes
- ✅ `POST /api/v1/me/like/book` - Like a libro
- ✅ `POST /api/v1/me/likes/authors` - Like ad autore

### Follow
- ✅ `POST /api/v1/me/follow` - Segui utente
- ✅ `GET /api/v1/me/friends` - Lista amici (implicito)

### Analytics
- ✅ `GET /api/v1/analytics/rankings/trendingbooks` - Trending books
- ✅ `GET /api/v1/analytics/books?year={year}` - Book rankings
- ✅ `GET /api/v1/analytics/rankings/authors` - Author rankings

---

## 🎓 Utilizzi Possibili

### 1. **Automated Testing in CI/CD**
Integra Newman nella pipeline CI/CD per test automatici dopo ogni deploy:
```yaml
# .gitlab-ci.yml o .github/workflows/tests.yml
test-functional:
  script:
    - npm install -g newman
    - newman run postman_newman/bookSphere_storytelling_enhanced.postman_collection.json
```

### 2. **Demo per Stakeholders**
Esegui i test durante demo live per mostrare:
- Creazione utenti real-time
- Popolamento database multi-strutturato
- Impatto immediato sulle analytics
- Social network che si forma dinamicamente

### 3. **Performance Benchmarking**
Usa Newman con reporter JSON per raccogliere metriche:
```bash
newman run bookSphere_storytelling_enhanced.postman_collection.json \
    --reporters json \
    --reporter-json-export results.json
```

### 4. **Ambiente di Test/Staging**
Popola rapidamente ambienti di test con dati realistici:
```bash
# Esegui 5 volte per creare 30 utenti, 60 recensioni, etc.
for i in {1..5}; do
    newman run bookSphere_storytelling_enhanced.postman_collection.json
done
```

### 5. **Training di Nuovi Sviluppatori**
Usa la collection come:
- **Tutorial interattivo** per capire il flusso API
- **Documentazione eseguibile** delle API
- **Esempio di best practices** (JWT, error handling, validazioni)

### 6. **Regression Testing**
Prima di ogni release, verifica che le API non siano regredite:
```bash
# Pre-release checklist
./run-enhanced-tests.ps1
# Se 62/62 requests passati → OK per deploy
```

---

## 📚 Documentazione Aggiuntiva

- **API Complete Documentation**: Vedi `MD_COPILOT/API_DOCUMENTATION.md`
- **MongoDB Analytics**: Vedi `MD_COPILOT/MONGODB_ANALYTICS_TEST_DOCUMENTATION.md`
- **Test Suite Documentation**: Vedi `MD_COPILOT/TEST_SUITE_DOCUMENTATION.md`
- **Detailed Results**: Vedi `ENHANCED_TEST_RESULTS.md` (questo folder)

---

## 🤝 Contribuire

Per aggiungere nuovi test alla collection:

1. **Importa in Postman Desktop**:
   - File → Import → Seleziona `bookSphere_storytelling_enhanced.postman_collection.json`

2. **Aggiungi Request**:
   - Crea nuovo request nel folder appropriato (Atto III, IV, V)
   - Usa variabili collection: `{{alice_token}}`, `{{book1_id}}`, etc.
   - Aggiungi test script per assertions

3. **Test Script Example**:
   ```javascript
   pm.test('Request successful', () => {
       pm.response.to.have.status(200);
   });
   
   console.log('✅ Operazione completata!');
   ```

4. **Export & Commit**:
   - File → Export → Collection v2.1
   - Sostituisci il file JSON esistente
   - Commit & Push

---

## 📞 Support

**Problemi o domande?**
- Consulta `ENHANCED_TEST_RESULTS.md` per troubleshooting dettagliato
- Verifica la documentazione API in `MD_COPILOT/`
- Controlla i logs dell'applicazione per errori server-side

---

## ✨ Credits

**Collection Design**: Storytelling approach con 6 personaggi  
**Versione**: 2.0 Enhanced (Febbraio 2026)  
**Status**: ✅ Production Ready  
**Coverage**: 62 requests, 36 assertions, 80+ database operations  

**Best Practice Implemented**:
- ISO country codes
- JWT authentication
- Dynamic variables
- Skip logic for missing data
- Pre-request guards
- Comprehensive logging
- Before/After analytics comparison

---

*📚 BookSphere - Large-Scale and Multi-Structured Database Project*  
*Powered by MongoDB Atlas + Neo4j + Spring Boot*
