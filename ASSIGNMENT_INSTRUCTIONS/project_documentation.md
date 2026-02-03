LARGE-SCALE

Colori:
-	Danilo Team Leader
-	Matteo Team Boxer
-	Matteo Team Donna

Platform introduction
Welcome to BookSphere, the ultimate social platform for book lovers designed to help you organize your reading life and connect with a global community. Beyond simply searching for titles and authors, BookSphere allows you to curate your own digital library by marking books as "To-Read," "Reading," or "Read," ensuring you never lose track of your literary journey.
The experience is deeply social and smart: you can follow friends to instantly see their latest updates and ratings, or discover "Real Influencers"—expert reviewers identified by the quality of their engagement rather than just follower count—to get the best recommendations for your favourite genres. The platform goes beyond standard suggestions by offering unique insights, such as a "Trending Probability" that predicts the next viral hit and an "Internationality Index" that shows you how far a book is traveling around the globe. You can share your own voice by leaving one-to-one-hundred ratings and written reviews, and at the end of every year, you’ll receive a personalized "Yearly Wrapped" recap to celebrate your reading highlights, top authors, and most-read genres.


Requirements
Actors
The System serves three main categories of user:
o	Administrator: their job is to manage the platform. Their tasks range from updating the catalogues to moderating the users when needed.
o	Registered User: these are the users for whom the platform is designed. They create connections between each other, write reviews and search within the platform.
o	Unregistered User: users that can explore the platform as viewers but can’t interact with it.

Functional Requirements
Generic User
1.	The System must permit a User to search for a book and author (different filters are available in the search).
2.	The System must allow to search other ones and view their activity (reviews and favourite books) 
3.	The System must enable any User to read book’s reviews. 
4.	The System must compute, for every newly added book, the Trending Probability Index (TPI) (i.e. how the book is likely to go viral).
5.	The System must compute an Author Versatility Index in terms of covered genre of his works.
6.	The System must enable an Unregistered User to create an account (sign-in).
7.	The System must compute the “How Far a Book Travels” (internationality) index, used to measure how much a book/author is spread across the globe.
8.	The System must generate different kind of rankings, for example: books, authors for different years  
9.	The System must calculate the average ratings for authors and books for every year.
10.	The System must find trending books based on the current month reviews and ratings.
11.	The system must identify influencers inside the application


Registered User 
1.	The System must provide suggestions based on the Registered User interaction with application (i.e. favourite genres, authors and books) and global trends.
2.	The System must allow a Registered User to review one or more books; the reviews are evaluations and an optional written comment.
3.	The System must enable a Registered User to follow others and view their newest activities.
4.	The System must enable a Registered User to add a book to the to-read-list.
5.	The System must enable a Registered User to create a list of books
6.	The System must enable a Registered User to like any book.
7.	The System must enable a Registered User to like others’ reviews.
8.	The System must allow each User to choose the book status, that range in to-read, reading, read.
9.	The System must enable a Registered User to view every publication of a specified author.
10.	The System must permit to a Registered User to Log In and Out the system
11.	The System must generate a Yearly Personal Recap for Registered User

Admin
1.	The System must enable an Admin to add a new book, author or genre.
2.	The System must enable an Admin to update the information related to a book, author or genre.
3.	The System must enable an Admin to remove a certain book, author or genre.
4.	The System must allow an Admin to view any Registered User.
5.	The System must enable an Admin to view any review.
6.	The System must enable an Admin to delete any review.
7.	The System must enable an Admin to ban any Registered User.
Non-Functional Requirements
1.	The System must follow RESTful design principles
2.	The System must avoid permanent data loss
3.	The System must encrypt the Registered User’s password
4.	The System must be highly available and fault tolerant.
5.	The System must enforce Eventual Consistency between the Databases
Analytics
Queries

Functional Requirement	Main Database	Secondary	UML Entities involved	Notes
Search book				
Search user				
Search author				
				
				
				
				
				
				
				
				

