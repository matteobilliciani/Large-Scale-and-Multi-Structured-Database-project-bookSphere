


LARGE-SCALE

Colori:
-	Danilo Team Leader
-	Matteo Team Boxer
-	Matteo Team Donna

1.	Platform introduction
Welcome to BookSphere, the ultimate social platform for book lovers designed to help you organize your reading life and connect with a global community. Beyond simply searching for titles and authors, BookSphere allows you to curate your own digital library by marking books as "To-Read," "Reading," or "Read," ensuring you never lose track of your literary journey.
The experience is deeply social and smart: you can follow friends to instantly see their latest updates and ratings or discover "Real Influencers" - expert reviewers identified by the quality of their engagement rather than just follower count - to get the best recommendations for your favourite genres. The platform goes beyond standard suggestions by offering unique insights, such as a "Trending Probability" that predicts the next viral hit and an "Internationality Index" that shows you how far a book is traveling around the globe. You can share your own voice by leaving one-to-one-hundred ratings and written reviews, and at the end of every year, you’ll receive a personalized "Yearly Wrapped" recap to celebrate your reading highlights, top authors, and most-read genres.


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
12.	The System must allow the Registered User to see his friends
13.	The System must allow the Registered User to see on demand all the review of a book

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
1	Analytics
Queries

Functional Requirement	Main Database	Secondary	UML Entities involved	Notes
Search book				
Search user				
Search author				
				
How manage the Reviews for different purpose 
In order to ensure to Registered User to see the most recent e popular reviews for a given book and to load on demand every review of a certain book:
-	 They are partially embedded for popular and recent (we assume that when a user sees a book this are the first reviews seen)
-	They are all linked by their id (we assume that after given a book the user will demand to upload all the review remember limit count)

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

Document Indexes (da definire quando le query sono implementate)
Per ora GEMINI CONSIGLIA I SEGUENTI INDICI, e controllando hanno senso
// --- BOOKS COLLECTION ---
// Supporta Query 1, 3 e API Author
db.books.createIndex({ "author.name": 1 });
// Supporta Query 1d, 4 e API Genre Filtering (Multikey Index)
db.books.createIndex({ "genres": 1 });
// Supporta API Search e ordinamento alfabetico
db.books.createIndex({ "title": 1 }); 
// Opzionale: Se le query di Ranking usano spesso il trend_score pre-calcolato, secondo me eliminabile come indice
db.books.createIndex({ "trend_score.rating": -1 });
// --- USERS COLLECTION ---
// Supporta Query 2 e Login (Unique Constraints)
db.users.createIndex({ "username": 1 }, { unique: true });
db.users.createIndex({ "email": 1 }, { unique: true });
// --- REVIEWS COLLECTION ---
// Fondamentale per caricare le recensioni di un libro (ESR Pattern)
// Filtra per libro -> Ordina per data
db.reviews.createIndex({ "book_snapshot.book_id": 1, "created_at": -1 });
// Supporta la visualizzazione del profilo utente (tutte le recensioni di un utente)
db.reviews.createIndex({ "user_id": 1 });
 
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

| **4. Genre Influencer:** Identify "Real Influencers" in a genre—users whose reviews consistently receive high engagement rather than just high volume.

Identify influencer users for a specific genre
1.1.1.1	"The Real Influencer" (Quality over Quantity)
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


GRAPH indexes
GEMINI GIUSTAMENTE CONSIGLIA:
// --- 2. INDICI FUNZIONALI (Per le tue 5 Query) ---
// Servono per trovare istantaneamente il nodo da cui parte la query.

// Query 1: Parte da (u:User {username: "Alice"})
CREATE INDEX user_username_idx FOR (u:User) ON (u.username);

// Query 2 & 3: Partono da (b:Book {title: "..."})
// Nota: I titoli possono non essere unici, quindi usiamo INDEX, non CONSTRAINT
CREATE INDEX book_title_idx FOR (b:Book) ON (b.title);

// Query 2 & 5: Partono da (a:Author {name: "..."})
CREATE INDEX author_name_idx FOR (a:Author) ON (a.name);

// Query 4: Parte da (g:Genre {name: "Fantasy"})
// (Questo è ridondante se hai già il CONSTRAINT su g.name, ma lo metto per chiarezza. 
// Se hai il constraint sopra, questo darà un warning che esiste già, puoi ignorarlo).
//DIPENDE SE SU GENRE NAME SI METTE IL CONSTRAIN SUL NOME ESSENDO UNIVOCO

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
"availability": "ACTIVE",
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

  // PATTERN: Subset (le 3 più recenti per la card del libro)
  "recent_reviews_snapshot": [
    {
      "_id": ObjectId("65c1..."),
     “user_id”: ObjectId(“46…”),
      "username": "BookLover",
      "rating": 5,
      "summary": "Assolutamente incredibile...",
      "date": ISODate("2025-11-10T14:30:00Z")
    }
  ],

  // PATTERN: Subset (Le 3 recensioni con più like)
  "popular_reviews_snapshot": [
    {
      "_id": ObjectId("65c2..."),
“user_id”: ObjectId(“46…”),
      "username": "MarioRossi",
      "rating": 4,
      "num_of_like": 45, // Campo denormalizzato per ordinamento
      "summary": "Bello ma lungo...",
      "date": ISODate("2025-05-20T09:00:00Z")
    }
  ],

