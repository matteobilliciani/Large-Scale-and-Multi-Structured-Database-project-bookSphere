#!/bin/bash

# Configurazione Cluster
URI="mongodb://localhost:27017,localhost:27018,localhost:27019/?replicaSet=myReplicaSet"
DB_NAME="booksphere"

# Percorso ai file JSONL
DATA_DIR="PREPROCESSING_DATASET/DATASET/MONGODB"

echo "==========================================="
echo "Popolamento MongoDB Cluster - BookSphere"
echo "==========================================="

# Drop database se esiste (usando getSiblingDB per sicurezza)
echo "Eliminazione database precedente '$DB_NAME'..."
mongosh "$URI" --quiet --eval "db.getSiblingDB('$DB_NAME').dropDatabase()"

echo ""
echo "Importazione collezioni..."

import_collection() {
    local col=$1
    local file="$DATA_DIR/$2"
    echo "Importazione $col..."
    mongoimport --uri="$URI" --db "$DB_NAME" --collection "$col" --file "$file"
    if [ $? -eq 0 ]; then
        echo "✓ $col importati con successo"
    else
        echo "✗ Errore nell'importazione di $col"
        exit 1
    fi
}

import_collection "books" "books.jsonl"
import_collection "users" "users.jsonl"
import_collection "reviews" "reviews.jsonl"
import_collection "authors" "authors.jsonl"

echo ""
echo "==========================================="
echo "Creazione indici per performance..."
echo "==========================================="

# Nota: il comando 'use $DB_NAME' all'interno di EOF è fondamentale
mongosh "$URI" <<EOF
use $DB_NAME;

print("Creazione indici per books...");
db.books.createIndex({ "title": 1 });
db.books.createIndex({ "author.id": 1 });
db.books.createIndex({ "genres": 1 });
db.books.createIndex({ "publication_year": 1 });
db.books.createIndex({ "trend_score.rating": -1 });

print("Creazione indici per users...");
db.users.createIndex({ "username": 1 }, { unique: true });
db.users.createIndex({ "email": 1 }, { unique: true });
db.users.createIndex({ "country": 1 });
db.users.createIndex({ "favorite_genres": 1 });

print("Creazione indici per reviews...");
db.reviews.createIndex({ "user_id": 1 });
db.reviews.createIndex({ "book_id": 1 });
db.reviews.createIndex({ "rating": 1 });
db.reviews.createIndex({ "created_at": -1 });
db.reviews.createIndex({ "likes_count": -1 });

print("Creazione indici per authors...");
db.authors.createIndex({ "name": 1 });
db.authors.createIndex({ "average_rating": -1 });

print("✓ Tutti gli indici sono stati creati");
EOF

echo ""
echo "==========================================="
echo "Verifica dati importati..."
echo "==========================================="

mongosh "$URI" --quiet --eval "
const myDb = db.getSiblingDB('$DB_NAME');
print('Database: ' + myDb.getName());
print('Books:    ' + myDb.books.countDocuments());
print('Users:    ' + myDb.users.countDocuments());
print('Reviews:  ' + myDb.reviews.countDocuments());
print('Authors:  ' + myDb.authors.countDocuments());
"

echo ""
echo "==========================================="
echo "Importazione completata con successo!"
echo "==========================================="