DocumentDB Analytics (3)
Certamente. Ecco il documento unico, pronto per essere copiato nella tua documentazione o presentazione.
Ho strutturato il contenuto in due parti:
1.	Strategia & Ottimizzazioni: Una spiegazione concettuale per il professore (il "Perché").
2.	Implementazione Tecnica: Il codice esatto delle query (il "Come").
________________________________________
MongoDB Queries & Analytics Strategy
1. Strategia e Ottimizzazione Dati (Schema V11)
Per garantire massime performance in lettura ed evitare costose operazioni di JOIN ($lookup), abbiamo adottato i seguenti pattern NoSQL:
•	Extended Reference Pattern (Denormalizzazione):
o	Nella collezione users, la bookshelf contiene copie dei dati di Autore e Genere. Questo permette di generare il "Yearly Wrapped" leggendo un solo documento utente, senza unire la collezione libri.
o	Nella collezione reviews, i dati del libro (Titolo, Generi) sono embedded nel book_snapshot.
•	Bucket Pattern:
o	Nella collezione books, usiamo l'array stats_per_year per pre-aggregare i voti annuali. Questo rende le query di Ranking Storico e Ranking per Anno istantanee, evitando di scansionare milioni di recensioni.
•	Snapshot Pattern:
o	Nella collezione books, manteniamo recent_reviews_snapshot (ultimi 3 voti). Questo permette di calcolare il Trending Score Real-Time e il Momentum direttamente in memoria, senza query storiche.
________________________________________

Name	Description	Implementation
Books and author ranking	AvgRating totale e per anno fatto da: somma delle stelle e contatore delle recensioni. Va tenuto aggiornato per ogni review aggiunta con eventual consistency. 
a.	Rank books in descending rating order for a specified author/genre.
b.	Find the highest rated books of a specific year, based on the reviews of that period (book publication year is not relevant).
	Query 1: Ranking & Historical Analytics
Obiettivo: Classifiche basate su dati storici aggregati (Bucket Pattern).
1a. Ranking Libri per Autore (Media Storica Ponderata)
Calcola la media esatta sommando i totali annuali.
JavaScript
db.books.aggregate([
    { $match: { "author.name": "J.R.R. Tolkien" } },
    { $addFields: {
        // Calcolo media ponderata dai bucket annuali (Bucket Pattern)
        hist_avg: { 
            $cond: [
                { $eq: [{ $sum: "$stats_per_year.ratings_count" }, 0] }, 
                0, 
                { $divide: [{ $sum: "$stats_per_year.sum_rating" }, { $sum: "$stats_per_year.ratings_count" }] }
            ] 
        }
    }},
    { $sort: { hist_avg: -1 } },
    { $project: { title: 1, hist_avg: { $round: ["$hist_avg", 2] } } }
]);
1b. Top Libri dell'anno 2025
Estrae chirurgicamente i dati del 2025 senza $unwind (usando $filter).
JavaScript
db.books.aggregate([
    { $addFields: {
        // Estrazione dati 2025 senza esplodere l'array
        stats_25: { 
            $arrayElemAt: [{ $filter: { input: "$stats_per_year", as: "s", cond: { $eq: ["$$s.year", 2025] } } }, 0] 
        }
    }},
    { $match: { "stats_25.ratings_count": { $gte: 5 } } }, // Filtro significatività
    { $sort: { "stats_25.average_rating": -1 } },
    { $project: { title: 1, rating_2025: "$stats_25.average_rating" } },
    { $limit: 10 }
]);
1c. Top Autori dell'anno 2025
Aggrega i libri per trovare gli autori dominanti nell'anno corrente.
JavaScript
db.books.aggregate([
    { $addFields: {
        s25: { $arrayElemAt: [{ $filter: { input: "$stats_per_year", as: "s", cond: { $eq: ["$$s.year", 2025] } } }, 0] }
    }},
    { $match: { "s25": { $exists: true } } },
    { $group: {
        _id: "$author.name",
        avg_rating: { $avg: "$s25.average_rating" },
        total_votes: { $sum: "$s25.ratings_count" }
    }},
    { $match: { total_votes: { $gte: 10 } } }, 
    { $sort: { avg_rating: -1 } },
    { $limit: 5 }
]);
1d. Top Generi dell'anno 2025
Richiede $unwind sui generi per il conteggio statistico corretto.
JavaScript
db.books.aggregate([
    { $addFields: {
        s25: { $arrayElemAt: [{ $filter: { input: "$stats_per_year", as: "s", cond: { $eq: ["$$s.year", 2025] } } }, 0] }
    }},
    { $match: { "s25": { $exists: true } } },
    { $unwind: "$genres" }, 
    { $group: {
        _id: "$genres",
        avg_rating: { $avg: "$s25.average_rating" },
        books_count: { $sum: 1 }
    }},
    { $sort: { avg_rating: -1 } },
    { $limit: 5 }
]);
________________________________________
Yearly wrapped	Generate yearly wrapped that includes 
a.	Highest and lowest rated books
b.	Most read authors (from list)
c.	Most read genres (from list)
	Query 2: User Yearly Wrapped (2025)