“reviews”:[ObjectId(), …]

  // PATTERN: Computed / Bucketing (Statistiche aggregate per anno)
  "stats_per_year": [
    {
      "year": 2023,
      "ratings_count": 8,
      "sum_rating": 630 // Accumulatore
    },
    {
      "year": 2024,
      "ratings_count": 10,
      "sum_rating": 900
    }
  ],

  // PATTERN: Current monthly score
  "month_score": {    “rating_count”: 1,
    “sum_rating”:91 ,
    "Current_Month”: “2025-12”
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
“reviews”:[ObjectId(), …]


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
//Opzionale
“isBanned”: “true”
  “username”: “Mario”
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
  “status”:”ACTIVE”
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
  "ratings_count": 862, // Totale voti ricevuti (Counter)
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

2	Restful API definition
API Endpoint Specification - BookSphere Platform
Queries + CRUD operations + analytics
Path variables: identifica una risorsa specifica (es. ricerche per ID)
Query string: filtra, ordina, cerca e pagina risultati di una query, con parametri anche opzionali
Le review sono accessibili sono tramite o profilo utente o libro, eccezion fatta per l’Admin che può cercarle.
Si noti che nella ricerca di altri utenti lo username è path variable in quanto univoco

Categoria Utente	Metodo	Endpoint	Input	Descrizione	Database Primario
Generic (Unregistered)	POST	/api/v1/auth/register	Username, mail, hashed password	Creazione di un nuovo account	MongoDB
X	POST	/api/v1/auth/login	Username|mail, password	Autenticazione utente	MongoDB
X	GET	/api/v1/books/{id}	pathVariable	Visualizza dettagli libro, snapshot recensioni e statistiche e lista di tutte le reviewID	MongoDB
	GET 	/api/v1/reviews?review=ID&review=ID…	Auth + query string con la lista di reviewID	Ottenere tutte le reviews di un libro o di un utente	Neo4j
X	GET	/api/v1/books?title = …	Query string	Ricerca il Libro dal titolo	MongoDB
X	GET	/api/v1/authors/{id}	Author’s Id	Visualizza profilo autore, opere pubblicate e rating	MongoDB
X	GET	/api/v1/authors?author_name = …	Query string	Ricerca Autore dal nome, opere pubblicate e rating	
X	GET	/api/v1/users/username/{username}	Path Variable 	Visualizza profilo utente e attività (bookshelf e reviews dell’anno e lista delle reviewID)	Mongo
X	GET	/api/v1/users/{id}	Path variable 	Ricerca utente per ID	
X	GET	/api/v1/analytics/rankings/trendingbooks		Lista di Libri di tendenza	MongoDB
X	GET	/api/v1/analytics/rankings/books?year = …	Query string 	Classifiche dei libri per un anno specifico o di sempre
	MongoDB
X	GET	/api/v1/analytics/rankings/books?year = …&author=…	Query string 	Classifiche dei libri per un anno specifico o di sempre di un certo genere	MongoDB
X	GET	/api/v1/analytics/rankings?genre=…year = …&genre=…	Query string	Classifiche dei libri per un anno specifico o di sempre di un certo autore	MongoDB
X	GET	/api/v1/analytics/rankings/authors	Query string	Classifiche degli autori di sempre	
X	GET	/api/v1/analytics/versatility/{authId}	Path variable	Calcola Author Versatility Index	Neo4j
X	GET	/api/v1/analytics/internationality/{book|authorID}	Path variable	Calcola Internationality Index (Book/Author)	Neo4j
X	GET	/api/v1/analytics/influencers?genre=…	Query string	Identifica influencer per genere (Engagement)	Neo4j
	GET	/api/analytics/revaluation		Identifica i libri con maggior divario di rating tra primo e ultimo anno	MongoDB
Registered User					
X	POST	/api/v1/me/reviews	Auth + bookid + voto + commento (optional)	Scrittura di una recensione (voto + commento)	Mongo+Neo4j
X	PATCH	/api/v1/me/reviews/{reviewID}	Auth + path variable + voto or commento (optional)	Modifica di una review postata precedentemente	Mongo+Neo4j
X	DELETE	/api/v1/me/reviews/{reviewID}	Auth + path variable + reviewID	Eliminazione di una recensione 	Mongo+Neo4j
X	POST	/api/v1/me/follow	Auth +  user’s id	Segui un altro utente	Neo4j
X	DELETE	/api/v1/me/unfollow/{userId}	Auth + path variable + username or other user’s id	Unfollow user	Neo4j
X	PATCH	/api/v1/me/username	Auth + new username	Cambia nome utente	MongoDB+Neo4j
X	POST	/api/v1/me/bookshelf	Auth + book id + status	Aggiunge libro alla to-read list o cambia status	MongoDB
X	PATCH	/api/v1/me/bookshelf/{bookID}	Auth + path variable + Status	Cambia stato di un libro	Mongo DB
X	DELETE	/api/v1/me/bookshelf /{bookId}	Auth + path variable	Elimina libro dalla lista	MongoDB
X	POST	/api/v1/me/like/book	Auth + book id	Metti "Like" a un libro	Neo4j
X	DELETE	/api/v1/me/unlike/book/{bookID}	Auth + path variable	Togli like al libro	Neo4j
X	POST	/api/v1/me/like/review	Auth + Review’s Id	Metti "Like" a una recensione	Neo4j+MongoDB
X	DELETE	/api/v1/me/unlikes/review/{reviewid}	Auth + path variable	Togli like a una review	Neo4j + MongoDB
	GET	/api/v1/me/liked/book	Auth 	Vedi I libri piaciuti	Neo4j
	GET	/api/v1/me/liked/author	Auth 	Vedi gli Autori piaciuti	Neo4j
	GET
	/api/v1/me/liked/review	Auth	Vedi le revies piaciute
	Neo4j
	GET	/api/v1/me/liked/genres	Auth	Vedi I generi piaciuti	Neo
	GET	/api/v1/me/friends	Auth	Vedi utenti seguiti	Neo
X	GET	/api/v1/me/recommendations	Auth	Suggerimenti basati su gusti e rete sociale	Neo4j
X	GET	/api/v1/me/wrapped	Auth	Genera lo Yearly Personal Recap (Wrapped)	MongoDB
X	POST	/api/v1/me/likes/genres	Auth + genre’s name	Metti like a un genere	Neo4j
X	DELETE	/api/v1/me/unlike/genres/{name}	Auth + path variable	Togli like a un genre	Neo4j
X<	POST	/api/v1/me/likes/authors	Auth + Author’s Id	Metti like all’autore	Neo4j
X	DELETE	/api/v1/me/unlike/authors/{authorID}	Auth + path variable	Togli like authore	Neo4j
X	DELETE	/api/v1/me/account	Auth	Rimozione account	Mongo + Neo

TO ADD: dato un utente restiturie:
lista di utenti seguiti
lista di libri/generi/review piaciute
Volendo anche solo ID.
Andrà fatta l’interazione per recuperare le info con Neo4j in questo caso.
Da un libro ottenere tutte le recensioni.
Administrator	POST	/api/v1/admin/books	Auth + corpo Book	Aggiunta di un nuovo libro al catalogo	MongoDB + Neo4j
X	PUT	/api/v1/admin/books/{id}	Auth + corpo book modificato + path variable	Aggiornamento informazioni libro(Only master update)	MongoDB + Neo4J
X	DELETE	/api/v1/admin/books/{id}	Auth + path variable	Rimozione di un libro dal sistema(Soft Delete)	MongoDB + Neo4J
X	DELETE	/api/v1/admin/reviews/{id}	Auth + path variable	Moderazione: elimina recensione offensiva	Mongo + Neo
X	PATCH	/api/v1/admin/users/{id}/ban	Auth + path variable + status banned	Ban di un utente dalla piattaforma set to “” in neo	MongoDB + NEO
X	POST	/api/v1/admin/authors	Auth + authors’ information fields	Inserisci autore	Mongo + neo
X	PUT	/api/v1/admin/authors/{id}	Auth + path variable + update author info	Aggiorna autor(Only Master Update)	Mongo + neo
X	POST	/api/v1/admin/genres	Auth + corpo genre	Inserisci nuovo genere	Neo 

NEL CODICE CI SONO ANCHE LE API PER RITORNARE TUTTI GLI USER E REVIEW LE TENIAMO?


3	Implementation
3.1	Model
3.1.1	Mongo
For each collection we have modelled in a single java class an entity.


3.1.2	Neo4j
For each node we have an entity, with their relation modelled inside each class representing the node.


3.2	JWT
3.2.1	Utils
-	JWTUtils per validare ed estrarre claims JWT.
-	UserPrincipal che rappresenta admin o user auntenticati.
-	SecurityUtils ritorna l’utente corrente , UserPrincipal che rappresenta admin o user auntenticati (Il JWT andrebbe passato tra le funzioni, lo userPrincipal è salvato nel contesto).
3.3	Config
-	securityConfig: configurazione ruolo per ogni endpoint 
-	jwtAuthFilter: filtro per la validazione dei token;
-	jwtAuthEntryPoint: gestione errori di auth (401);
3.4	DTO
-	AuthResponseDTO oggetto traferito in fase di register o login;
3.5	Application.properies
-	Chiave jwt e relativa scadenza (24h) specificata
Nel codice l’inserimento di uno user in neo4j è effettuato dentro una try ma per l’inserimento in mongo no.
1. Ruolo dei Database nel Sistema
•	MongoDB è il database primario per la persistenza degli utenti (profilo completo, password hashata, stato, ecc.). È essenziale per il funzionamento dell'autenticazione.
•	Neo4j è il database secondario per le relazioni grafiche (nodi utente per connessioni future, come amicizie o raccomandazioni). Non è critico per l'autenticazione di base, ma serve per mantenere la consistenza tra i due sistemi.
Se MongoDB fallisce, l'intera registrazione deve fallire (non ha senso creare un utente incompleto). Se Neo4j fallisce, possiamo "riparare" la situazione facendo rollback su MongoDB per evitare dati inconsistenti.
2. Perché Non c'è try-catch per MongoDB?
•	Propagazione dell'Eccezione: Se userRepository.save(user) fallisce (es. connessione persa, vincolo univoco violato, ecc.), l'eccezione viene lasciata propagare senza essere catturata. Questo è corretto perché:
o	Il metodo è annotato con @Transactional, quindi il transaction manager di Spring (per MongoDB) gestisce automaticamente il rollback se un'eccezione non catturata viene lanciata.
o	Non c'è bisogno di un try-catch manuale: l'eccezione interrompe il flusso e viene gestita a livello superiore (es. dal GlobalExceptionHandler nel controller, che restituisce un errore HTTP 500 o personalizzato).
•	Semplicità e Chiarezza: Aggiungere un try-catch attorno a MongoDB renderebbe il codice più verboso senza benefici reali. Se fallisce MongoDB, non possiamo procedere comunque, quindi è meglio lasciare che l'eccezione salga.
3. Perché c'è try-catch per Neo4j?
•	Gestione della Consistenza: Neo4j è opzionale ma critico per la coerenza. Se il salvataggio su Neo4j fallisce, il codice cattura l'eccezione e fa un rollback manuale cancellando l'utente appena creato in MongoDB (userRepository.delete(savedUser)). Questo garantisce che non rimangano "utenti fantasma" in MongoDB senza corrispondente in Neo4j.
•	Transazioni Separate: MongoDB e Neo4j usano transaction manager diversi (Spring Data MongoDB vs. Spring Data Neo4j). Il @Transactional copre solo MongoDB; per Neo4j, il controllo manuale è necessario per il rollback cross-database.
•	Logging e Recupero: Il catch permette di loggare l'errore specifico e rilanciare un'eccezione più chiara (RuntimeException), facilitando il debugging.

3.6	AUTHENTICATION: REGISTER e LOGIN
Nella Register l’operazione su mongodb non è in un try-catch mentre quella per neo4j sì perché
3.7	MAPPER
Implementato via MapStruct.
3.8	Spring Retry
Abilitato in tutta l’applicazione, metodo dichiarato che ritenta più volte l’esecuzione di certe operazioni sul db  in caso di fallimento.
3.8.1	SPRING-BOOT-STARTER-DATA-NEO4J instead of neo4j-java-driver
Lo starter include il driver e integra spring con neo4j gestendo automaticamente le connessioni con l’application-properties.

📋 ARCHITETTURA MANCANTE
Pattern Architetturali:
1.	Outbox Pattern - gestione eventual consistency
2.	Worker Pattern - task schedulati
3.	Processor Pattern - elaborazione asincrona
4.	Notification System - sistema di notifiche

4	CAP theorem
We to prioritize the availability (we need to discuss it).
On the primary we do the write, the read on the secondary.
Mongo DB automatically managed the eventual consistency (w=majority), when set the replicas will be acknowledged of the writes before committed. 

5	Eventual Consistency between Mongo and Neo4j
In order to guarantee the consistency between the DBs...
•	Retryable
•	getOrCreate: es. Dopo aver controllato che un utente esiste su MongoDB, su neo4j viene fatta get or create
•	application.preperties defines URLs for primary and secondary and W/R preferences, Mongo Library will correctly handle connections based on the operation done
•	Asynchronous consistency is implemented for the following operations
Operazione	Strict Consistency 	Eventual Consistency (asynchronous)
Create review	MONGO:
-	Add in review collection
-	Update book collection (LINKING + RECENT EMB.)
-	Update User collection (LINKING + YEAR EMB.)
NEO: add
-	Add node
-	Add relation post	MONGO:
-	 Update book collection (STATS MONTH(trending score)/YEAR/ALL)
-	Update Author collection (STATS)
Update Review	MONGO:
-	Review collection
-	Book collection (EMB.)
-	User collection (EMB.)
NEO:
-	Update rating	MONGO:
-	Update book statistics
-	Update author statistics
Delete Review	MONGO:
-	Review collection
-	Book collection (EMB & LINK)
-	User collection (EMB & LINK)
NEO:
-	Delete Node (and relations)	MONGO
-	Update Book stats
-	Update Author stats
Update username	MONGO:
-	User collection
NEO:
-	User node	MONGO
-	Review collection
-	Book collection (EMB.)
Add like to review	MONGO:
-	Review collection
-	Book collection (EMB. POPULAR) 
NEO:
-	Add like relation	
Delete like from review	MONGO:
-	Review collection
NEO:
-	Remove like relation 	MONGO
-	Update Book popular reviews (EMB.)  
Delete account
(così come per Banned, non sarà più accessibile il probilo, ma le interazioni che ha avuto rimangono accessibili nel sistema)	MONGO:
-	Delete User info (status deleted)
-	Update status to “deleted account”
NEO:
	Mongo: 
-	Eliminare il campo username da users e ovunque esso appaia 

Neo:
-	Eliminare username









Nota: Una volta eliminato l’account rimuovere ovunque il campo col nome utente eliminato.

Operazione	Strict Consistency (Sincrono - Immediato)	Eventual Consistency (Asincrono)
POST /admin/books


(Nuovo Libro)	MONGO:
- Insert in books collection.
NEO:
- Create (:Book) Node.
- Create Relation (:Author)-[:WROTE]->(:Book).	(Nessuna azione)
PUT /admin/books/{id}


(Update Info)	MONGO:
- Update Document Master in books collection.
NEO:
- Update property b.title sul nodo.	NESSUNA AZIONE.


(Le copie del titolo nelle review e bookshelf restano col vecchio nome come dato storico).
DELETE /admin/books/{id}


(Soft Delete)	MONGO:
- Set status: "ARCHIVED" in books collection.
NEO:
- DETACH DELETE nodo Libro (per bloccare raccomandazioni).	NESSUNA AZIONE.


(Il libro resta visibile nelle librerie utente e nelle review esistenti).
POST /admin/authors


(Nuovo Autore)	MONGO:
- Insert in authors collection.
NEO:
- Create (:Author) Node.	(Nessuna azione)
PUT /admin/authors/{id}


(Update Info)	MONGO:
- Update Document Master in authors collection.
NEO:
- Update property a.name sul nodo.	NESSUNA AZIONE.


(I nomi embedded nei libri e nelle review restano invariati).
DELETE /admin/authors/{id}


(Soft Delete)	MONGO:
- Set status: "ARCHIVED" in authors.
NEO:
- DETACH DELETE nodo Autore (o rimozione label).	NESSUNA AZIONE.
DELETE /admin/reviews/{id}


(Moderazione)	MONGO:
- Delete Document in reviews.
- $pull da review_ids e snapshots in books (Critico per rimuovere contenuti offensivi).
NEO:
- Detach Delete Node.	MONGO:


- Update stats in books e authors.
- $pull da review_ids e reviews_year in users.

(Qui la pulizia serve perché stiamo rimuovendo un contenuto tossico, non un'entità catalogo).
PATCH /admin/users/{id}/ban


(Ban Utente)	MONGO:
- Set status: "BANNED" in users.
NEO:
- Set label (:BannedUser).	MONGO (Cleanup):
- Set is_banned: true su reviews.
- $pull review dagli snapshots dei libri.


(Anche qui: pulizia necessaria per nascondere lo spammer).
POST /admin/genres
(Nuovo Genere)	NEO:
- Create (:Genre) Node.	(Nessuna azione)






Le relationship di neo4j non vengono modellate su Java perché non verranno utilizzate e verranno usate solo su neo4j per raccomandazioni ecc… . Le relationship vengono create tramite query mongo DB tramite CREATE ad esempio un LIKE ad un autore lo facciamo con

MATCH (u:User {mongoId: $userId})
        MATCH (a:Author {mongoId: $authorId})
        CREATE (u)-[r:LIKES {timestamp: $timestamp}]->(a)
RETURN r

Questo perché salvandoci le relationship su java quello che succedeva era che alcuni super nodi soprattutto i generi che erano collegati a tanti i libri riempivano la memoria della JVM.




Query Name	Logica di Business	Integrazione MongoDB (Backend Logic)	Cypher Query (Neo4j)
1. User Recommendations	Suggerisce libri non letti basandosi su:


1. Gusti utenti seguiti


2. Autori piaciuti


3. Generi piaciuti.



Logica Graph: Percorsi a 2-3 salti (FOLLOWS/LIKES, LIKES/WROTE, LIKES/BELONGS_TO).	PRE-QUERY (Blacklist):


1. Backend chiama db.users.findOne({username: $username}, {bookshelf: 1}).


2. Estrae i book_id con status "read".


3. Passa questa lista come parametro $excludedMongoIds a Neo4j.	MATCH (u:User {mongoId: $userId})
        OPTIONAL MATCH (u)-[:FOLLOWS]->(:User)-[:LIKES]->(b1:Book)
        OPTIONAL MATCH (u)-[:LIKES]->(:Author)-[:WROTE]->(b2:Book)
        OPTIONAL MATCH (u)-[:LIKES]->(:Genre)<-[:BELONGS_TO]-(b3:Book)
        WITH collect(b1) + collect(b2) + collect(b3) AS recommendations, u
        UNWIND recommendations AS book
        WITH u, book
        WHERE NOT EXISTS((u)-[:POSTED]->(:Review)-[:REFER_TO]->(book)) AND book IS NOT NULL
        WITH book, count(*) AS score
        ORDER BY score DESC
        LIMIT $limit
        MATCH (a:Author)-[:WROTE]->(book)
        RETURN book.mongoId AS bookId, 
            book.title AS title, 
            book.year AS publicationYear,
            score,
            collect(a.name) AS authors // Uso collect per evitare righe duplicate se ci sono più autori

2. Internationality Index	Calcola "quanto viaggia" un libro/autore analizzando la provenienza geografica di chi mette Like o scrive recensioni.		#### 2. Internationality Index (Book Travel)
MATCH (b:Book {mongoId: $bookId})
        OPTIONAL MATCH (b)<-[:REFER_TO]-(r:Review)<-[:POSTED]-(reviewer:User)
        OPTIONAL MATCH (b)<-[:LIKES]-(liker:User)
        WITH reviewer, liker
        WITH collect(DISTINCT reviewer) + collect(DISTINCT liker) AS users
        UNWIND users AS u
        WITH u WHERE u IS NOT NULL AND u.country IS NOT NULL
        RETURN u.country AS country, count(DISTINCT u.mongoId) AS uniqueUsers, count(*) AS totalInteractions
        ORDER BY uniqueUsers DESC

//AUTHOR
MATCH (a:Author {mongoId: $authorId})
        OPTIONAL MATCH (a)<-[:LIKES]-(liker:User)
        OPTIONAL MATCH (a)-[:WROTE]->(b:Book)<-[:REFER_TO]-(r:Review)<-[:POSTED]-(reviewer:User)
        WITH liker, reviewer
        WITH collect(DISTINCT liker) + collect(DISTINCT reviewer) AS users
        UNWIND users AS u
        WITH u WHERE u IS NOT NULL AND u.country IS NOT NULL
        RETURN u.country AS country, count(DISTINCT u.mongoId) AS uniqueUsers, count(*) AS totalInteractions
        ORDER BY uniqueUsers DESC


4. Genre Influencer	Identifica i "Veri Influencer" in un genere (Quality over Quantity).


Trova chi scrive review che ricevono molti Like in un dato genere.		//By a specific genre
MATCH (g:Genre {name: $genreName})<-[:BELONGS_TO]-(b:Book)<-[:REFER_TO]-(r:Review)<-[:POSTED]-(influencer:User)
        MATCH (r)<-[:LIKES]-(fan:User)
        WITH influencer,
             count(DISTINCT r) AS numReviews,
             count(fan) AS totalLikes
        WHERE numReviews > 3 AND u.username <> ""
        RETURN influencer.username AS username,
               totalLikes AS totalEngagement,
               numReviews AS numReviews,
               toFloat(totalLikes) / numReviews AS avgLikesPerReview
        ORDER BY avgLikesPerReview DESC
        LIMIT $limit

//By all genre
MATCH (r:Review)<-[:POSTED]-(influencer:User)
        MATCH (r)<-[:LIKES]-(fan:User)
        WITH influencer,
             count(DISTINCT r) AS numReviews,
             count(fan) AS totalLikes
        WHERE numReviews > 5 AND u.username <> ""

        RETURN influencer.username AS username,
               totalLikes AS totalEngagement,
               numReviews AS numReviews,
               toFloat(totalLikes) / numReviews AS avgLikesPerReview
        ORDER BY avgLikesPerReview DESC
        LIMIT $limit



WRAPPER DTO
{
  "stats": {
    "total_books_read": 12,
    "best_book": { "book": "Dune", "rating": 5, ... },
    "worst_book": { "book": "Twilight", "rating": 1, ... }
  },
  "top_authors": [
    { "_id": "Frank Herbert", "count": 3 },
    { "_id": "Isaac Asimov", "count": 2 }
  ],
  "top_genres": [
    { "_id": "Sci-Fi", "count": 5 },
    { "_id": "Fantasy", "count": 2 }
  ]
}

-Cambiato wrapper, ora usa le aggregation e mappa con mapper
-Cambiato trend score ora vede il mese corrente
-Fixato il ranking, ora usa aggregate e usa mapper+projection
-Eliminato in raccomandazione l'interazione con mongo
-aggiunto il ranking autore e per genere: per autore si potrebbe usare la lista di ID facendo un JOIN da applicazione
-aggiunti i relativi tes

-author ranking all time
-modificata la logica ranking
-aggiunto trend score, da cambiare nome
-testato
-eliminato ranking genere e relativo test
- AVRAGE ELIMINATION: i DTO li hanno ancora, i model no e tolte tutte le interazioni con gli average rating


TO DO
-popolamento ad hoc (popolandolo utilizzando le API)
- macchine virtuali Schiavo






6	Mongo DB Analytics
6.1	Main Analytics

Funzionalità	Obiettivo dell'Analisi	Query MongoDB / Metodo
Book Month Trend	Classifica libri in base alla viralità mensile.	
Author Ranking	Classifica autore of all time.	
Yearly Wrapped	Riepilogo annuale utente. In base a review year e bookshelf

Si usa unwind per contare la frequenza di certi autori e generi nelle year review altrimenti con group direttamente non funzionerebbe aggregando tutto insieme.	db.users.aggregate([
  // 1. MATCH: Filtra il singolo utente
  {
    "$match": {
      "_id": "USER_ID_CORRENTE" 
    }
  },

  // 2. ADDFIELDS: Pre-calcola i dati (filtra e ordina array interni)
  {
    "$addFields": {
      "best_book": {
        "$arrayElemAt": [
          {
            "$sortArray": {
              "input": { "$ifNull": ["$reviews_year", []] },
              "sortBy": { "rating": -1 } // DESC
            }
          },
          0
        ]
      },
      "worst_book": {
        "$arrayElemAt": [
          {
            "$sortArray": {
              "input": { "$ifNull": ["$reviews_year", []] },
              "sortBy": { "rating": 1 } // ASC
            }
          },
          0
        ]
      },
      "yearly_books": {
        "$filter": {
          "input": { "$ifNull": ["$bookshelf", []] },
          "as": "b",
          "cond": {
            "$and": [
              { "$eq": ["$$b.status", "read"] },
              { "$gte": ["$$b.added_at", ISODate("2026-01-01T00:00:00Z")] },
              { "$lte": ["$$b.added_at", ISODate("2026-12-31T23:59:59Z")] }
            ]
          }
        }
      }
    }
  },

  // 3. FACET: Esegue 3 analisi parallele sui dati calcolati sopra
  {
    "$facet": {
      // Analisi 1: Autori più letti
      "top_authors": [
        { "$project": { "yearly_books": 1 } },
        { "$unwind": "$yearly_books" },
        { 
          "$group": { 
            "_id": "$yearly_books.author.name", 
            "count": { "$sum": 1 } 
          } 
        },
        { "$sort": { "count": -1 } },
        { "$limit": 3 }
      ],

      // Analisi 2: Generi preferiti
      "top_genres": [
        { "$project": { "yearly_books": 1 } },
        { "$unwind": "$yearly_books" },
        { "$unwind": "$yearly_books.genres" },
        { 
          "$group": { 
            "_id": "$yearly_books.genres", 
            "count": { "$sum": 1 } 
          } 
        },
        { "$sort": { "count": -1 } },
        { "$limit": 3 }
      ],

      // Analisi 3: Metadati (Best/Worst e Totale)
      "meta": [
        {
          "$project": {
            "best_book": 1,
            "worst_book": 1,
            "total_books_read": { "$size": "$yearly_books" }
          }
        }
      ]
    }
  }
])

6.2	Other Analytics
Funzionalità	Obiettivo dell'Analisi	Query MongoDB / Metodo
Book Rankings	Classifica libri (Annuale/All-time).	db.books.aggregate([
    // 1. Initial Match
    { 
        "$match": { 
            "availability": "ACTIVE",
            // "author.id": {$id}, // (Optional)
         //
            // "genres": "Horror"             // (Optional)
        } 
    },

    // 2. Filter Array (Year Logic)
    {
        "$project": {
            "title": 1,
            "author": 1,
            "targetStat": {
                "$filter": {
                    "input": "$stats_per_year",
                    "as": "stat",
                    "cond": { "$eq": ["$$stat.year", 2023] }
                }
            }
        }
    },

    // 3. Extract Totals (Flattening)
    {
        "$project": {
            "title": 1,
            "author": 1,
            "totalRatings": { "$sum": "$targetStat.ratings_count" },
            "sumRating": { "$sum": "$targetStat.sum_rating" }
        }
    },

    // 4. Threshold Filter
    { 
        "$match": { "totalRatings": { "$gt": 5 } } 
    },

    // 5. Calculate Average & Rename fields for DTO
    {
        "$project": {
            "name": "$title",
            "additionalInfo": "$author.name",
            "totalRatings": 1,
            "averageRating": { "$divide": ["$sumRating", "$totalRatings"] }
        }
    },

    // 6. Final Sort & Limit
    { "$sort": { "averageRating": -1 } },
    { "$limit": 25 }
])
Revaluation	Libri con maggiore scarto tra primo anno di review e ultimo anno di review	db.books.aggregate([
    // 1. Filtra libri con almeno 2 anni di storico
    { $match: { "stats_per_year.1": { $exists: true }, {“availability”: “ACTIVE”} },

    // 2. Estrai il primo e l'ultimo oggetto statistico
    { $project: {
        title: 1,
        author: 1,
        firstStat: { $arrayElemAt: ["$stats_per_year", 0] },
        lastStat: { $arrayElemAt: ["$stats_per_year", -1] }
    }},

    // 3. Calcola le Medie (Safe Division: Sum / Count)
    { $project: {
        title: 1,
        author: "$author.name",
        startYear: "$firstStat.year",
        endYear: "$lastStat.year",
        
        // Calcolo Media Iniziale
        startRating: {
            $cond: [
                { $gt: ["$firstStat.ratings_count", 0] },
                { $divide: ["$firstStat.sum_rating", "$firstStat.ratings_count"] },
                0.0
            ]
        },

        // Calcolo Media Finale
        endRating: {
            $cond: [
                { $gt: ["$lastStat.ratings_count", 0] },
                { $divide: ["$lastStat.sum_rating", "$lastStat.ratings_count"] },
                0.0
            ]
        }
    }},

    // 4. Calcola il Delta (Finale - Iniziale)
    { $addFields: {
        ratingDelta: { $subtract: ["$endRating", "$startRating"] }
    }},

    // 5. Ordina e Limita
    { $sort: { ratingDelta: -1 } },
    { $limit: 10 }
]);
])

Yearly Wrapped	Riepilogo annuale utente. In base a review year e bookshelf

Si usa unwind per contare la frequenza di certi autori e generi nelle year review altrimenti con group direttamente non funzionerebbe aggregando tutto insieme.	db.users.aggregate([
  // 1. MATCH: Filtra il singolo utente
  {
    "$match": {
      "_id": "USER_ID_CORRENTE" 
    }
  },

  // 2. ADDFIELDS: Pre-calcola i dati (filtra e ordina array interni)
  {
    "$addFields": {
      "best_book": {
        "$arrayElemAt": [
          {
            "$sortArray": {
              "input": { "$ifNull": ["$reviews_year", []] },
              "sortBy": { "rating": -1 } // DESC
            }
          },
          0
        ]
      },
      "worst_book": {
        "$arrayElemAt": [
          {
            "$sortArray": {
              "input": { "$ifNull": ["$reviews_year", []] },
              "sortBy": { "rating": 1 } // ASC
            }
          },
          0
        ]
      },
      "yearly_books": {
        "$filter": {
          "input": { "$ifNull": ["$bookshelf", []] },
          "as": "b",
          "cond": {
            "$and": [
              { "$eq": ["$$b.status", "read"] },
              { "$gte": ["$$b.added_at", ISODate("2026-01-01T00:00:00Z")] },
              { "$lte": ["$$b.added_at", ISODate("2026-12-31T23:59:59Z")] }
            ]
          }
        }
      }
    }
  },

  // 3. FACET: Esegue 3 analisi parallele sui dati calcolati sopra
  {
    "$facet": {
      // Analisi 1: Autori più letti
      "top_authors": [
        { "$project": { "yearly_books": 1 } },
        { "$unwind": "$yearly_books" },
        { 
          "$group": { 
            "_id": "$yearly_books.author.name", 
            "count": { "$sum": 1 } 
          } 
        },
        { "$sort": { "count": -1 } },
        { "$limit": 3 }
      ],

      // Analisi 2: Generi preferiti
      "top_genres": [
        { "$project": { "yearly_books": 1 } },
        { "$unwind": "$yearly_books" },
        { "$unwind": "$yearly_books.genres" },
        { 
          "$group": { 
            "_id": "$yearly_books.genres", 
            "count": { "$sum": 1 } 
          } 
        },
        { "$sort": { "count": -1 } },
        { "$limit": 3 }
      ],

      // Analisi 3: Metadati (Best/Worst e Totale)
      "meta": [
        {
          "$project": {
            "best_book": 1,
            "worst_book": 1,
            "total_books_read": { "$size": "$yearly_books" }
          }
        }
      ]
    }
  }
])




Le Altre aggregation che facciamo sono:

LOCAL CLUSTER 
CLUSTER MONGODB

mkdir -p ~/mongo-cluster/data1 ~/mongo-cluster/data2 ~/mongo-cluster/data3
mkdir -p ~/mongo-cluster/logs

libera le porte
sudo systemctl stop mongod

per ogni scheda del terminale
1)
mongod --replSet "myReplicaSet" --port 27017 --dbpath ~/mongo-cluster/data1 --bind_ip localhost
2)
mongod --replSet "myReplicaSet" --port 27018 --dbpath ~/mongo-cluster/data2 --bind_ip localhost
3)
mongod --replSet "myReplicaSet" --port 27019 --dbpath ~/mongo-cluster/data3 --bind_ip localhost

