"""
SCRIPT 1: MongoDB Data Generation - FINAL V12 (CORRECTED STRUCTURES)
- Features:
    - Review: Includes 'username' (denormalized).
    - Book: Includes 'review_ids' (linked list) + Snapshots with 'user_id'.
    - User: Includes 'review_ids' (linked list).
    - Bookshelf: Fully embedded.
"""

import pandas as pd
import json
import random
import re
import pickle
import sys
import hashlib
from datetime import datetime, timedelta
from pathlib import Path
from collections import Counter
from bson import ObjectId

# --- CONFIGURAZIONE ---
random.seed(42)

BASE_DIR = Path(__file__).resolve().parent
AMAZON_BOOKS = BASE_DIR / "DATASET" / "AMAZON" / "books_data.csv"
AMAZON_RATINGS = BASE_DIR / "DATASET" / "AMAZON" / "Books_rating.csv"
BOOKCROSSING_BOOKS = BASE_DIR / "DATASET" / "BOOKCROSSING" / "Books.csv"
BOOKCROSSING_RATINGS = BASE_DIR / "DATASET" / "BOOKCROSSING" / "Ratings.csv"

MONGO_OUTPUT = BASE_DIR / "DATASET" / "MONGODB"
CHECKPOINT_DIR = BASE_DIR / "DATASET" / ".checkpoints"

ENABLE_CHECKPOINTS = True
START_FROM_STEP = 1 

MAX_BOOKS_TOTAL = 20000         
MAX_USERS = 12000               
MAX_REVIEWS_PER_BOOK = 50       

# DATE CONFIG
END_DATE_CAP = datetime(2025, 12, 31)
SHIFT_YEARS = 12
SHIFT_DAYS = (365 * SHIFT_YEARS) + 3

# --- UTILS ---
def generate_object_id(): return str(ObjectId())

RE_CLEAN = re.compile(r'\([^)]*\)|\[[^\]]*\]|:.*')
def clean_key_fast(text):
    if not isinstance(text, str): return ""
    text = text.lower()
    text = RE_CLEAN.sub('', text)
    return text.strip()

# --- AUTHOR NORMALIZATION ---
def normalize_author_key(name):
    if not name: return "unknown"
    s = name.lower().replace('.', '').replace(',', '').replace(' ', '')
    return s

def normalize_rating(rating, source):
    try: val = float(rating)
    except: return 0
    if pd.isna(val): return 0
    
    if source == "amazon":
        s = int(val)
        if s >= 5: return random.randint(90, 100)
        elif s == 4: return random.randint(70, 89)
        elif s == 3: return random.randint(50, 69)
        else: return random.randint(10, 49)
    elif source == "bookcrossing":
        base = int(val) * 10
        if base == 0: return 0
        return max(10, min(100, base + random.randint(-2, 2)))
    return 0

def parse_authors(author_str):
    if pd.isna(author_str): return ["Unknown Author"]
    s = str(author_str).strip()
    if s.startswith('['): s = s[1:-1]
    return [a.strip().strip("'\"") for a in s.split(',') if a] or ["Unknown Author"]

def parse_genres(genre_str):
    if pd.isna(genre_str): return ["General"]
    s = str(genre_str).strip()
    if s.startswith('['): s = s[1:-1]
    res = []
    for g in s.split(','):
        clean = g.strip().strip("'\"")
        if clean:
            if '&' in clean: clean = clean.split('&')[0].strip()
            res.append(clean)
    return res[:3] if res else ["General"]

def random_date(start_year=2023, end_year=2025):
    start = datetime(start_year, 1, 1)
    end = datetime(end_year, 12, 31)
    if end > END_DATE_CAP: end = END_DATE_CAP
    delta = end - start
    if delta.days <= 0: return start
    return start + timedelta(days=random.randint(0, delta.days))

def random_country():
    return random.choice(["US", "GB", "CA", "IT", "DE", "FR", "ES"])

def to_mongo_date(dt): return {"$date": dt.isoformat() + ('Z' if not dt.isoformat().endswith('Z') else '')}
def to_mongo_oid(oid_str): return {"$oid": oid_str}

def save_checkpoint(step, data):
    if not ENABLE_CHECKPOINTS: return
    CHECKPOINT_DIR.mkdir(parents=True, exist_ok=True)
    with open(CHECKPOINT_DIR / f"step_{step}.pkl", 'wb') as f: pickle.dump(data, f)
    print(f"  💾 Saved checkpoint {step}")

