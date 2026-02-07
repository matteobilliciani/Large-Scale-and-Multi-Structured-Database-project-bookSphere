# MongoDB Analytics Test Documentation

## Panoramica

Il test `MongoDbAnalyticsPersistentTest` verifica il corretto funzionamento di tutte le API analytics che utilizzano MongoDB come sorgente dati. Il test è stato progettato seguendo lo stesso pattern del test Neo4j (`Neo4jAnalyticsPersistentTest`): i dati vengono creati **una sola volta** al primo run e **persistono** tra le esecuzioni successive.

## Strategia di Test: Persistent Data

### Motivazioni
- **Efficienza**: Evita la creazione ripetitiva di dati ad ogni esecuzione
- **Consistenza**: I dati rimangono gli stessi per tutti i test
- **Velocità**: Test più rapidi nelle esecuzioni successive
- **Realismo**: Simula meglio un ambiente di produzione con dati esistenti

### Comportamento
1. **Prima Esecuzione**: Controlla se i dati esistono. Se non esistono, li crea in MongoDB
2. **Esecuzioni Successive**: Riutilizza i dati esistenti senza ricrearli
3. **Nessun Cleanup**: I dati NON vengono cancellati al termine del test

---

## Creazione dei Dati di Test

### 1. Setup Utente

**Metodo**: `setupUser()`

Crea un singolo utente di test che verrà utilizzato per testare la funzionalità Yearly Wrapped.

```java
RegisterDTO dto = new RegisterDTO();
dto.setUsername("mongo_analytics_user");
dto.setPassword("Pass123!");
dto.setEmail("mongo_analytics_user@test.com");
dto.setCountry("IT");
testUserId1 = authService.register(dto).getId();
```

**Verifica Esistenza**: Controlla se esiste già un utente con username `mongo_analytics_user`

---

### 2. Setup Autori e Libri

**Metodo**: `setupAuthorsAndBooks()`

#### Autori Creati

| Autore | Caratteristiche | Scopo nel Test |
|--------|----------------|----------------|
| **MongoDB Test Author One** | Alta qualità (rating medio ~77-80) | Verificare ranking autori di successo |
| **MongoDB Test Author Two** | Media qualità (rating medio ~57-60) | Confronto con autore migliore |

> **⚠️ IMPORTANTE**: Tutti i rating nel dataset BookSphere sono su **scala 0-100**, non 0-5 come in alcuni sistemi di recensioni tradizionali.

#### Libri Creati

Il test crea **4 libri** con caratteristiche diverse per testare vari scenari analytics:

##### 📚 **Book 1: "MongoDB Analytics Book 1 - Trending"**
- **Autore**: MongoDB Test Author One
- **Anno Pubblicazione**: 2026
- **Generi**: `["MongoAnalytics", "TestFiction"]`
- **Caratteristiche**:
  - `month_score.rating`: **80.0** (alta attività corrente)
  - `month_score.rating_count`: **50** recensioni recenti
  - **Stats 2026**: rating 80.0, 50 recensioni, sum 4000
  - **Stats 2025**: rating 75.0, 30 recensioni, sum 2250

**Scopo**: Testare libri con **alta attività recente** per trending e TPI

---

##### 📗 **Book 2: "MongoDB Analytics Book 2 - Top Rated"**
- **Autore**: MongoDB Test Author One
- **Anno Pubblicazione**: 2025
- **Generi**: `["MongoAnalytics"]`
- **Caratteristiche**:
  - `month_score.rating`: **85.0** (eccellente)
  - `month_score.rating_count`: **20** recensioni recenti
  - **Stats 2026**: rating 85.0, 80 recensioni, sum 6800
  - **Stats 2025**: rating 82.0, 60 recensioni, sum 4920

**Scopo**: Testare **book rankings** - dovrebbe essere nei top risultati

---

##### 📘 **Book 3: "MongoDB Analytics Book 3 - Average"**
- **Autore**: MongoDB Test Author Two
- **Anno Pubblicazione**: 2025
- **Generi**: `["TestFiction"]`
- **Caratteristiche**:
  - `month_score.rating`: **60.0** (media)
  - `month_score.rating_count`: **15** recensioni recenti
  - **Stats 2026**: rating 60.0, 40 recensioni, sum 2400
  - **Stats 2025**: rating 55.0, 25 recensioni, sum 1375

