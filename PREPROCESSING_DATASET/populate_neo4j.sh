#!/bin/bash

# Script veloce per popolare Neo4j usando LOAD CSV
# Molto più veloce del driver Python!

set -e

NEO4J_PASSWORD="password"
NEO4J_IMPORT_DIR="/var/lib/neo4j/import"
CSV_SOURCE_DIR="DATASET/NEO4J"

echo "=================================================="
echo "Popolamento Neo4j - BookSphere (FAST MODE)"
echo "=================================================="
echo ""

# 1. Copia i CSV nella cartella import di Neo4j
echo "[1/4] Copia CSV nella cartella import di Neo4j..."
sudo mkdir -p "$NEO4J_IMPORT_DIR"
sudo cp -v "$CSV_SOURCE_DIR"/*.csv "$NEO4J_IMPORT_DIR/"
sudo chmod -R 777 "$NEO4J_IMPORT_DIR"
echo "✓ CSV copiati"
echo ""

# 2. Pulizia database
echo "[2/4] Pulizia database..."
cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "MATCH (n) DETACH DELETE n;"
echo "✓ Database pulito"
echo ""

# 3. Creazione indici
echo "[3/4] Creazione indici..."
cypher-shell -u neo4j -p "$NEO4J_PASSWORD" <<EOF
CREATE INDEX user_mongoid IF NOT EXISTS FOR (u:User) ON (u.mongoId);
CREATE INDEX book_mongoid IF NOT EXISTS FOR (b:Book) ON (b.mongoId);
CREATE INDEX review_mongoid IF NOT EXISTS FOR (r:Review) ON (r.mongoId);
CREATE INDEX author_mongoid IF NOT EXISTS FOR (a:Author) ON (a.mongoId);
CREATE INDEX genre_name IF NOT EXISTS FOR (g:Genre) ON (g.name);
EOF
echo "✓ Indici creati"
echo ""

# 4. Import VELOCE con LOAD CSV
echo "[4/4] Importazione dati con LOAD CSV..."

echo "  - Users..."
cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "
LOAD CSV WITH HEADERS FROM 'file:///users.csv' AS row
CREATE (:User {
    mongoId: row.mongoId,
    username: row.username,
    country: row.country
});"

echo "  - Books..."
cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "
LOAD CSV WITH HEADERS FROM 'file:///books.csv' AS row
CREATE (:Book {
    mongoId: row.mongoId,
    title: row.title,
    year: toInteger(row.year)
});"

echo "  - Reviews..."
cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "
LOAD CSV WITH HEADERS FROM 'file:///reviews.csv' AS row
CREATE (:Review {
    mongoId: row.mongoId,
    rating: toInteger(row.rating),
    createdAt: datetime(row.createdAt)
});"

echo "  - Authors..."
cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "
LOAD CSV WITH HEADERS FROM 'file:///authors.csv' AS row
CREATE (:Author {
    mongoId: row.mongoId,
    name: row.name
});"

echo "  - Genres..."
cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "
LOAD CSV WITH HEADERS FROM 'file:///genres.csv' AS row
CREATE (:Genre {
    name: row.name
});"

echo "  - FOLLOWS..."
cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "
LOAD CSV WITH HEADERS FROM 'file:///follows.csv' AS row
MATCH (follower:User {mongoId: row.start_id})
MATCH (followed:User {mongoId: row.end_id})
CREATE (follower)-[:FOLLOWS {since: datetime(row.since)}]->(followed);"

echo "  - POSTED..."
cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "
LOAD CSV WITH HEADERS FROM 'file:///posted.csv' AS row
MATCH (u:User {mongoId: row.start_id})
MATCH (r:Review {mongoId: row.end_id})
CREATE (u)-[:POSTED]->(r);"

echo "  - REFER_TO..."
cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "
LOAD CSV WITH HEADERS FROM 'file:///refer_to.csv' AS row
MATCH (r:Review {mongoId: row.start_id})
MATCH (b:Book {mongoId: row.end_id})
CREATE (r)-[:REFER_TO]->(b);"

echo "  - WROTE..."
cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "
LOAD CSV WITH HEADERS FROM 'file:///wrote.csv' AS row
MATCH (a:Author {mongoId: row.start_id})
MATCH (b:Book {mongoId: row.end_id})
CREATE (a)-[:WROTE]->(b);"

echo "  - BELONGS_TO..."
cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "
LOAD CSV WITH HEADERS FROM 'file:///belongs_to.csv' AS row
MATCH (b:Book {mongoId: row.start_id})
MATCH (g:Genre {name: row.end_id})
CREATE (b)-[:BELONGS_TO]->(g);"

echo "  - LIKES_REVIEW..."
cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "
LOAD CSV WITH HEADERS FROM 'file:///user_likes_review.csv' AS row
MATCH (u:User {mongoId: row.start_id})
MATCH (r:Review {mongoId: row.end_id})
CREATE (u)-[:LIKES_REVIEW {timestamp: datetime(row.timestamp)}]->(r);"

echo "  - LIKES_BOOK..."
cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "
LOAD CSV WITH HEADERS FROM 'file:///user_likes_book.csv' AS row
MATCH (u:User {mongoId: row.start_id})
MATCH (b:Book {mongoId: row.end_id})
CREATE (u)-[:LIKES_BOOK {timestamp: datetime(row.timestamp)}]->(b);"

echo "  - LIKES_AUTHOR..."
cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "
LOAD CSV WITH HEADERS FROM 'file:///user_likes_author.csv' AS row
MATCH (u:User {mongoId: row.start_id})
MATCH (a:Author {mongoId: row.end_id})
CREATE (u)-[:LIKES_AUTHOR]->(a);"

echo "  - LIKES_GENRE..."
cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "
LOAD CSV WITH HEADERS FROM 'file:///user_likes_genre.csv' AS row
MATCH (u:User {mongoId: row.start_id})
MATCH (g:Genre {name: row.end_id})
CREATE (u)-[:LIKES_GENRE]->(g);"

echo ""
echo "=================================================="
echo "Verifica dati importati..."
echo "=================================================="

cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "
MATCH (u:User) RETURN 'Users' AS Type, count(u) AS Count
UNION
MATCH (b:Book) RETURN 'Books' AS Type, count(b) AS Count
UNION
MATCH (r:Review) RETURN 'Reviews' AS Type, count(r) AS Count
UNION
MATCH (a:Author) RETURN 'Authors' AS Type, count(a) AS Count
UNION
MATCH (g:Genre) RETURN 'Genres' AS Type, count(g) AS Count;"

echo ""
echo "=================================================="
echo "✅ Importazione completata con successo!"
echo "=================================================="
echo ""