def load_checkpoint(step):
    f = CHECKPOINT_DIR / f"step_{step}.pkl"
    if ENABLE_CHECKPOINTS and f.exists() and START_FROM_STEP > step:
        with open(f, 'rb') as h: return pickle.load(h)
    return None

# --- MAIN ETL ---
print("="*60 + "\nMONGO GENERATOR V12 (CORRECTED STRUCTURES)\n" + "="*60)

# STEP 1: RANKING & BOOKS
step = 1
cp = load_checkpoint(step)
if cp:
    print(f"[{step}/6] ⏩ Loaded Checkpoint.")
    books_data, books_map, title_to_book_id, isbn_to_book_id, authors_map, genres_set, book_id_to_genres, amz_id_to_book_id, author_normalization_map, genre_to_books_map = \
        cp['books_data'], cp['books_map'], cp['title_to_book_id'], cp['isbn_to_book_id'], cp['authors_map'], cp['genres_set'], cp['book_id_to_genres'], cp['amz_id_to_book_id'], cp['author_normalization_map'], cp['genre_to_books_map']
else:
    print(f"[{step}/6] Global Rankings (BC First)...")
    
    # 1. BC POPULARITY
    bc_r = pd.read_csv(BOOKCROSSING_RATINGS, sep=';', encoding='latin-1', on_bad_lines='skip', dtype=str)
    bc_r.rename(columns={'ISBN': 'isbn', 'User-ID': 'user_id', 'Book-Rating': 'rating', 'Rating': 'rating'}, inplace=True)
    bc_isbn_counts = bc_r['isbn'].value_counts()
    
    bc_books = pd.read_csv(BOOKCROSSING_BOOKS, sep=';', encoding='latin-1', on_bad_lines='skip', dtype=str)
    bc_books.rename(columns={'ISBN': 'isbn', 'Book-Title': 'title'}, inplace=True)
    bc_books['clean_title'] = bc_books['title'].apply(clean_key_fast)
    
    title_scores = {}
    isbn_count_map = bc_isbn_counts.to_dict()
    for isbn, title in zip(bc_books['isbn'], bc_books['clean_title']):
        if not title: continue
        c = isbn_count_map.get(isbn, 0)
        if c > 0: title_scores[title] = title_scores.get(title, 0) + c
            
    # 2. AMAZON SELECTION
    amz_books_df = pd.read_csv(AMAZON_BOOKS, encoding='utf-8', on_bad_lines='skip')
    amz_books_df['clean_title'] = amz_books_df['Title'].apply(clean_key_fast)
    amz_books_df['score'] = amz_books_df['clean_title'].map(title_scores).fillna(0)
    amz_books_df.sort_values(by=['score'], ascending=False, inplace=True)
    amz_books_df.drop_duplicates(subset=['clean_title'], keep='first', inplace=True)
    
    top_books = amz_books_df.head(MAX_BOOKS_TOTAL)
    
    # 3. GENERATE DOCS
    books_data = []
    books_map = {}
    title_to_book_id = {}
    authors_map = {} 
    author_normalization_map = {}
    genres_set = set()
    book_id_to_genres = {}
    
    for _, row in top_books.iterrows():
        mid = generate_object_id()
        clean = row['clean_title']
        
        # --- AUTHOR NORMALIZATION ---
        raw_auth_name = parse_authors(row.get('authors'))[0]
        norm_key = normalize_author_key(raw_auth_name)
        
        if norm_key in author_normalization_map:
            auth_id = author_normalization_map[norm_key]
            display_name = authors_map[auth_id]["name"]
        else:
            auth_id = generate_object_id()
            author_normalization_map[norm_key] = auth_id
            display_name = raw_auth_name 
            authors_map[auth_id] = {
                "id": auth_id, 
                "name": display_name, 
                "books": []
            }

        genres = parse_genres(row.get('categories'))
        
        book = {
            "_id": to_mongo_oid(mid),
            "title": str(row['Title']),
            "publication_year": 2000,
            "description": str(row.get('description', ''))[:500],
            "author": {"id": to_mongo_oid(auth_id), "name": display_name}, 
            "genres": genres,
            "external_ids": {"isbns": []}, 
            
            # --- UPDATED STRUCTURES ---
            "recent_reviews_snapshot": [], 
            "popular_reviews_snapshot": [], 
            "stats_per_year": [],
            "review_ids": [], # LINKED ID: Added
            
            "trend_score": {"rating": 0, "updated_at": to_mongo_date(datetime.now())},
            "source": "amazon_master",
        }
        
        books_data.append(book)
        books_map[clean] = book
        title_to_book_id[clean] = mid
        book_id_to_genres[mid] = genres
        genres_set.update(genres)
        authors_map[auth_id]["books"].append({"_id": to_mongo_oid(mid), "title": book["title"]})

    # 4. MAPPING
    valid_titles = set(title_to_book_id.keys())
    bc_books_filtered = bc_books[bc_books['clean_title'].isin(valid_titles)]
    isbn_to_book_id = {}
    for clean, isbn in zip(bc_books_filtered['clean_title'], bc_books_filtered['isbn']):
        if pd.notna(isbn): isbn_to_book_id[str(isbn).strip()] = title_to_book_id[clean]
            
    amz_ratings = pd.read_csv(AMAZON_RATINGS, encoding='utf-8', on_bad_lines='skip', usecols=['Id', 'Title'])
    amz_ratings['clean_title'] = amz_ratings['Title'].apply(clean_key_fast)
    amz_ratings_filtered = amz_ratings[amz_ratings['clean_title'].isin(valid_titles)]
    amz_ratings_filtered.drop_duplicates(subset=['Id'], inplace=True)
    
    amz_id_to_book_id = {}
    for aid, clean in zip(amz_ratings_filtered['Id'], amz_ratings_filtered['clean_title']):
        amz_id_to_book_id[aid] = title_to_book_id[clean]

    print(f"    Mapped {len(isbn_to_book_id)} BC ISBNs and {len(amz_id_to_book_id)} Amazon IDs.")
    
    genre_to_books_map = {}
    for b in books_data:
        bid_str = b['_id']['$oid']
        for g in b['genres']:
            genre_to_books_map.setdefault(g, []).append(bid_str)

    save_checkpoint(step, {
        'books_data': books_data, 'books_map': books_map, 'title_to_book_id': title_to_book_id,
        'isbn_to_book_id': isbn_to_book_id, 'authors_map': authors_map, 'genres_set': genres_set,
        'book_id_to_genres': book_id_to_genres, 'amz_id_to_book_id': amz_id_to_book_id,
        'author_normalization_map': author_normalization_map,
        'genre_to_books_map': genre_to_books_map
    })