**Scopo**: Testare **author rankings** - peggiore di Author One

---

##### 📙 **Book 4: "MongoDB Analytics Book 4 - Low Activity"**
- **Autore**: MongoDB Test Author Two
- **Anno Pubblicazione**: 2024
- **Generi**: `["MongoAnalytics", "TestFiction"]`
- **Caratteristiche**:
  - **NO month_score** (nessuna attività recente)
  - **Stats 2025**: rating 50.0, 10 recensioni, sum 500
  - Nessuna attività nel 2026

**Scopo**: Testare **TPI con bassa/nulla attività** - predizione "STABLE"

---

### 3. Setup Dati Utente per Yearly Wrapped

Il test popola anche i dati dell'utente per testare la funzionalità Yearly Wrapped:

#### Bookshelf (Libri Letti)
```java
BookshelfItem item1:
  - bookId: testBookId1
  - status: "read"
  - addedAt: Instant.now()
  - title: "MongoDB Analytics Book 1 - Trending"
  - author: MongoDB Test Author One
  - genres: ["MongoAnalytics", "TestFiction"]

BookshelfItem item2:
  - bookId: testBookId2
  - status: "read"
  - addedAt: Instant.now()
  - title: "MongoDB Analytics Book 2 - Top Rated"
  - author: MongoDB Test Author One
  - genres: ["MongoAnalytics"]
```

#### Reviews Year (Recensioni Annuali)
```java
ReviewYear review1:
  - id: "review1_[testBookId1]"
  - book: "MongoDB Analytics Book 1 - Trending"
  - rating: 100 (BEST - scala 0-100)

ReviewYear review2:
  - id: "review2_[testBookId3]"
  - book: "MongoDB Analytics Book 3 - Average"
  - rating: 30 (WORST - scala 0-100)
```

**Scopo**: Verificare che Yearly Wrapped identifichi correttamente best/worst books e top authors/genres

---

## Verifica dei Dati - Test Cases

### Test 1: Setup/Load Data
**Scopo**: Verificare che tutti i dati siano stati creati o caricati correttamente

**Verifiche**:
- ✅ `testUserId1` non null
- ✅ `testAuthorId1` e `testAuthorId2` non null
- ✅ Tutti i 4 `testBookId` non null

---

### Test 2: Trending Books
**API Testata**: `GET /api/analytics/trending-books`

**Query MongoDB**:
```javascript
{
  status: "ACTIVE",
  "month_score.rating_count": { $gte: 5 }
}
Sort: { "month_score.rating": -1 }
Limit: 20
```

**Verifiche**:
- ✅ Ritorna una lista non vuota
- ✅ I libri hanno alta attività nel mese corrente
- 📊 Book 1 e Book 2 dovrebbero comparire (hanno month_score elevati)

**Output Esempio**:
```
'MongoDB Analytics Book 2 - Top Rated' by MongoDB Test Author One (year=2025)
'MongoDB Analytics Book 1 - Trending' by MongoDB Test Author One (year=2026)
```

**Nota**: I rating nel dataset sono su scala **0-100**, non 0-5!

---

### Test 3: Book Rankings (All-Time)
**API Testata**: `GET /api/analytics/book-rankings`

**Logica**:
1. Recupera tutti i libri ACTIVE
2. Calcola rating medio da `statsPerYear` (tutti gli anni)
3. Filtra libri con > 5 recensioni totali
4. Ordina per rating decrescente

**Verifiche**:
- ✅ Almeno 1 libro nel risultato
- ✅ **Ordinamento corretto**: ogni libro deve avere rating ≥ del successivo
- 📊 Book 2 (rating 4.9) dovrebbe essere primo

**Output Esempio**:
```
'MongoDB Analytics Book 2 - Top Rated' by MongoDB Test Author One - rating=82.50, count=140
'MongoDB Analytics Book 1 - Trending' by MongoDB Test Author One - rating=77.50, count=80
```

---