quarto terminale si connette al primo nodo
mongosh --port 27017

inizializza il cluster
rs.initiate({
  _id: "myReplicaSet",
  members: [
    { _id: 0, host: "localhost:27017" },
    { _id: 1, host: "localhost:27018" },
    { _id: 2, host: "localhost:27019" }
  ]
})

e il prompt deve cambiare da "test>" a "myReplicaSet [direct: primary] >"






7	IMPLEMENTAZIONE
7.1	STRUTTURA CARTELLE
7.1.1	CONTROLLER E VARI END POINT
7.1.2	DTO
Descrizione DTO RITORNATI, non credo importi mettere la struttura, vedere altre documentazioni
7.1.3	MAPPER
Utili per mappare automaticamente i Model in DTO
7.1.4	REPOSITORY
In Repository nelle repository per Mongo e Neo4j troviamo anche Projection che servono per salvarsi dati intermedi ritornati dalle query più complesse.
Nelle repository NEO Abbiamo anche usato Query specifiche per alcune azioni tra cui le analytics o ritornare i libri piaciuti.
7.1.5	SERVICE
Impelmentazione Controller, descrizione breve del contenuto e nome file. Più cartella Async per i servizi asincroni
7.1.6	CONFIG, UTILS, VALIDATION & EXCEPTION
Bo scrivere a che servono
7.2	DESCRZIONE SERVICE GENERALE e SCELTE IMPELEMNTATIVE
Si utilizza @Transaction dove si scrive e @Retrayable per le richieste
NOTA: Quando si usa @Retryable bisogna stare attenti all’idempotenza.
Inoltre per le operazioni di aggiornamento abbiamo scelto di Usare alcune cose con @Async in modo da avere aggiornamenti in differita delle cose meno importanti come il voto medio nell’immediato andando a sfruttare l’eventual consistency.
Dato che i metodi Async devono essere chiamati da classi appostite sono state inseriti in una cartella apposita.
7.2.1	ADMIN
Operazioni di BAN User/Review e DELETE Book and AUTHOR come si sono gestite?
Le operazioni descritte sopra comportano una cancellazione dal graph DB per non infastidire le analytics e query con dati sporchi.
In mongo DB AUTHOR e BOOK vengono messi come ARCHIVED e non potranno più esserci interazioni con questi ultimi
In mongoDB User viene settato con status BANNED mentre la review viene eliminata da anche da tutti le cose embedded. Inoltre una volta bannato l’utente eliminiamo tutte le sue review come se le considerassimo tutte bannate.
7.2.2	ANALYTIC
Qui implementiamo le analytic usabili da utenti generici sia usando neo che mongoDB, in particolare:
-Internationality su book o author passando l’ID e AUTHOR o BOOK
-I veri influencer passando i generi e limit per fare paging, se non c’è generi fa real influencer generico
-I trending book in base al month score
-BookRankins in base ad anno, genere O(esclusivo) autore. L’autore è passato tramite nome. Inoltre essite una versione ottimizzata con un join lato applicativo che è resistente allo sharding.
-il ranking degli autori all time
-Book Revaluation
NOTA: Il ranking ha spesso limitazioni sul numero minimo di recensioni per evitare outlier.
7.2.3	AUTHOR & BOOK & USER
Serve per cercare libri e autori per id/nome

