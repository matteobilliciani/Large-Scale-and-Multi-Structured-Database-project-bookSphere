"""
SCRIPT 2: Neo4j Data Export - FINAL V9 (SMART GENRES & AUTHORS)
- Input: JSONL files from Script 1 (MongoDB).
- Output: Clean CSV files for Neo4j Import.
- Logic Changes:
    - LIKES_GENRE: Calculated from User's Bookshelf (Implicit Preferences).
    - LIKES_AUTHOR: Calculated from User's Bookshelf.
"""

import pandas as pd
import json
import random
from datetime import datetime, timedelta
from pathlib import Path
from collections import Counter # Necessario per contare i generi

# --- CONFIGURAZIONE ---
random.seed(42)

BASE_DIR = Path(__file__).resolve().parent
MONGO_INPUT = BASE_DIR / "DATASET" / "MONGODB"
NEO4J_OUTPUT = BASE_DIR / "DATASET" / "NEO4J"

# --- UTILS ---
def load_jsonl(filename):
    data = []
    path = MONGO_INPUT / filename
    if not path.exists():
        print(f"❌ ERRORE: {filename} non trovato in {MONGO_INPUT}.")
        exit(1)
    with open(path, 'r', encoding='utf-8') as f:
        for line in f:
            if line.strip(): data.append(json.loads(line))
    return data

def get_oid(obj):
    if isinstance(obj, dict) and '$oid' in obj: return obj['$oid']
    return str(obj)

def get_date(obj):
    if isinstance(obj, dict) and '$date' in obj: return obj['$date']
    return str(obj)

def random_date_after_iso(iso_date_str):
    try:
        clean_iso = iso_date_str.replace('Z', '')
        start = datetime.fromisoformat(clean_iso)
        now = datetime.now()
        if start >= now: return iso_date_str
        delta = now - start
        days_window = min(delta.days, 60)
        if days_window <= 0: return iso_date_str
        res = start + timedelta(days=random.randint(0, days_window))
        return res.isoformat() + 'Z'
    except:
        return iso_date_str

# --- MAIN ---
print("="*60 + "\nNEO4J CSV EXPORTER V9 (SMART GENRES)\n" + "="*60)

print("[1/4] Loading JSONL Data...")
books = load_jsonl('books.jsonl')
users = load_jsonl('users.jsonl')
reviews = load_jsonl('reviews.jsonl')
authors = load_jsonl('authors.jsonl')

NEO4J_OUTPUT.mkdir(parents=True, exist_ok=True)

# Map per lookup veloce: BookID -> Genres
book_genres_map = {}
for b in books:
    book_genres_map[get_oid(b["_id"])] = b.get("genres", [])

# --- 1. NODES ---
print("\n[2/4] Exporting Nodes CSV...")

# USERS
df_users = pd.DataFrame([{
    "mongoId": get_oid(u["_id"]),
    "username": u["username"],
    "country": u["country"]
} for u in users])
df_users.to_csv(NEO4J_OUTPUT / "users.csv", index=False)

# BOOKS
df_books = pd.DataFrame([{
    "mongoId": get_oid(b["_id"]),
    "title": b["title"],
    "year": b["publication_year"]
} for b in books])
df_books.to_csv(NEO4J_OUTPUT / "books.csv", index=False)

# REVIEWS
df_reviews = pd.DataFrame([{
    "mongoId": get_oid(r["_id"]),
    "rating": r["rating"],
    "createdAt": get_date(r["created_at"])
} for r in reviews])
df_reviews.to_csv(NEO4J_OUTPUT / "reviews.csv", index=False)

# AUTHORS
df_authors = pd.DataFrame([{
    "mongoId": get_oid(a["_id"]),
    "name": a["name"]
} for a in authors])
df_authors.to_csv(NEO4J_OUTPUT / "authors.csv", index=False)

# GENRES
genres_set = set()
for b in books: 
    for g in b.get("genres", []): genres_set.add(g)
all_genres_list = sorted(list(genres_set))
df_genres = pd.DataFrame([{"name": g} for g in all_genres_list])
df_genres.to_csv(NEO4J_OUTPUT / "genres.csv", index=False)

# --- 2. STRUCTURAL EDGES ---
print("\n[3/4] Exporting Structural Relationships...")

# WROTE (Author -> Book)
wrote_data = []
book_to_author_map = {} 
for b in books:
    if "author" in b and "id" in b["author"]:
        auth_id = get_oid(b["author"]["id"])
        bid = get_oid(b["_id"])
        wrote_data.append({"start_id": auth_id, "end_id": bid})
        book_to_author_map[bid] = auth_id

pd.DataFrame(wrote_data).to_csv(NEO4J_OUTPUT / "wrote.csv", index=False)

# BELONGS_TO (Book -> Genre)
belongs_data = []
for b in books:
    bid = get_oid(b["_id"])
    for g in b.get("genres", []):
        belongs_data.append({"start_id": bid, "end_id": g})
pd.DataFrame(belongs_data).to_csv(NEO4J_OUTPUT / "belongs_to.csv", index=False)

# POSTED & REFER_TO
posted_data = []
refer_data = []
for r in reviews:
    rid = get_oid(r["_id"])
    uid = get_oid(r.get("Userd_id", r.get("user_id")))
    bid = get_oid(r["book_snapshot"]["book_id"]) 
    posted_data.append({"start_id": uid, "end_id": rid})
    refer_data.append({"start_id": rid, "end_id": bid})
pd.DataFrame(posted_data).to_csv(NEO4J_OUTPUT / "posted.csv", index=False)
pd.DataFrame(refer_data).to_csv(NEO4J_OUTPUT / "refer_to.csv", index=False)

