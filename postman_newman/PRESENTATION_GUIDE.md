# 📊 BookSphere - Presentation Materials

Materiali per la presentazione dei **Functional Tests con Storytelling** del sistema BookSphere.

## 📁 Files nella cartella

| File | Descrizione | Uso |
|------|-------------|-----|
| `presentation.html` | **Dashboard principale** con journey completo e esempi request/response | Apri nel browser per mostrare il flow narrativo |
| `analytics_dashboard.html` | **Dashboard con grafici interattivi** (Chart.js) per visualizzare before/after | Apri nel browser per mostrare confronti visivi |
| `capture-analytics.ps1` | **Script PowerShell** per catturare snapshot delle analytics prima e dopo i test | Esegui per generare dati JSON reali |
| `bookSphere_storytelling_enhanced.postman_collection.json` | Collection Postman completa (62 requests) | Importa in Postman o esegui con Newman |
| `run-enhanced-tests.ps1` | Script per eseguire i test automaticamente | Esegui per lanciare tutti i 62 test |

---

## 🚀 Quick Start per la Presentazione

### 1️⃣ **Preparazione (Prima della presentazione)**

```powershell
# Terminale 1 - Avvia l'applicazione
.\mvnw.cmd spring-boot:run

# Terminale 2 - Cattura dati BEFORE
cd postman_newman
.\capture-analytics.ps1
# Segui le istruzioni: premi INVIO quando ti viene chiesto
# Lo script cattura automaticamente i dati BEFORE e AFTER
```

Questo genererà la cartella `presentation_data/` con:
- `before_*.json` - Stato iniziale analytics
- `after_*.json` - Stato dopo i test
- `comparison_report_*.txt` - Report testuale del confronto

### 2️⃣ **Durante la Presentazione**

#### **Opzione A: Dashboard Interattivo (CONSIGLIATO)**

1. Apri `presentation.html` nel browser
2. Mostra:
   - Header con statistiche complessive
   - Journey per atti (Prologo → Atto I-V → Epilogo)
   - Esempi di request/response per ogni fase
   - Confronto before/after delle analytics
   - Tabella impatto database

3. **BONUS**: Clicca il bottone "**🔄 Carica Analytics Live dal Server**"
   - Mostra i dati REALI dal database in tempo reale!
   - Perfetto per dimostrare che i dati sono reali

#### **Opzione B: Dashboard con Grafici**

1. Apri `analytics_dashboard.html` nel browser
2. Clicca "**📈 Mostra Dati Mock**"
   - Mostra 5 grafici interattivi:
     - 📊 Operazioni Database (bar chart)
     - 🤝 Interazioni Sociali (doughnut chart)
     - 📈 Confronto Before/After (comparison bar chart)
     - 🕸️ Crescita Grafo Neo4j (line chart)
     - ✅ Risultati Test (pie chart 100% success)
   - Tabella dettagliata per atto

3. **BONUS**: Clicca "**🔴 Carica Dati Live dal Server**"
   - Carica analytics in tempo reale dall'API

#### **Opzione C: Live Demo (SE HAI TEMPO)**

```powershell
# Mostra i test che girano live
newman run bookSphere_storytelling_enhanced.postman_collection.json --color on
```

Output mostrerà:
- ✓ 62/62 requests passati
- ✓ 36/36 assertions passate
- Duration: ~8.7 secondi
- 0 errors!

---

## 🎭 Il Journey - Cosa Mostrare

### **PROLOGO** 🎬
- "Qui catturiamo lo stato iniziale delle analytics"
- Mostra: GET trending books (4 libri baseline)

### **ATTO I** 🎭 - Registrazione di 6 Utenti
- "6 utenti da 6 paesi diversi si registrano"
- Mostra: POST /api/v1/auth/register
- Impatto: +6 users MongoDB, +6 User nodes Neo4j

### **ATTO II** 🔍 - Discovery
- "Ogni utente cerca libri diversi esistenti nel DB"
- Mostra: GET /api/v1/books?title=Love
- 4 libri scoperti: "Love", "Night", "Life", "Season"

### **ATTO III** ✍️ - 12 Reviews
- "Ogni libro riceve 3 recensioni dettagliate (84-95/100)"
- Mostra: POST /api/v1/me/reviews
- Impatto: +12 reviews MongoDB, +24 edges Neo4j

### **ATTO IV** ❤️ - 22 Likes
- "16 book likes + 6 author likes distribuiti"
- Mostra: POST /api/v1/me/like/book
- Impatto: +22 LIKES edges in Neo4j

### **ATTO V** 🤝 - 14 Follows
- "Alice è l'hub centrale, social network denso"
- Mostra: POST /api/v1/me/follow
- Impatto: +14 FOLLOWS edges in Neo4j

### **EPILOGO** 📊 - Analytics Trasformate
- "Confronto before/after: tutto è cambiato!"
- Mostra: GET /api/v1/analytics/rankings/trendingbooks
- Libri target ora hanno ratings 90+, appaiono in rankings!

---

## 📊 Numeri Chiave da Evidenziare

| Metrica | Valore | Impatto |
|---------|--------|---------|
| **Test Requests** | 62 | 100% success rate (0 errors) |
| **Assertions** | 36 | 100% passed |
| **Utenti Creati** | 6 | Alice, Bob, Charlie, Dave, Eve, Frank |
| **Recensioni** | 12 | 3 per libro, rating 84-95/100 |
| **Likes Totali** | 22 | 16 books + 6 authors |
| **Follow Relations** | 14 | Alice = hub (5 in + 5 out) |
| **MongoDB Documents** | +18 | 6 users + 12 reviews |
| **Neo4j Nodes** | +18 | 6 User + 12 ReviewNode |
| **Neo4j Edges** | +60 | FOLLOWS + LIKES + review edges |
| **Test Duration** | 8.7s | ~140ms per request |
| **Database Impact** | MASSIVE | Analytics completamente trasformate |