# STEP 2: USERS
step = 2
cp = load_checkpoint(step)
if cp:
    print(f"[{step}/6] ⏩ Loaded Checkpoint.")
    users_data, user_activity = cp['users_data'], cp['user_activity']
else:
    print(f"[{step}/6] Generating Users...")
    u_set = set()
    try:
        bc_u = pd.read_csv(BOOKCROSSING_RATINGS, sep=';', usecols=['User-ID'], nrows=100000, encoding='latin-1', dtype=str)
        u_set.update(bc_u['User-ID'].dropna())
        amz_u = pd.read_csv(AMAZON_RATINGS, usecols=['User_id'], nrows=100000, dtype=str)
        u_set.update(amz_u['User_id'].dropna())
    except: pass
    
    uid_list = list(u_set)
    if len(uid_list) > MAX_USERS: uid_list = random.sample(uid_list, MAX_USERS)
    
    users_data = {}
    user_activity = {}
    for uid in uid_list:
        mid = generate_object_id()
        users_data[uid] = {
            "_id": to_mongo_oid(mid), 
            "username": f"User_{uid}",
            "password_hashed": hashlib.sha256(f"password_{uid}".encode()).hexdigest(),
            "email": f"user_{uid}@bx.com", "country": random_country(),
            "joined_at": to_mongo_date(random_date(2023, 2024)), "status": "active",
            
            # --- UPDATED STRUCTURES ---
            "bookshelf": [], 
            "reviews_year": [],
            "review_ids": [] # LINKED ID: Added
        }
        user_activity[uid] = {'mongo_id': mid, 'read_books': set(), 'review_ids': [], 'genres': Counter()}
        
    save_checkpoint(step, {'users_data': users_data, 'user_activity': user_activity})

# STEP 3: REVIEWS
step = 3
print(f"[{step}/6] Processing Reviews...")

reviews_data = []
reviews_map = {}
book_reviews = {} 