Obiettivo: Statistiche personali (Top Autori, Top Generi, Best Book).
Ottimizzazione: Nessun JOIN grazie alla bookshelf arricchita. Uso di $facet per calcoli paralleli.
JavaScript
db.users.aggregate([
    { $match: { username: "User_12345" } },

    // 1. Best & Worst Book (da reviews_year embedded)
    { $addFields: {
        sorted_revs: { $sortArray: { input: "$reviews_year", sortBy: { rating: -1 } } }
    }},
    { $project: {
        best_book: { $first: "$sorted_revs" },
        worst_book: { $last: "$sorted_revs" },
        // 2. Filtro Bookshelf per l'anno 2025 in memoria
        books_2025: {
            $filter: {
                input: "$bookshelf", as: "b",
                cond: { $and: [
                    { $eq: ["$$b.status", "read"] },
                    { $gte: ["$$b.added_at", ISODate("2025-01-01T00:00:00Z")] },
                    { $lte: ["$$b.added_at", ISODate("2025-12-31T23:59:59Z")] }
                ]}
            }
        }
    }},
    
    // 3. Unwind necessario solo sui libri filtrati per contare le frequenze
    { $unwind: "$books_2025" },
    
    // 4. Calcolo Parallelo Autori e Generi
    { $facet: {
        "top_authors": [
            { $group: { _id: "$books_2025.author.name", count: { $sum: 1 } } },
            { $sort: { count: -1 } }, { $limit: 3 }
        ],
        "top_genres": [
            { $unwind: "$books_2025.genres" }, 
            { $group: { _id: "$books_2025.genres", count: { $sum: 1 } } },
            { $sort: { count: -1 } }, { $limit: 3 }
        ],
        "meta": [{ $limit: 1 }, { $project: { best_book: 1, worst_book: 1 } }]
    }}
]);
________________________________________
Popularity prediction	Popularity prediction of a book
a.	Rating of the publications of the same author
b.	Genre of the book appearance on rankings	Query 3: Author Popularity Prediction (Hybrid Model)
Obiettivo: Predire il trend futuro (Rising/Falling).
Logica: Combina la "Reputazione Storica" (60%) con il "Momentum Recente" (40%).
JavaScript
db.books.aggregate([
    { $match: { "author.name": "Stephen King" } },
    { $project: {
        title: 1,
        // A. Reputazione (Storica)
        historical: { 
            $cond: [{ $eq: [{ $sum: "$stats_per_year.ratings_count" }, 0] }, 0, 
            { $divide: [{ $sum: "$stats_per_year.sum_rating" }, { $sum: "$stats_per_year.ratings_count" }] }] 
        },
        // B. Momentum (Recente - Snapshot)
        momentum: { $avg: { $map: { input: "$recent_reviews_snapshot", as: "r", in: "$$r.rating" } } }
    }},
    // Gestione caso nessun dato recente (fallback sullo storico)
    { $addFields: { momentum: { $ifNull: ["$momentum", "$historical"] } } },
    { $group: {
        _id: "$author.name",
        avg_hist: { $avg: "$historical" },
        avg_mom: { $avg: "$momentum" }
    }},
    { $project: {
        // Formula Ibrida: 60% Storia, 40% Attualità
        prediction_index: { $add: [{ $multiply: ["$avg_hist", 0.6] }, { $multiply: ["$avg_mom", 0.4] }] },
        // Label Business Intelligence
        trend: { $cond: [{ $gte: ["$avg_mom", "$avg_hist"] }, "RISING 📈", "FALLING 📉"] }
    }}
]);
________________________________________
Trending Books	1.	Bookshelves list of the users
2.	Recent/popular reviews snapshots
	Query 4: Trending Score (Real-Time Discovery)