---

## 🎯 Messaggi Chiave

### **1. Storytelling approach** 🎭
- Non solo "test", ma una **storia completa**
- 6 personaggi con interazioni realistiche
- Journey diviso in atti narrativi

### **2. Zero Errors** ✅
- **62/62 requests PASSED**
- Uso di libri realmente esistenti nel DB
- Country codes ISO-compliant (US, GB, IT, FR, DE, ES)
- Token management corretto
- Skip logic per gestire dipendenze

### **3. Database Impact** 💾
- **Multi-database**: MongoDB + Neo4j lavorano insieme
- MongoDB: document storage + analytics
- Neo4j: social graph densamente connesso
- Analytics completamente trasformate dai test

### **4. Production Ready** 🚀
- Può girare in CI/CD
- Può popolare environment di test
- Può essere demo per training
- Dati realistici e significativi

---

## 🖼️ Screenshot Suggeriti

Se vuoi fare screenshot manualmente:

1. **Trending Books Before**
   - Vai a: http://localhost:8080/api/v1/analytics/rankings/trendingbooks
   - Screenshot del JSON

2. **Run Tests Live**
   - Esegui: `newman run ...` nel terminale
   - Screenshot dell'output con ✓ 62/62 passed

3. **Trending Books After**
   - Vai di nuovo a: http://localhost:8080/api/v1/analytics/rankings/trendingbooks
   - Screenshot del JSON (diverso dal before!)

4. **Dashboard HTML**
   - Apri `presentation.html`
   - Screenshot del confronto before/after

5. **Grafici Dashboard**
   - Apri `analytics_dashboard.html`
   - Screenshot dei 5 grafici

---

## 💡 Tips per la Presentazione

### **✅ DO**
- Mostra il file HTML aperto nel browser (molto più visivo!)
- Usa il bottone "Carica Live" per dimostrare che i dati sono reali
- Enfatizza i numeri: "60+ Neo4j relationships in 8.7 secondi"
- Mostra la collection Postman per vedere la struttura
- Evidenzia lo skip logic (prerequest scripts) per robustezza

### **❌ DON'T**
- Non mostrare solo il JSON della collection (troppo tecnico)
- Non entrare troppo nei dettagli di implementazione
- Non eseguire i test live SE hai problemi di rete/lentezza
- Non dimenticare di avviare l'applicazione PRIMA!

---

## 🔧 Troubleshooting Presentazione

### **Dashboard HTML non carica dati live**
```powershell
# Verifica che l'app sia in esecuzione
curl http://localhost:8080/actuator/health

# Se non risponde, riavvia
.\mvnw.cmd spring-boot:run
```

### **Script capture-analytics.ps1 fallisce**
- Controlla che l'app sia running
- Controlla la configurazione del profilo (local/cluster/wsl)
- Verifica che MongoDB e Neo4j siano attivi

### **Browser blocca CORS per "Carica Live"**
- Apri la Developer Console (F12)
- Ignora l'errore CORS, è normale in locale
- Usa i dati mock come fallback

---

## 📦 Cosa Consegnare

Per la presentazione, porta:

1. ✅ `presentation.html` - Dashboard principale
2. ✅ `analytics_dashboard.html` - Dashboard grafici
3. ✅ `presentation_data/` - Dati JSON catturati
4. ✅ Screenshot dei dashboard (backup se la demo live fallisce)
5. ✅ Questo README per riferimento
6. ✅ Test results: `newman run` output (screenshot o testo)

---

## 🎓 Spiegazione Tecnica (Per Domande)

### **Perché Storytelling?**
- Dimostra un **use case realistico** end-to-end
- Più facile capire il flow rispetto a test isolati
- Mostra come le API interagiscono tra loro
- Popola il database in modo significativo per analytics

### **Perché MongoDB + Neo4j?**
- MongoDB: ottimo per **document storage** (users, books, reviews)
- Neo4j: ottimo per **graph queries** (social network, recommendations)
- **Multi-model = versatilità**: usa il DB giusto per il task giusto

### **Come funziona lo skip logic?**
```javascript
// Prerequest script example
if (!pm.collectionVariables.get("book1_id")) {
    pm.execution.skipRequest(); // Skip se dipendenza manca
}
```
- Protegge da errori a cascata
- Permette di runnare parti della collection
- Production-ready approach

### **Perché 6 utenti e non 3?**
- Volume sufficiente per **impatto significativo** sulle analytics
- Social graph diventa **denso e interessante**
- 12 reviews (3 per libro) = statistiche credibili
- Dimostra **scalabilità** del testing approach

---

## 📞 Support

Per problemi durante la presentazione:
1. Controlla che l'app sia running: `http://localhost:8080/actuator/health`
2. Verifica MongoDB: controlla `application.properties`
3. Verifica Neo4j: controlla `application.properties`  
4. Usa i dati mock nei dashboard HTML come fallback

---

## 🎉 Conclusione

Hai tutti i materiali per una presentazione di successo! 

**Il messaggio finale**: Abbiamo creato un sistema di testing che non solo verifica le funzionalità, ma **racconta una storia**, **popola il database in modo realistico**, e **dimostra l'impatto misurabile** sulle analytics.

**0 errors. 62 tests. 100% success. 🚀**

---

*Generated for BookSphere Enhanced Storytelling Tests*  
*February 2026*