bid_to_book_obj = {b['_id']['$oid']: b for b in books_data}

# Helper to get username safely
def get_username(uid):
    if uid in users_data: return users_data[uid]['username']
    return "Unknown User"

# 1. BOOKCROSSING
bc_df = pd.read_csv(BOOKCROSSING_RATINGS, sep=';', encoding='latin-1', dtype=str, on_bad_lines='skip')
bc_df.rename(columns={'ISBN': 'isbn', 'User-ID': 'user_id', 'Book-Rating': 'rating', 'Rating': 'rating'}, inplace=True)
valid_isbns = set(isbn_to_book_id.keys())
valid_users = set(users_data.keys())
bc_valid = bc_df[ (bc_df['isbn'].isin(valid_isbns)) & (bc_df['user_id'].isin(valid_users)) ]

bc_count = 0
for _, row in bc_valid.iterrows():
    if len(reviews_data) >= MAX_BOOKS_TOTAL * MAX_REVIEWS_PER_BOOK: break
    
    mongo_bid = isbn_to_book_id[row['isbn']]
    mongo_uid = user_activity[row['user_id']]['mongo_id']
    
    if len(book_reviews.get(mongo_bid, [])) >= MAX_REVIEWS_PER_BOOK: continue 
    rating = normalize_rating(row['rating'], "bookcrossing")
    if rating == 0: continue
    
    rid = generate_object_id()
    rdate = random_date(2023, 2025)
    
    # --- FETCH DETAILS ---
    target_book = bid_to_book_obj.get(mongo_bid)
    book_title = target_book['title'] if target_book else "Unknown Title"
    raw_author = target_book['author'] if target_book else {"id": "", "name": "Unknown"}
    book_author_snap = {"id": raw_author.get("id"), "name": raw_author.get("name")}
    book_genres = target_book['genres'] if target_book else []
    # ---------------------

    rev = {
        "_id": to_mongo_oid(rid), 
        "user_id": to_mongo_oid(mongo_uid),
        
        # --- DENORMALIZATION: USERNAME ADDED ---
        "username": get_username(row['user_id']),
        
        "rating": rating, 
        "source": "bookcrossing", 
        "text": "",
        "summary": "",
        "created_at": to_mongo_date(rdate), 
        "likes_count": 0, "is_banned": False,
        "book_snapshot": {
            "title": book_title, 
            "book_id": to_mongo_oid(mongo_bid),
            "genres": book_genres 
        },
        "author_snapshot": book_author_snap
    }
    reviews_data.append(rev); reviews_map[rid] = rev
    book_reviews.setdefault(mongo_bid, []).append(rid)
    user_activity[row['user_id']]['review_ids'].append(rid)
    user_activity[row['user_id']]['read_books'].add(mongo_bid)
    bc_count += 1

print(f"    > Imported {bc_count} BC Reviews.")

# 2. AMAZON
amz_df = pd.read_csv(AMAZON_RATINGS, encoding='utf-8', on_bad_lines='skip')
valid_aids = set(amz_id_to_book_id.keys())
amz_valid = amz_df[ (amz_df['Id'].isin(valid_aids)) & (amz_df['User_id'].isin(valid_users)) ]
amz_count = 0