Obiettivo: Scoprire i trend attuali ignorando il passato.
Logica: Calcolo in memoria basato solo su recent_reviews_snapshot (ultimi 3 voti).
JavaScript
db.books.aggregate([
    // Considera solo libri con attività recente
    { $match: { "recent_reviews_snapshot.0": { $exists: true } } },
    { $project: {
        title: 1,
        genres: 1,
        // Media aritmetica snapshot recente
        recent_avg: { $avg: { $map: { input: "$recent_reviews_snapshot", as: "r", in: "$$r.rating" } } },
        volume: { $size: "$recent_reviews_snapshot" }
    }},
    // Boosting: Premia chi ha più recensioni recenti (Volume Factor)
    { $addFields: {
        score: { $multiply: ["$recent_avg", { $cond: [{ $gte: ["$volume", 3] }, 1.0, 0.8] }] }
    }},
    { $sort: { score: -1 } },
    { $limit: 10 }
]);

Document Indexes (da definire quando le query sono implementate)
 
NEO4J QUERIES

Domain Specific Formulation	Graph-centric formulation	Implementation
**1. User Recommendations:** Suggest books the user hasn't read yet based on the tastes of followed users, followed authors, and preferred genres.
Generate User suggestions and recommendation based on similar books of 
a.	Followed users’ tastes
b.	Followed authors
c.	Likes to book and reviews
d.	Favourite genres
e.	Reviews done
	Find `:Book` nodes reachable from a source `:User` via 2 or 3-hop paths (`FOLLOWS/LIKES`, `LIKES/WROTE`, `LIKES/BELONGS_TO`), excluding books already linked via `:POSTED->:Review->:REFER_TO`.	#### 1. Recommendation Engine
```cypher
MATCH (u:User {username: "Alice"})
// Path A: Books liked by people I follow
OPTIONAL MATCH (u)-[:FOLLOWS]->(:User)-[:LIKES]->(b1:Book)
// Path B: Books written by authors I like
OPTIONAL MATCH (u)-[:LIKES]->(:Author)-[:WROTE]->(b2:Book)
// Path C: Books belonging to genres I like
OPTIONAL MATCH (u)-[:LIKES]->(:Genre)<-[:BELONGS_TO]-(b3:Book)

WITH collect(b1) + collect(b2) + collect(b3) AS recommendations, u
UNWIND recommendations AS book
// Filter out books the user has already reviewed
WHERE NOT (u)-[:POSTED]->(:Review)-[:REFER_TO]->(book)
RETURN book.title, count(*) AS score
ORDER BY score DESC
LIMIT 10
```

**2. Internationality Index:** Calculate "how far a book/author travels" by analyzing the geographical distribution of likes and reviews.
Calculate “How far a Book travels” (internationality index) (also af an author).
a.	From where and how much likes were originated
b.	From where and how much reviews were originated
	Traverse incoming `:REFER_TO` and `:LIKES` relationships to a specific `:Book` or `:Author`, aggregating the `country` property of the source `:User` nodes.	#### 2. Internationality Index (Book/Author Travel)
```cypher
MATCH (target) 
WHERE (target:Book {title: "The Name of the Rose"}) OR (target:Author {name: "Umberto Eco"})
// Match users who interacted via Review or direct Like
MATCH (u:User)-[:POSTED|LIKES]->(interaction)
WHERE (interaction)-[:REFER_TO]->(target) OR interaction = target
RETURN u.country AS Country, 
       count(DISTINCT u) AS UniqueUsers, 
       count(interaction) AS TotalInteractions
ORDER BY UniqueUsers DESC
```

**3. Most Popular Review:** Identify the most impactful review for a specific book based on community engagement.
Find the most popular review for a specified book.
	Locate `:Review` nodes connected to a specific `:Book` via `:REFER_TO` and find the one with the highest in-degree of `:LIKES` relationships.	#### 3. Most Popular Review for a Book
```cypher
MATCH (b:Book {title: "1984"})<-[:REFER_TO]-(r:Review)
MATCH (r)<-[l:LIKES]-(u:User)
RETURN r.mongoId AS ReviewID, 
       r.rating AS Rating, 
       count(l) AS LikeCount,
       collect(u.username) AS LikedBy
ORDER BY LikeCount DESC
LIMIT 1
```