### Test 4: Book Rankings by Year
**API Testata**: `GET /api/analytics/book-rankings?year=2026`

**Logica**:
1. Filtra `statsPerYear` per anno 2026
2. Usa solo le statistiche di quell'anno
3. Ordina per rating

**Verifiche**:
- ✅ Tutti i ranking hanno `year = 2026`
- 📊 Solo libri con statistiche 2026 compaiono

---

### Test 5: Author Rankings
**API Testata**: `GET /api/analytics/author-rankings`

**Logica**:
1. Per ogni libro, calcola rating medio dell'autore
2. Raggruppa per nome autore
3. Filtra autori con ≥ 10 recensioni totali
4. Ordina per rating medio

**Verifiche**:
- ✅ Almeno 1 autore nel risultato
- ✅ **Author 1 ranking > Author 2**: MongoDB Test Author One deve avere rating più alto
- 📊 Author One: ~77-80, Author Two: ~57-60

**Output Esempio**:
```
MongoDB Test Author One - rating=77.50, total_ratings=220
MongoDB Test Author Two - rating=57.50, total_ratings=75
```

---

### Test 6: Genre Rankings
**API Testata**: `GET /api/analytics/genre-rankings`

**Logica**:
1. Per ogni libro, distribuisce rating sui suoi generi
2. Raggruppa per genere
3. Filtra generi con ≥ 3 libri
4. Ordina per rating medio

**Verifiche**:
- ✅ Almeno 1 genere nel risultato
- ✅ I generi test compaiono: `MongoAnalytics` e `TestFiction`
- 📊 MongoAnalytics dovrebbe avere rating più alto (contiene Book 1 e Book 2)

**Output Esempio**:
```
MongoAnalytics - rating=73.33, book_count=3
TestFiction - rating=61.67, book_count=3
```

---

### Test 7: TPI Prediction - Rising Star
**API Testata**: `GET /api/analytics/tpi/{bookId}` con Book 1

**Algoritmo TPI**:
1. Calcola **Author Benchmark**: rating medio storico di tutti i libri dell'autore
2. Calcola **Book Momentum**: rating corrente del libro (month_score)
3. Confronta Momentum vs Benchmark:
   - Se `momentum > benchmark * 1.05` → 🚀 **RISING STAR**
   - Se `momentum < benchmark * 0.95` → 📉 **UNDERPERFORMING**
   - Altrimenti → ➡️ **STABLE**
   - Se `currentActivity == 0` → ⏸️ **STABLE (No recent data)**

**Verifiche per Book 1**:
- ✅ `bookId` corretto
- ✅ `authorBenchmark` calcolato (≈ 77.50)
- ✅ `bookMomentum` = 80.0 (da month_score)
- ✅ `currentActivity` = 50 (recensioni recenti)
- ✅ `prediction` contiene una delle etichette valide

**Output Esempio**:
```
Book: MongoDB Analytics Book 1 - Trending
Author Benchmark: 77.50
Book Momentum: 80.00
Prediction: ➡️ STABLE
Activity: 50
```

---

### Test 8: TPI Prediction - Stable/Low Activity
**API Testata**: `GET /api/analytics/tpi/{bookId}` con Book 4

**Caso Speciale**: Book 4 NON ha `month_score` (nessuna attività recente)

**Verifiche**:
- ✅ `currentActivity` = 0
- ✅ `bookMomentum` = 0.0
- ✅ `prediction` contiene **"STABLE"** (indica nessun dato recente)

**Output Esempio**:
```
Book: MongoDB Analytics Book 4 - Low Activity
Author Benchmark: 57.50
Book Momentum: 0.00
Prediction: ⏸️ STABLE (No recent data)
Activity: 0
```

---

### Test 9: Yearly Wrapped
**API Testata**: `GET /api/user/yearly-wrapped`

**Logica**:
1. Recupera utente corrente (autenticato)
2. Filtra `bookshelf` per item con `status = "read"` e `addedAt` nell'anno corrente
3. Identifica best/worst book da `reviewsYear` (max/min rating)
4. Calcola top 3 authors e top 3 genres per frequenza