# --- 3. SOCIAL & INTERACTION EDGES ---
print("\n[4/4] Generating Social Graph...")

user_ids_list = [get_oid(u["_id"]) for u in users]
all_author_ids = [get_oid(a["_id"]) for a in authors]

# A. FOLLOWS
follows_data = []
for uid in user_ids_list:
    num_follows = random.randint(0, 5)
    targets = random.sample(user_ids_list, min(num_follows, len(user_ids_list)))
    for t in targets:
        if t != uid:
            follows_data.append({
                "start_id": uid, "end_id": t, "since": random_date_after_iso("2023-01-01T00:00:00Z")
            })
pd.DataFrame(follows_data).to_csv(NEO4J_OUTPUT / "follows.csv", index=False)

# B. LIKES_BOOK
likes_book_data = []
for r in reviews:
    if r["rating"] >= 80:
        uid = get_oid(r.get("Userd_id", r.get("user_id")))
        bid = get_oid(r["book_snapshot"]["book_id"])
        likes_book_data.append({
            "start_id": uid, "end_id": bid, "timestamp": get_date(r["created_at"])
        })
pd.DataFrame(likes_book_data).to_csv(NEO4J_OUTPUT / "user_likes_book.csv", index=False)

# C. LIKES_REVIEW
likes_review_data = []
for r in reviews:
    target_likes = r.get("likes_count", 0)
    if target_likes > 0:
        rid = get_oid(r["_id"])
        author_id = get_oid(r.get("Userd_id", r.get("user_id")))
        review_date = get_date(r["created_at"])
        candidates = random.sample(user_ids_list, min(target_likes + 2, len(user_ids_list)))
        likers = [u for u in candidates if u != author_id][:target_likes]
        for liker in likers:
            likes_review_data.append({
                "start_id": liker, "end_id": rid, "timestamp": random_date_after_iso(review_date)
            })
pd.DataFrame(likes_review_data).to_csv(NEO4J_OUTPUT / "user_likes_review.csv", index=False)

# D. LIKES_GENRE (SMART LOGIC FROM BOOKSHELF)
# ----------------------------------------------------
print("  - Calculating Smart Genre Preferences from Bookshelf...")
likes_genre_data = []

for u in users:
    uid = get_oid(u["_id"])
    user_genres_counter = Counter()
    
    # 1. Analisi Bookshelf (Solo libri Letti)
    bookshelf = u.get("bookshelf", [])
    has_read_books = False
    for item in bookshelf:
        if item.get("status") == "read":
            bid = get_oid(item["book_id"])
            # Recuperiamo i generi reali dal libro
            if bid in book_genres_map:
                g_list = book_genres_map[bid]
                user_genres_counter.update(g_list)
                has_read_books = True

    # 2. Selezione Top 3 Generi
    top_genres = []
    if user_genres_counter:
        top_genres = [g for g, _ in user_genres_counter.most_common(3)]
    else:
        # 3. Fallback (Cold Start): Se non ha letto nulla, assegniamo 1-2 generi a caso
        # per non lasciare l'utente isolato nel grafo delle raccomandazioni
        if all_genres_list:
            top_genres = random.sample(all_genres_list, min(2, len(all_genres_list)))

    # 4. Creazione Relazioni
    for g in top_genres:
        likes_genre_data.append({"start_id": uid, "end_id": g})

pd.DataFrame(likes_genre_data).to_csv(NEO4J_OUTPUT / "user_likes_genre.csv", index=False)
# ----------------------------------------------------

# E. LIKES_AUTHOR (SMART LOGIC)
print("  - Calculating Smart Author Likes...")
likes_author_data = []

for u in users:
    uid = get_oid(u["_id"])
    user_liked_authors = set()
    
    # 1. Dagli scaffali (Libri Letti)
    bookshelf = u.get("bookshelf", [])
    for item in bookshelf:
        if item.get("status") == "read":
            bid = get_oid(item["book_id"])
            if bid in book_to_author_map:
                user_liked_authors.add(book_to_author_map[bid])
    
    # 2. Fallback Casuale
    if len(user_liked_authors) < 2 and all_author_ids:
        num_random = random.randint(1, 3)
        random_picks = random.sample(all_author_ids, min(num_random, len(all_author_ids)))
        user_liked_authors.update(random_picks)
    
    for aid in user_liked_authors:
        likes_author_data.append({"start_id": uid, "end_id": aid})

pd.DataFrame(likes_author_data).to_csv(NEO4J_OUTPUT / "user_likes_author.csv", index=False)

# --- FINAL REPORT ---
print("\n" + "="*30)
print(" 📊 FINAL GENERATION REPORT")
print("="*30)
print(f"👤 Users:   {len(df_users)}")
print(f"📖 Books:   {len(df_books)}")
print(f"✍️  Authors: {len(df_authors)}")
print(f"⭐ Reviews: {len(df_reviews)}")
print(f"🏷️  Genres:  {len(df_genres)}")
print("-" * 20)
print(f"➡️  WROTE:           {len(wrote_data)}")
print(f"➡️  BELONGS_TO:      {len(belongs_data)}")
print(f"➡️  POSTED:          {len(posted_data)}")
print(f"➡️  REFER_TO:        {len(refer_data)}")
print(f"➡️  FOLLOWS:         {len(follows_data)}")
print("-" * 20)
print(f"💖 LIKES (Book):    {len(likes_book_data)}")
print(f"👍 LIKES (Review):  {len(likes_review_data)}")
print(f"❤️  LIKES (Genre):   {len(likes_genre_data)}")
print(f"✒️  LIKES (Author):  {len(likes_author_data)}")
print("="*30)
print(f"\n✅ FILES SAVED TO: {NEO4J_OUTPUT}")