| **4. Genre Influencer:** Identify "Real Influencers" in a genre—users whose reviews consistently receive high engagement rather than just high volume.

Identify influencer users for a specific genre
"The Real Influencer" (Quality over Quantity)
Business Goal: "Chi sono gli utenti che dettano legge in un genere? Non chi ha più follower, ma chi scrive recensioni che ricevono più Like." Perché è Killer: Incrocia 4 entità diverse (Genre -> Book -> Review -> User).
Cypher
// Partiamo dal genere Fantasy
MATCH (g:Genre {name: "Fantasy"})<-[:BELONGS_TO]-(b:Book)<-[:REFER_TO]-(r:Review)
// Chi ha scritto la review?
MATCH (author:User)-[:POSTED]->(r)
// Chi ha messo like alla review?
MATCH (r)<-[:LIKES]-(fan:User)

WITH author, count(DISTINCT fan) as total_likes, count(DISTINCT r) as num_reviews
// Filtro per evitare chi ha avuto fortuna con una sola review
WHERE num_reviews > 1 
RETURN author.username, 
       total_likes, 
       (total_likes / num_reviews) as avg_likes_per_review
ORDER BY total_likes DESC
LIMIT 5
Cosa dire all'esame: "Identifichiamo gli Influencer basandoci sull'Engagement reale (Likes received) e non solo sulla topologia (Followers), segmentando per Genere."
	Multi-hop traversal: `Genre <- Book <- Review <- User`. Aggregate incoming `:LIKES` for those reviews and calculate the ratio of total likes to the number of reviews posted.	