PAGINAZIONE
Differenza TOTAL: In Neo4j uso il count totale (PageImpl con total), mentre in MongoDB uso solo Page<T> che già include il total automaticamente - questo è corretto perché Spring Data MongoDB calcola il total automaticamente, mentre per Neo4j con query custom Cypher devo farlo manualmente.

7.2.4	BookShelf
Gestiure la propria bookshelf, tutto senza Async
7.2.5	LIKE
Gestire i like, messi e retrive cose a cui si è messo like. È gestito in modo asicrono l’embedding delle popular review.
7.2.6	FOLLOW
Gestione Following,
il follow di una persona che ha eliminato l’account rimane ma l’utente può eliminarlo
7.2.7	PROFILE
Un utente può modificare l’account cambiando nome o cancellandosi.
In caso di cambiamento nome l’update negli snapshot e nelle review viene fatto in modo asincrono. Mentre su NEO viene fatto subito
In caso di delete
L’utente viene messo con username null che non è disponibile altrimenti dovendo essere di almeno 3 caratteri e viene anche rimopssa l’email oltre che settato lo status a deleted.
Inoltre negli snapshot l’username diventa null in modo asincrono
Su NEO viene messo come nome “” così da poter essere differenziato ed escluso da attività come find real influencer ma ancora essere valido per le raccomandazioni. 

