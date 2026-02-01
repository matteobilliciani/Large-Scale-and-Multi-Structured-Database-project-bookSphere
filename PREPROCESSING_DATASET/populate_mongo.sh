#!/bin/bash

# Script per popolare MongoDB con i dati di BookSphere
# Assicurati che MongoDB sia in esecuzione prima di lanciare questo script

# Database name
DB_NAME="booksphere"

# Percorso ai file JSONL
DATA_DIR="DATASET/MONGODB"

echo "==========================================="
echo "Popolamento MongoDB - BookSphere"
echo "==========================================="

# Drop database se esiste (per un fresh start)
echo "Eliminazione database precedente (se esiste)..."
mongosh --eval "use $DB_NAME" --eval "db.dropDatabase()"

echo ""
echo "Importazione collezioni..."

# Importa books
echo "[1/4] Importazione books..."
mongoimport --db $DB_NAME --collection books --file "$DATA_DIR/books.jsonl"
if [ $? -eq 0 ]; then
    echo "✓ Books importati con successo"
else
    echo "✗ Errore nell'importazione di books"
    exit 1
fi

# Importa users
echo "[2/4] Importazione users..."
mongoimport --db $DB_NAME --collection users --file "$DATA_DIR/users.jsonl"
if [ $? -eq 0 ]; then
    echo "✓ Users importati con successo"
else
    echo "✗ Errore nell'importazione di users"
    exit 1
fi

# Importa reviews
echo "[3/4] Importazione reviews..."
mongoimport --db $DB_NAME --collection reviews --file "$DATA_DIR/reviews.jsonl"
if [ $? -eq 0 ]; then
    echo "✓ Reviews importati con successo"
else
    echo "✗ Errore nell'importazione di reviews"
    exit 1
fi

# Importa authors
echo "[4/4] Importazione authors..."
mongoimport --db $DB_NAME --collection authors --file "$DATA_DIR/authors.jsonl"
if [ $? -eq 0 ]; then
    echo "✓ Authors importati con successo"
else
    echo "✗ Errore nell'importazione di authors"
    exit 1
fi

echo ""
echo "==========================================="
echo "Creazione indici per performance..."
echo "==========================================="

# Crea indici per migliorare le performance delle query
mongosh --eval "use $DB_NAME" <<EOF
// Indici per books
db.books.createIndex({ "title": 1 });
db.books.createIndex({ "author.id": 1 });
db.books.createIndex({ "genres": 1 });
db.books.createIndex({ "publication_year": 1 });
db.books.createIndex({ "trend_score.rating": -1 });

// Indici per users
db.users.createIndex({ "username": 1 }, { unique: true });
db.users.createIndex({ "email": 1 }, { unique: true });
db.users.createIndex({ "country": 1 });
db.users.createIndex({ "favorite_genres": 1 });

// Indici per reviews
db.reviews.createIndex({ "user_id": 1 });
db.reviews.createIndex({ "book_id": 1 });
db.reviews.createIndex({ "rating": 1 });
db.reviews.createIndex({ "created_at": -1 });
db.reviews.createIndex({ "likes_count": -1 });

// Indici per authors
db.authors.createIndex({ "name": 1 });
db.authors.createIndex({ "average_rating": -1 });

print("✓ Indici creati con successo");
EOF

echo ""
echo "==========================================="
echo "Verifica dati importati..."
echo "==========================================="

mongosh --eval "use $DB_NAME" <<EOF
print("Books: " + db.books.countDocuments());
print("Users: " + db.users.countDocuments());
print("Reviews: " + db.reviews.countDocuments());
print("Authors: " + db.authors.countDocuments());
EOF

echo ""
echo "==========================================="
echo "Importazione completata con successo!"
echo "==========================================="
echo ""
echo "Database: $DB_NAME"
echo "Per connetterti: mongosh --eval 'use $DB_NAME'"
echo ""