#### 4. Genre Influencer (Quality over Quantity)
```cypher
MATCH (g:Genre {name: "Fantasy"})<-[:BELONGS_TO]-(b:Book)<-[:REFER_TO]-(r:Review)<-[:POSTED]-(influencer:User)
MATCH (r)<-[:LIKES]-(fan:User)
WITH influencer, 
     count(DISTINCT r) AS num_reviews, 
     count(fan) AS total_likes
// Filter for users with a minimum activity to ensure statistical relevance
WHERE num_reviews > 1
RETURN influencer.username AS Influencer, 
       total_likes AS TotalEngagement, 
       (toFloat(total_likes) / num_reviews) AS AvgLikesPerReview
ORDER BY AvgLikesPerReview DESC
LIMIT 5

Find authors that have written books of different genres (versatility).
		

GRAPH indexes
•	Vincoli di unicità
•	Genres (per real influencers)

 




WORKLOAD
A critical aspect considered when designing the system and database architecture is the expected workload profile. Given the social nature of the application, the system is designed to handle a Read-Heavy workload. (? OLTP -> OLAP ?)
Most operations (~80-85%) will be Reads, consisting of content consumption (browsing books), Social Graph queries, Analytics, and Authentication (Login).
Write operations (~15-20%) are further categorized based on frequency and complexity:
•	High-Frequency Writes (~10-15%): Lightweight interactions such as 'Likes' and 'Follows', which require low latency and can tolerate eventual consistency.
•	Low-Frequency Writes (<5%): Content creation tasks such as writing Reviews, rating books, and Administrative operations (CRUD on the catalogue)."


VOLENDO qua possiamo aggiungere i numeri di R/W attesi al secondo per poi fare i test


GEMINI CONSIGLIA
Consiglio Extra per l'Esame
Quando presentate questa slide o sezione, collegate subito questi numeri alle scelte architetturali richieste dal bando:
•	80% Reads: "Ecco perché abbiamo implementato i Replica Sets sul Document DB: per bilanciare il carico di lettura."
•	Analytics: "Dato che le Analytics sono pesanti ma sono solo letture, le eseguiremo su nodi secondari o dedicati per non bloccare l'utente."

SHARDING DISCUSSION
Since no partition must be implemented, one server is the primary one and others only contain replicas, explaining what is written above.
Discuss how sharding would improve this scenario.









DATASET
Book-Crossing: User review ratings
Amazon Books Reviews


Source	Description	Volume
goodbooks10k
Contains six million user ratings for the 10,000 books. It also includes books metadatas (title, author, etc.).	~ 90MB
Randomuser API
Dynamic creation of fake users and reviews. This API allows to generate fully fictional user data on demand.	Dynamic creation of fake users and reviews
Amazon Reviews dataset
Amazon review Dataset contains product reviews and metadata from Amazon, spanning May 1996 - July 2014.	~ 5GB

Variety: the data are collected from different sources such APIs, real reviews from Amazon, and randomly generated user data
Velocity/Variability: Old reviews (1 year) are not meaningful for some operations based on a specific period, such as the end year recap


Aggiungere degli utenti ad hoc per mostrare query specifiche: 
-	Wrapped (dell’anno corrente non lo fa fare)
-	Influencer (follower) vs. real influencer

DOCUMENT
Book	User	Review	Author
{
  "_id": ObjectId("65b3f..."),
  "title": "The Fellowship of the Ring",
  "publication_year": 1954,
  "description": "A gripping read...",
  "source": "amazon_master", // o "bookcrossing"

  // PATTERN: Extended Reference (Autore embeddato per evitare join in lettura)
  "author": {
    "id": ObjectId("65b3a..."),
    "name": "J.R.R. Tolkien"
  },

  // PATTERN: Attribute (Array semplice per query veloci sui generi)
  "genres": ["Fantasy", "Adventure", "Classic"],

  "external_ids": {
    "isbns": ["978-0547928210", "0345339703"] // Multipli ISBN (Amazon + BC)
  },

  // PATTERN: Subset (Le 3 recensioni più recenti per la card del libro)
  "recent_reviews_snapshot": [
    {
      "_id": ObjectId("65c1..."),
      "username": "BookLover",
      "rating": 5,
      "snippet": "Assolutamente incredibile...",
      "date": ISODate("2025-11-10T14:30:00Z")
    }
  ],

  // PATTERN: Subset (Le 3 recensioni con più like)
  "popular_reviews_snapshot": [
    {
      "_id": ObjectId("65c2..."),
      "username": "MarioRossi",
      "rating": 4,
      "num_of_like": 45, // Campo denormalizzato per ordinamento
      "snippet": "Bello ma lungo...",
      "date": ISODate("2025-05-20T09:00:00Z")
    }
  ],

  // PATTERN: Computed / Bucketing (Statistiche aggregate per anno)
  "stats_per_year": [
    {
      "year": 2023,
      "average_rating": 4.2,
      "ratings_count": 150,
      "sum_rating": 630 // Accumulatore
    },
    {
      "year": 2024,
      "average_rating": 4.5,
      "ratings_count": 200,
      "sum_rating": 900
    }
  ],

  // PATTERN: Computed (Score attuale per query "Trending Now")
  "trend_score": {
    "rating": 4.35,
    "updated_at": ISODate("2026-02-01T10:00:00Z")
  }
}
	{
  "_id": ObjectId("65d1..."),
  "username": "User_12345",
  "email": "user_12345@bx.com",
  "password_hashed": "a3f5e...", // SHA-256
  "country": "IT",
  "joined_at": ISODate("2023-05-12T10:00:00Z"),
  "status": "active",

  // PATTERN: Bucket (Bookshelf gestita come array di oggetti stato)
  "bookshelf": [
    {
      "book_id": ObjectId("65b3f..."),
      "status": "read",
      "added_at": ISODate("2023-06-01T09:00:00Z"),
     "title": "The Hobbit",
      "author": { "id": { "$oid": "..." }, "name": "J.R.R. Tolkien" }, 
     "genres": ["Fantasy", "Classic"]
    },
    {
      "book_id": ObjectId("88a1b..."),
      "status": "want_to_read",
      "added_at": ISODate("2025-12-15T18:30:00Z")
      "title": "Funk",
      "author": { "id": { "$oid": "..." }, "name": "pierino" }, 
     "genres": ["Fantasy”]
    }
  ],

  // PATTERN: Subset / Report (Solo le recensioni dell'anno corrente 2025)
  "reviews_year": [
    {
      "id": ObjectId("99a1..."), // ID Recensione
      "rating": 85,
      "book": "Dune" // Titolo denormalizzato
    }
  ]
}
	{
  "_id": ObjectId("99a1..."),
  "user_id": ObjectId("65d1..."),
  
  // Nota: book_id è rimosso dalla radice e spostato nello snapshot (scelta V5)
  
  "rating": 90, // Scala unificata 0-100
  "source": "amazon", // o "bookcrossing"
  "text": "Un libro che ti cambia la vita...",
  "summary": "Must read assoluto", // Solo per Amazon
  "created_at": ISODate("2025-08-20T15:30:00Z"),
  
  // PATTERN: Eventual Consistency (Aggiornato asincronamente rispetto al Grafo)
  "likes_count": 12, 
  "is_banned": false,

  // PATTERN: Subset (Dati minimi del libro per visualizzare la recensione nel feed utente)
  "book_snapshot": {
    "book_id": ObjectId("65b3f..."),
    "title": "The Fellowship of the Ring"
  }
}
	{
  "_id": ObjectId("65b3a..."),
  "name": "J.R.R. Tolkien",
  
  // PATTERN: Extended Reference (Lista dei libri pubblicati)
  "published_books": [
    {
      "_id": ObjectId("65b3f..."),
      "title": "The Fellowship of the Ring"
    },
    {
      "_id": ObjectId("65b40..."),
      "title": "The Two Towers"
    }
  ],

  // PATTERN: Computed (Aggregazioni pre-calcolate da tutte le recensioni dei suoi libri)
  "average_rating": 4.85,
  "ratings_count": 15000, // Totale voti ricevuti (Counter)
  "sum_ratings": 72750    // Somma voti (Accumulatore)
}



Il link tra REVIEW e USER/BOOK non è usato in modo relazionale ma per poter accedere facilmente alle review di un libro/utente

NEO4J
  



Nodi

Nodo (Label)	Proprietà	Tipo Dato (Neo4j)	Descrizione
User	mongoId	String	ID di collegamento con MongoDB.
	username	String	Nome visualizzato dell'utente.
	country	String	Codice nazione (es. "US", "IT").
Book	mongoId	String	ID di collegamento con MongoDB.
	title	String	Titolo del libro.
	year	Integer	Anno di pubblicazione (convertito con toInteger).
Review	mongoId	String	ID di collegamento con MongoDB.
	rating	Integer	Voto numerico (es. 0-100).
	createdAt	Datetime	Data e ora della recensione (convertito con datetime()).
Author	mongoId	String	ID di collegamento con MongoDB.
	name	String	Nome dell'autore.
Genre	name	String	Nome del genere (Chiave primaria per questo nodo).



Relationship

Relazione	Da ➔ A	Proprietà	Tipo Dato	Descrizione
FOLLOWS	User ➔ User	since	Datetime	Data in cui l'utente ha iniziato a seguire l'altro.
LIKES	User ➔ Review	timestamp	Datetime	Data esatta in cui è stato messo il "Mi piace".
LIKES	User ➔ Book	timestamp	Datetime	Data (derivata dalla recensione) dell'apprezzamento.
LIKES	User ➔ Genre	(Nessuna)	-	Indica un interesse statico/generale.
LIKES	User ➔ Author	(Nessuna)	-	Indica un interesse statico/generale.
WROTE	Author ➔ Book	(Nessuna)	-	Relazione strutturale pura.
BELONGS_TO	Book ➔ Genre	(Nessuna)	-	Relazione strutturale pura.
POSTED	User ➔ Review	(Nessuna)	-	Relazione strutturale pura.
REFER_TO	Review ➔ Book	(Nessuna)	-	Relazione strutturale pura.





```