for _, row in amz_valid.iterrows():
    if len(reviews_data) >= MAX_BOOKS_TOTAL * MAX_REVIEWS_PER_BOOK: break
    
    mongo_bid = amz_id_to_book_id[row['Id']]
    if len(book_reviews.get(mongo_bid, [])) >= MAX_REVIEWS_PER_BOOK: continue
    
    mongo_uid = user_activity[row['User_id']]['mongo_id']
    rating = normalize_rating(row.get('review/score'), "amazon")
    
    rid = generate_object_id()
    try:
        orig_ts = float(row.get('review/time', 0))
        if orig_ts > 0:
            rdate = datetime.fromtimestamp(orig_ts) + timedelta(days=SHIFT_DAYS)
            if rdate > END_DATE_CAP:
                rdate = END_DATE_CAP - timedelta(days=random.randint(0, 30))
        else: rdate = random_date(2023, 2025)
    except: rdate = random_date(2023, 2025)
        
    summary = str(row.get('review/summary', ''))[:150]

    # --- FETCH DETAILS ---
    target_book = bid_to_book_obj.get(mongo_bid)
    book_title = target_book['title'] if target_book else "Unknown Title"
    raw_author = target_book['author'] if target_book else {"id": "", "name": "Unknown"}
    book_author_snap = {"id": raw_author.get("id"), "name": raw_author.get("name")}
    book_genres = target_book['genres'] if target_book else []
    # ---------------------
    
    rev = {
        "_id": to_mongo_oid(rid), 
        "user_id": to_mongo_oid(mongo_uid),
        
        # --- DENORMALIZATION: USERNAME ADDED ---
        "username": get_username(row['User_id']),
        
        "rating": rating, 
        "source": "amazon", 
        "text": str(row.get('review/text', ''))[:500],
        "summary": summary,
        "created_at": to_mongo_date(rdate), 
        "likes_count": int(random.expovariate(0.2)), 
        "is_banned": False,
        "book_snapshot": {
            "title": book_title, 
            "book_id": to_mongo_oid(mongo_bid),
            "genres": book_genres 
        },
        "author_snapshot": book_author_snap 
    }
    reviews_data.append(rev); reviews_map[rid] = rev
    book_reviews.setdefault(mongo_bid, []).append(rid)
    user_activity[row['User_id']]['review_ids'].append(rid)
    user_activity[row['User_id']]['read_books'].add(mongo_bid)
    amz_count += 1

print(f"    > Imported {amz_count} Amazon Reviews.")

# STEP 4: ENRICHMENT
print("\n[4/6] Finalizing Stats & Saving...")

user_oid_to_name = {u['_id']['$oid']: u['username'] for u in users_data.values()}

# Inject BC ISBNs
count_isbns = 0
for isbn, bid in isbn_to_book_id.items():
    if bid in bid_to_book_obj:
        if isbn not in bid_to_book_obj[bid]['external_ids']['isbns']:
            bid_to_book_obj[bid]['external_ids']['isbns'].append(isbn)
            count_isbns += 1
print(f"    Mapped {count_isbns} ISBNs into book documents.")

# 1. USERS ENRICHMENT
for uid, act in user_activity.items():
    u = users_data[uid]
    
    # --- POPULATE REVIEW_IDS (User side) ---
    u["review_ids"] = [to_mongo_oid(rid) for rid in act['review_ids']]
    
    # ... (Bookshelf logic similar to before) ...
    
    # --- CALCOLO GENERE SOLO INTERNO ---
    user_genres_counter = Counter()
    for bid in act['read_books']:
        g_list = book_id_to_genres.get(bid, [])
        user_genres_counter.update(g_list)
    
    temp_fav_genres = []
    if user_genres_counter:
        temp_fav_genres = [g for g, _ in user_genres_counter.most_common(3)]
    else:
        temp_fav_genres = random.sample(list(genres_set), random.randint(1, 3))

    # --- BOOKSHELF (READ) ---
    u["bookshelf"] = []
    for bid in list(act['read_books'])[:20]:
        tb = bid_to_book_obj.get(bid)
        if tb:
            u["bookshelf"].append({
                "book_id": to_mongo_oid(bid),
                "title": tb['title'],
                "status": "read",
                "added_at": to_mongo_date(random_date(2023, 2025)),
                "author": tb['author'],
                "genres": tb['genres']
            })

    # --- BOOKSHELF (WANT TO READ) ---
    candidates = set()
    for g in temp_fav_genres:
        if g in genre_to_books_map:
            candidates.update(genre_to_books_map[g])
    
    valid_candidates = list(candidates - act['read_books'])
    
    if valid_candidates and random.random() < 0.2:
        num_to_pick = min(2, len(valid_candidates))
        picked_books = random.sample(valid_candidates, num_to_pick)
        for bid_str in picked_books:
            tb = bid_to_book_obj.get(bid_str)
            if tb:
                u["bookshelf"].append({
                    "book_id": to_mongo_oid(bid_str),
                    "title": tb['title'],
                    "status": "want_to_read",
                    "added_at": to_mongo_date(random_date(2025, 2025)),
                    "author": tb['author'], 
                    "genres": tb['genres'] 
                })
    
    for rid in act['review_ids']:
        if rid in reviews_map:
            r = reviews_map[rid]
            if "2025" in r['created_at']['$date']:
                u["reviews_year"].append({
                    "id": to_mongo_oid(rid),
                    "rating": r["rating"],
                    "book": r["book_snapshot"]["title"]
                })
    u["reviews_year"] = u["reviews_year"][:20] 

