LARGE-SCALE

Colori:
-	Danilo Team Leader
-	Matteo Team Boxer
-	Matteo Team Donna

Platform introduction
BookSphere is a platform where users can search for any books, ranking, authors and reviews. They can also generate lists, write reviews and follow other Users.
On the BookSphere platform users can add books to their personal bookshelves or to-read list, rate and review books, and get suggestion based on their reading choices (fav. Authors, genres and friends’ taste) and reviews of previously read books.
Once users have added a new friend they will see what their friends have read and their rating about; even leaving a like to their favourite reviews.
The rating system goes from one to five, with the possibility of include the rating with a written review.







Requirements
Actors
The System serves three main categories of user:
o	Administrator: their job is to manage the platform. Their tasks range from updating the catalogues to moderating the users when needed.
o	Registered User: these are the users for whom the platform is designed. They create connections between each other, write reviews and search within the platform.
o	Unregistered User: users that can explore the platform as viewers but can’t interact with it.

Functional Requirements
Generic User
1.	The System must permit a User to search for a book, author or genre (different filters are available in the search).
2.	The System must allow any User to search other ones and view their activity (reviews and favourite books)
3.	The System must enable any User to read book’s reviews.
4.	The System must compute, for every newly added book, the Trending Probability Index (TPI) (i.e. how the book is likely to go viral).
5.	The System must compute an Author Versatility Index in terms of covered genre of his works.
6.	The System must enable an Unregistered User to create an account (sign-in).
7.	The System must compute the “How Far a Book Travels” (internationality) index, used to measure how much a book/author is spread across the globe.
8.	The System must generate different kind of rankings, for example: bestsellers; classics; most reviewed books etc…
9.	The System must calculate the average ratings for authors and books for every year.
10.	The System must find the trending books by genre/author/year; the ranking is based on the number of like of each book.

Registered User
1.	The System must provide suggestions based on the Registered User interaction with application (i.e. favourite genres, authors and books) and global trends.
2.	The System must allow a Registered User to review one or more books; the reviews are evaluations and an optional written comment.
3.	The System must enable a Registered User to follow others and view their newest activities.
4.	The System must enable a Registered User to add a book to the to-read-list.
5.	The System must enable a Registered User to create a list of books (either public or private).
6.	The System must enable a Registered User to like any book.
7.	The System must enable a Registered User to like others’ reviews.
8.	The System must rank a specified book using the average rating of itself.
9.	The System must allow each User to choose the book status, that range in to-read, reading, read.
10.	The System must enable a Registered User to view every publication of a specified author.
11.	The System must permit to a Registered User to Log In and Out the system
12.	The System must generate a Yearly Personal Recap for Registered User

Admin
1.	The System must enable an Admin to add a new book, author or genre.
2.	The System must enable an Admin to update the information related to a book, author or genre.
3.	The System must enable an Admin to remove a certain book, author or genre.
4.	The System must allow an Admin to view any Registered User.
5.	The System must enable an Admin to view any review.
6.	The System must enable an Admin to delete any review.
7.	The System must enable an Admin to ban any Registered User.
8.	The system must identify influencers inside the application
      Non-Functional Requirements
1.	The System must follow RESTful design principles
2.	The System must avoid permanent data loss
3.	The System must encrypt the Registered User’s password
4.	The System must be highly available and fault tolerant.
5.	The System must enforce Eventual Consistency between the Databases
      Analytics
      Queries

Functional Requirement	Main Database	Secondary	UML Entities involved	Notes


DocumentDB Queries (3)
1.	Compute the average ratings for an author/book.
      AvgRating totale e per anno fatto da: somma delle stelle e contatore delle recensioni. Va tenuto aggiornato per ogni review aggiunta con eventual consistency.
      a.	Rank books in descending rating order for a specified author/genre.
      b.	Find the highest rated books of a specific year, based on the reviews of that period (book publication year is not relevant).
2.	Generate yearly wrapped that includes
      a.	Highest and lowest rated books
      b.	Most read authors (from list)
      c.	Most read genres (from list)
3.	Popularity prediction of a book
      a.	Rating of the publications of the same author
      b.	Genre of the book appearance on rankings
      c.	(GRAPH) like to its books
4.	Trending Books/Genres/Authors
      a.	Bookshelves list of the users
      b.	Recent/popular reviews snapshots
      Document Indexes (da definire quando le query sono implementate)
      •	Book: titolo & autore (per ricerca)
      •	Book: genre & trending (per trending book)
      •	Book: stats_per_year (ranking)
      •	Review: sono già parzialmente embedded
      •	User: email (per il login)

GraphDB Queries (2)
Domain-specific	Graph-centric


1.	Generate User suggestions and recommendation based on similar books of
      a.	Followed users’ tastes
      b.	Followed authors
      c.	Likes to book and reviews
      d.	Favourite genres
      e.	Reviews done
2.	Calculate “How far a Book travels” (internationality index) (also af an author).
      a.	From where and how much likes were originated
      b.	From where and how much reviews were originated
3.	Find the most popular review for a specified book.
4.	Identify influencer users for a specific genre
2. "The Real Influencer" (Quality over Quantity)
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
5.

GRAPH indexes
•	Vincoli di unicità
•	Genres (per real influencers)




MIXED QUERY
1.	To Ban  (moderation) a REVIEW from DocDB and GraphDB
2.	Find authors that have written books of different genres (versatility).






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





























1)

NEW DOCUMENT
BOOK
{
"_id": ObjectId("65b3f..."),
"title": "The Fellowship of the Ring",
"publication_year": 1954,
"description": "A gripping read...",
"source": "amazon_master", // o "bookcrossing"
"bc_score": 150, // Punteggio popolarità usato per il ranking

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

USER
{
"_id": ObjectId("65d1..."),
"username": "User_12345",
"email": "user_12345@bx.com",
"password_hashed": "a3f5e...", // SHA-256
"country": "IT",
"joined_at": ISODate("2023-05-12T10:00:00Z"),
"status": "active",

// PATTERN: Computed (Generati analizzando le letture)
"favorite_genres": ["Science Fiction", "Thriller"],

// PATTERN: Bucket (Bookshelf gestita come array di oggetti stato)
"bookshelf": [
{
"book_id": ObjectId("65b3f..."),
"status": "read",
"added_at": ISODate("2023-06-01T09:00:00Z")
},
{
"book_id": ObjectId("88a1b..."),
"status": "want_to_read",
"added_at": ISODate("2025-12-15T18:30:00Z")
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


Il link tra REVIEW e USER/BOOK non è usato in modo relazionale ma per poter accedere facilmente alle review di un libro/utente
REVIEW
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


AUTHOR
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