Restful API definition
API Endpoint Specification - BookSphere Platform
Queries + CRUD operations + analytics

Categoria Utente	Metodo	Endpoint	Descrizione	Database Primario
Generic (Unregistered)	POST	/api/v1/auth/register	Creazione di un nuovo account	MongoDB
X	POST	/api/v1/auth/login	Autenticazione utente	MongoDB
X	GET	/api/v1/books/{id}	Visualizza dettagli libro e snapshot recensioni	MongoDB
X	GET	/api/v1/books/{title}	Ricerca il Libro dal titolo	MongoDB
X	GET	/api/v1/authors/{id}	Visualizza profilo autore e opere pubblicate	MongoDB
X	GET	/api/v1/authors/{name}		
X	GET	/api/v1/users/{username}	Visualizza profilo pubblico utente e attività	Mongo
X	GET	/api/v1/users/{id}		
X	GET	/api/v1/analytics/rankings/trendingbooks	Libri di tendenza	MongoDB
X	GET	/api/v1/analytics/rankings/books/{OPT:year}	Classifiche generali (classici, più letti, ecc.)
	MongoDB
X	GET	/api/v1/analytics/rankings/authors/{OPT:year}		
X	GET	/api/v1/ analytics/rankings/genres/{OPT:year}		
X	GET	/api/v1/analytics/tpi/{bookId}	Calcola il Trending Probability Index	MongoDB
X	GET	/api/v1/analytics/versatility/{authId}	Calcola Author Versatility Index	Neo4j
X	GET	/api/v1/analytics/internationality/{book/authorID}	Calcola Internationality Index (Book/Author)	Neo4j
X	GET	/api/v1/analytics/influencers	Identifica influencer per genere (Engagement)	Neo4j
Registered User				
	POST	/api/v1/me/reviews	Scrittura di una recensione (voto + commento)	Mongo+Neo4j
	PATCH	/api/v1/me/reviews/{reviewID}	Modifica di una review postata precedentemente	Mongo+Neo4j
	DELETE	/api/v1/me/reviews/{reviewId}	Eliminazione di una recensione 	Mongo+Neo4j
	POST	/api/v1/me/follow/{username}	Segui un altro utente	Neo4j
	DELETE	/api/v1/me/follow/{username}	Unfollow user	Neo4j
	PATCH	/api/v1/me/username/{new_username}	Cambia nome utente	MongoDB+Neo4j