**Verifiche**:
- ✅ `year` = 2026 (anno corrente)
- ✅ `totalBooksRead` ≥ 0
- ✅ **Best Book**: rating = 100 (Book 1 - scala 0-100)
- ✅ **Worst Book**: rating = 30 (Book 3 - scala 0-100)
- ✅ **Top Authors**: contiene "MongoDB Test Author One" (2 libri letti)
- ✅ **Top Genres**: contiene "MongoAnalytics" e "TestFiction"

**Output Esempio**:
```
Year: 2026
Books Read: 2
Best Book: 'MongoDB Analytics Book 1 - Trending' - rating 100
Worst Book: 'MongoDB Analytics Book 3 - Average' - rating 30
Top Authors:
  - MongoDB Test Author One (2 books)
Top Genres:
  - MongoAnalytics (2 books)
  - TestFiction (1 books)
```

---

## Struttura Dati MongoDB Utilizzata

### BookDocument
```javascript
{
  _id: ObjectId,
  title: String,
  publication_year: Number,
  author: {
    id: ObjectId,
    name: String
  },
  genres: [String],
  status: "ACTIVE",
  
  // Attività mese corrente
  month_score: {
    rating: Double,
    rating_count: Integer
  },
  
  // Statistiche per anno
  stats_per_year: [
    {
      year: Integer,
      average_rating: Double,
      ratings_count: Integer,
      sum_rating: Integer
    }
  ]
}
```

### RegisteredUser (per Wrapped)
```javascript
{
  _id: ObjectId,
  username: String,
  
  // Libreria personale
  bookshelf: [
    {
      book_id: ObjectId,
      status: "read" | "reading" | "want_to_read",
      added_at: ISODate,
      title: String,
      author: { id: ObjectId, name: String },
      genres: [String]
    }
  ],
  
  // Recensioni anno corrente
  reviews_year: [
    {
      id: String,
      book: String,
      rating: Integer
    }
  ]
}
```

---

## Pattern di Test Utilizzati

### 1. Verifica Esistenza Prima di Creare
```java
if (userRepository.findByUsername(TEST_USERNAME).isPresent()) {
    testUserId1 = userRepository.findByUsername(TEST_USERNAME).get().getId();
    return false; // Dati già esistenti
} else {
    // Crea nuovi dati
    return true; // Dati creati
}
```

### 2. Assertion Progressivi
```java
assertNotNull(result);
assertTrue(result.size() > 0, "Expected at least 1 ranking");
// Verifiche specifiche...
```

### 3. Ordinamento Verificato
```java
for (int i = 0; i < result.size() - 1; i++) {
    assertTrue(result.get(i).getRating() >= result.get(i + 1).getRating(),
        "Rankings should be sorted by rating descending");
}
```

### 4. Confronto Relativo
```java
assertTrue(author1Rank.getAverageRating() > author2Rank.getAverageRating(),
    "Author 1 should have higher rating than Author 2");
```

---

## Esecuzione Test

### Comando Maven
```bash
./mvnw test -Dtest=MongoDbAnalyticsPersistentTest
```

### Output Atteso
```
========================================
MongoDB Analytics Persistent Test
IMPORTANT: Data is created ONCE and persists
========================================

--- TEST 1: Setup/Load Test Data ---
✓ Created new test authors and books
✓ All test data loaded successfully

--- TEST 2: Trending Books ---
✓ Found 2 trending books
✓ PASSED: Trending books retrieved

[... altri test ...]

========================================
✓ ALL TESTS PASSED
Data persists for further testing
========================================

Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
```

---

## Conclusioni

Questo test fornisce una **copertura completa** delle funzionalità MongoDB Analytics:

✅ **Trending Books** - Identifica libri popolari correnti  
✅ **Book Rankings** - Classifica libri per qualità  
✅ **Author Rankings** - Confronta performance autori  
✅ **Genre Rankings** - Popolarità per genere  
✅ **TPI Prediction** - Predice trend futuri  
✅ **Yearly Wrapped** - Statistiche personalizzate utente  

I dati persistenti permettono test **veloci**, **consistenti** e **ripetibili**, simulando un ambiente di produzione realistico.