7.2.8	REVIEW
Creazione:
Asincrono l’aggiornamento delle statistiche di libro e autore viene fatto subito l’aggiornamento su NEO
Update:
Aggiornamento sincorno su NEO
Quando avviene l’aggiornamento di una review non viene contato viene sempre contata come fatta nel momento di creazione per quanto riguarda l’aggiornamento di tutte le statistiche.

7.2.9	USERFEATURE
Impelemnta l’analytic Wrapped

8	TEST
8.1	OPEN
Test Funzionali Esaustivi per le API OPEN
Nuovi Test Creati:
ReviewControllerTest (8 test)

Test per recupero singolo/multiplo di review per ID
Test per review inesistenti
Test per liste vuote e miste
UserControllerTest (9 test)

Test per recupero utenti per ID
Test per recupero utenti per username
Test per utenti inesistenti
Test per case sensitivity
Test per indipendenza tra utenti
AnalyticsControllerTest (26 test)

Test per trending books
Test per book rankings (con filtri per year, author, genre)
Test per author rankings
Test per book revaluation
Test per internationality (book e author)
Test per influencer detection
8.2	ADMIN
Riepilogo dei Test Funzionali ADMIN
Ho creato e migliorato test esaustivi per tutte le API ADMIN:
✅ AdminCatalogControllerTest (14 test - NUOVO)
•	✓ Cleanup e setup
•	✓ Add author (con normalizzazione nomi)
•	✓ Update author
•	✓ Delete/Archive author
•	✓ Test su autore archiviato (fallimenti attesi)
•	✓ Add genre (con normalizzazione)
•	✓ Gestione generi duplicati
•	✓ Consistenza MongoDB + Neo4j
✅ AdminBookControllerTest (13 test - MIGLIORATO)
•	✓ Add, update, delete book
•	✓ Cambio autore e generi
•	✓ Test su book archiviato
•	✓ Verifica relazioni Neo4j (WROTE, BELONGS_TO)
•	✓ Gestione errori (ID non esistenti)
•	✓ Cleanup automatico dati test
✅ AdminModerationControllerTest (14 test - MIGLIORATO)
•	✓ Delete review (moderazione)
•	✓ Ban user (con verifica cascata)
•	✓ Get all users/reviews (paginazione)
•	✓ Test con ID invalidi
•	✓ Creazione e pulizia dati test multipli
•	✓ Verifica autenticazione admin
📊 Totale: 41 test - 100% passati
Caratteristiche implementate:
•	Popolamento automatico database per test
•	Pulizia completa dati dopo ogni esecuzione
•	Test di consistenza MongoDB + Neo4j
•	Gestione normalizzazione nomi (case-insensitive)
•	Verifica errori e casi edge
•	Autenticazione admin configurata correttamente

LEGGI MD_COPILOT/TEST_SUITE_DOCUMENTATION
FUNCTIONAL TEST
Ok
STRESS TEST
-	Postman doesnt allow to stress the system enough
-	JMetric is a tool with GUI but it has high resources consumption, with big XML files
-	Gatling can be integrated in maven and allows optimized resources tests

9	INDEXES
9.1	MONGO DB
Sulla collection book ci sono due possibili indici:
-	Genres: così da velocizzare la ricerca di libri di un certo genere e il ranking dei libri di un certo genere
-	Title: abbiamo due possibilità:
•	Aggiungere un indice semplice ma la ricerca è ottimizzata solo se è fatta sull’inizio della parola
•	Aggiungere un filtro text e usare per le ricerche l operatore text che è un motore interno a mongo per la ricerca
Sulla collection author l’indice su nome stessa cosa di sopra per titolo dei libri.
9.2	NEO4J
On all the mongoID and on genre name for uniquess and search