X	POST	/api/v1/me/bookshelf /{bookid + status}	Aggiunge libro alla to-read list o cambia status	MongoDB
X	PATCH	/api/v1/me/bookshelf /{bookid}/{status}	Cambia stato di un libro	Mongo DB
X	DELETE	/api/v1/me/bookshelf /{bookid}	Elimina libro dalla lista	MongoDB
X	POST	/api/v1/me/likes/book/{id}	Metti "Like" a un libro	Neo4j
X	DELETE	/api/v1/me/likes/book/{id}	Togli like al libro	Neo4j
X	POST	/api/v1/me/likes/review/{id}	Metti "Like" a una recensione	Neo4j+MongoDB
X	DELETE	/api/v1/me/likes/review/{id}	Togli like a una review	Neo4j + MongoDB
	GET	/api/v1/me/recommendations	Suggerimenti basati su gusti e rete sociale	Neo4j
	GET	/api/v1/me/wrapped	Genera lo Yearly Personal Recap (Wrapped)	MongoDB
	POST	/api/v1/me/likes/genres/{name}		MongoDB + Neo4j
	DELETE	/api/v1/me/likes/genres/{name}		MongoDB + Neo4j
	POST	/api/v1/me/likes/authors/{name}		Neo4j
	DELETE	/api/v1/me/likes/authors/{name}		Neo4j
	DELETE	/api/v1/me/account	Rimozione account	Mongo + Neo


Administrator	POST	/api/v1/admin/books	Aggiunta di un nuovo libro al catalogo	MongoDB + Neo4j
	PUT	/api/v1/admin/books/{id}	Aggiornamento informazioni libro	MongoDB + Neo4J
	DELETE	/api/v1/admin/books/{id}	Rimozione di un libro dal sistema	MongoDB + Neo4J
	DELETE	/api/v1/admin/reviews/{id}	Moderazione: elimina recensione offensiva	Mongo + Neo
	PATCH	/api/v1/admin/users/{id}/ban	Ban di un utente dalla piattaforma	MongoDB
	POST	/api/v1/admin/authors	Inserisci autore	Mongo + neo
	PUT	/api/v1/admin/authors/{id}	Aggiorna autore	Mongo + neo
	DELETE	/api/v1/admin/authors/{id}	Elimina autore	Mongo + neo
	POST	/api/v1/admin/genres	Inserisci nuovo genere	Neo




Implementation
Model
Mongo
For each collection we have modelled in a single java class an entity.


Neo4j
For each node we have an entity, with their relation modelled inside each class representing the node.