# 2. BOOKS STATS & LINKING
for b in books_data:
    bid = b['_id']['$oid']
    
    # --- POPULATE REVIEW_IDS (Book side) ---
    if bid in book_reviews:
        # Convert all string RIDs to Mongo OIDs
        b["review_ids"] = [to_mongo_oid(rid) for rid in book_reviews[bid]]
        
        revs = [reviews_map[rid] for rid in book_reviews[bid]]
        
        # Stats Logic ...
        ratings = [x['rating'] for x in revs]
        avg = sum(ratings)/len(ratings) if ratings else 0
        b['trend_score']['rating'] = round(avg, 2)
        
        by_year = {}
        for r in revs:
            y = int(r['created_at']['$date'][:4])
            if y not in by_year: by_year[y] = []
            by_year[y].append(r)
            
        for y, yr in by_year.items():
            rs = [x['rating'] for x in yr]
            total_sum = sum(rs)
            b['stats_per_year'].append({
                "year": y,
                "average_rating": round(total_sum/len(rs), 2),
                "ratings_count": len(rs),
                "sum_rating": total_sum 
            })
        b['stats_per_year'].sort(key=lambda k: k['year'])

        # --- SNAPSHOTS (With UserID & Username) ---
        top_likes = sorted(revs, key=lambda x: x.get('likes_count', 0), reverse=True)[:3]
        for tr in top_likes:
            # Note: reviews_map already has username denormalized, but let's be safe
            real_username = tr.get("username", "Unknown")
            b['popular_reviews_snapshot'].append({
                '_id': tr['_id'],
                'user_id': tr['user_id'], # ADDED: User ID in snapshot
                'username': real_username, 
                'rating': tr['rating'], 'num_of_like': tr.get('likes_count', 0),
                'snippet': tr['text'][:50], 'date': tr['created_at']
            })
            
        recents = sorted(revs, key=lambda x: x['created_at']['$date'], reverse=True)[:3]
        for tr in recents:
             real_username = tr.get("username", "Unknown")
             b['recent_reviews_snapshot'].append({ 
                '_id': tr['_id'],
                'user_id': tr['user_id'], # ADDED: User ID in snapshot
                'username': real_username, 
                'rating': tr['rating'], 'snippet': tr['text'][:50], 'date': tr['created_at']
            })

# 3. AUTHORS STATS
for ad in authors_map.values():
    tot_ratings = 0
    sum_ratings = 0
    books_key = "published_books" if "published_books" in ad else "books"
    for b_ref in ad[books_key]:
        bid = b_ref["_id"]["$oid"]
        if bid in book_reviews:
            for rid in book_reviews[bid]:
                r = reviews_map[rid]
                tot_ratings += 1            
                sum_ratings += r['rating']   
    if tot_ratings > 0: avg = round(sum_ratings / tot_ratings, 2)
    else: avg = 0
    ad['average_rating'] = avg
    ad['ratings_count'] = tot_ratings
    ad['sum_ratings'] = sum_ratings

# STEP 5: SAVING
print("\n[5/6] Saving Files...")
MONGO_OUTPUT.mkdir(parents=True, exist_ok=True)
def save(data, n):
    with open(MONGO_OUTPUT / n, 'w', encoding='utf-8') as f:
        for d in data: f.write(json.dumps(d, ensure_ascii=False) + '\n')

save(books_data, 'books.jsonl')
save(reviews_data, 'reviews.jsonl')

all_users = list(users_data.values())
active_count = sum(1 for u in all_users if len(u['bookshelf']) > 0)
empty_count = len(all_users) - active_count

print(f"    User Stats: {active_count} active | {empty_count} empty. Total: {len(all_users)}")
save(all_users, 'users.jsonl')

save([
    {
        "_id": to_mongo_oid(v["id"]), 
        "name": v["name"], 
        "published_books": v.get("published_books", v.get("books", [])), 
        "average_rating": v.get("average_rating", 0),
        "ratings_count": v.get("ratings_count", 0),
        "sum_ratings": v.get("sum_ratings", 0),
    } 
    for v in authors_map.values()
], 'authors.jsonl')

mb = sum(f.stat().st_size for f in MONGO_OUTPUT.glob('*.jsonl')) / (1024*1024)
print(f"📦 Total Size: {mb:.2f} MB")
print(f"\n✅ DONE! Output: {MONGO_OUTPUT}")