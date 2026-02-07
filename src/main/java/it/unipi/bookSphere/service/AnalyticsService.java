package it.unipi.bookSphere.service;

import it.unipi.bookSphere.dto.AuthorDTO;
import it.unipi.bookSphere.dto.BookDTO;
import it.unipi.bookSphere.dto.GenreDTO;
import it.unipi.bookSphere.dto.InfluencerDTO;
import it.unipi.bookSphere.dto.InternationalityDTO;
import it.unipi.bookSphere.dto.RankingDTO;
import it.unipi.bookSphere.dto.TpiPredictionDTO;
import it.unipi.bookSphere.exceptions.BookNotFoundException;
import it.unipi.bookSphere.exceptions.AuthorNotFoundException;
import it.unipi.bookSphere.exceptions.GenreNotFoundException;
import it.unipi.bookSphere.mapper.RankingMapper;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.repository.mongo.AuthorRepository;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import it.unipi.bookSphere.repository.mongo.projections.RankingProjection;
import it.unipi.bookSphere.repository.neo4j.AuthorNodeRepository;
import it.unipi.bookSphere.repository.neo4j.BookNodeRepository;
import it.unipi.bookSphere.repository.neo4j.GenreNodeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AccumulatorOperators;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for analytics operations using MongoDB and Neo4j
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);
    private static final int DEFAULT_INFLUENCER_LIMIT = 10;
    
    private final MongoTemplate mongoTemplate;
    private final BookNodeRepository bookNodeRepository;
    private final AuthorNodeRepository authorNodeRepository;
    private final GenreNodeRepository genreNodeRepository;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    @Autowired
    private RankingMapper rankingMapper;

    /**
     * Calculate Internationality Index for a book or author
     * Analyzes geographical distribution of likes and reviews
     * 
     * @param entityId MongoDB ObjectId of the book or author
     * @param entityType Type of entity: "BOOK" or "AUTHOR"
     * @return List of InternationalityDTO with country distribution
     */
    public List<InternationalityDTO> calculateInternationality(String entityId, String entityType) {
        logger.info("Calculating internationality for entity: {} of type: {}", entityId, entityType);
        
        List<? extends it.unipi.bookSphere.repository.neo4j.projections.InternationalityProjection> results;
        
        if ("BOOK".equalsIgnoreCase(entityType)) {
            logger.debug("Entity {} is a Book", entityId);
            // Verify book exists in MongoDB
            if (!bookRepository.existsById(entityId)) {
                throw new BookNotFoundException("Book not found in MongoDB: " + entityId);
            }
            // Verify book exists in Neo4j
            if (!bookNodeRepository.existsByMongoId(entityId)) {
                throw new BookNotFoundException("Book not found in Neo4j graph: " + entityId);
            }
            results = bookNodeRepository.calculateBookInternationality(entityId);
        } else if ("AUTHOR".equalsIgnoreCase(entityType)) {
            logger.debug("Entity {} is an Author", entityId);
            // Verify author exists in MongoDB
            if (!authorRepository.existsById(entityId)) {
                throw new AuthorNotFoundException("Author not found in MongoDB: " + entityId);
            }
            // Verify author exists in Neo4j
            if (!authorNodeRepository.existsByMongoId(entityId)) {
                throw new AuthorNotFoundException("Author not found in Neo4j graph: " + entityId);
            }
            results = authorNodeRepository.calculateAuthorInternationality(entityId);
        } else {
            throw new IllegalArgumentException("Invalid entityType. Must be 'BOOK' or 'AUTHOR', got: " + entityType);
        }
        
        // Convert results to DTOs
        List<InternationalityDTO> internationality = results.stream()
            .map(proj -> new InternationalityDTO(
                proj.country(),
                proj.uniqueUsers(),
                proj.totalInteractions()
            ))
            .collect(Collectors.toList());
        
        logger.info("Calculated internationality for entity {}: {} countries", entityId,internationality.size());
        return internationality;
    }

    /**
     * Find influencers for a specific genre or across all genres
     * Identifies users whose reviews consistently receive high engagement
     * 
     * @param genre Optional genre name to filter influencers. If null, returns top influencers across all genres
     * @param limit Maximum number of influencers to return (default 10)
     * @return List of InfluencerDTO with engagement metrics
     */
    public List<InfluencerDTO> getInfluencers(String genre, Integer limit) {
        int resultLimit = (limit != null && limit > 0) ? limit : DEFAULT_INFLUENCER_LIMIT;
        
        List<? extends it.unipi.bookSphere.repository.neo4j.projections.InfluencerProjection> results;
        
        if (genre != null && !genre.isEmpty()) {
            logger.info("Finding influencers for genre: {}", genre);
            
            // Verify genre exists
            if (!genreNodeRepository.existsByName(genre)) {
                throw new GenreNotFoundException("Genre not found: " + genre);
            }
            
            results = genreNodeRepository.findGenreInfluencers(genre, resultLimit);
        } else {
            logger.info("Finding top influencers across all genres");
            results = genreNodeRepository.findTopInfluencers(resultLimit);
        }
        
        // Convert results to DTOs
        List<InfluencerDTO> influencers = results.stream()
            .map(proj -> new InfluencerDTO(
                proj.username(),
                proj.totalEngagement(),
                proj.numReviews(),
                proj.avgLikesPerReview()
            ))
            .collect(Collectors.toList());
        
        logger.info("Found {} influencers", influencers.size());
        return influencers;
    }

    // ============== MONGODB ANALYTICS METHODS ==============
    
    /**
     * Get trending books based on current month activity
     */
    public List<BookDTO> getTrendingBooks() {
        // 1. Calcola il mese corrente nel formato "yyyy-MM"
        String currentMonthStr = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        logger.info("Getting trending books based on activity for month: {}", currentMonthStr);

        Query query = new Query();
        
        // 2. Aggiungi il controllo sul campo 'month_score.Current_Month'
        query.addCriteria(Criteria.where("status").is("ACTIVE")
                .and("month_score.rating_count").gte(5)
                .and("month_score.Current_Month").is(currentMonthStr)); // Filtra per mese corrente

        query.with(Sort.by(Sort.Direction.DESC, "month_score.rating"));
        query.limit(20);

        List<BookDocument> results = mongoTemplate.find(query, BookDocument.class, "books");

        List<BookDTO> trendingBooks = results.stream()
            .map(this::mapBookDocumentToDTO)
            .collect(Collectors.toList());

        logger.info("Found {} trending books", trendingBooks.size());
        return trendingBooks;
    }

    /**
     * Get book rankings for a specific year or all-time
     */
    public List<RankingDTO> getBookRankings(Integer year) {
        logger.info("Getting book rankings via Aggregation for year: {}", year != null ? year : "all-time");

        List<AggregationOperation> pipeline = new ArrayList<>();

        // 1. MATCH: ATTENZIONE! Nel documento che hai incollato NON VEDO il campo "status".
        // Se il campo non esiste, questa riga fa fallire tutto.
        // Se esiste ma non l'hai incollato, tienila. Altrimenti commentala.
        // pipeline.add(Aggregation.match(Criteria.where("status").is("ACTIVE")));

        // 2. CALCOLO TOTALI
        if (year != null) {
            // --- CASO ANNO SPECIFICO ---
            
            // A. Filtra l'array stats_per_year (SNAKE_CASE!)
            pipeline.add(AddFieldsOperation.addField("targetStat")
                .withValue(ArrayOperators.Filter.filter("stats_per_year") // <--- QUI ERA L'ERRORE
                    .as("stat")
                    .by(ComparisonOperators.Eq.valueOf("stat.year").equalToValue(year)))
                .build());

            // B. Estrai i valori (Assumo che dentro l'oggetto ci siano ratings_count e sum_rating in snake_case)
            pipeline.add(AddFieldsOperation.addField("totalRatings")
                .withValue(ConditionalOperators.ifNull(
                    // Nota: stat.ratings_count (snake_case)
                    ArrayOperators.ArrayElemAt.arrayOf("targetStat.ratings_count").elementAt(0) 
                ).then(0))
                .build());

            pipeline.add(AddFieldsOperation.addField("sumRating")
                .withValue(ConditionalOperators.ifNull(
                    // Nota: stat.sum_rating (snake_case)
                    ArrayOperators.ArrayElemAt.arrayOf("targetStat.sum_rating").elementAt(0)
                ).then(0))
                .build());

        } else {
            // --- CASO ALL-TIME ---
            
            // Somma su stats_per_year (SNAKE_CASE!)
            pipeline.add(AddFieldsOperation.addField("totalRatings")
                .withValue(AccumulatorOperators.Sum.sumOf(
                    // Qui devi usare il percorso esatto nel JSON di Mongo
                    "stats_per_year.ratings_count" 
                ))
                .build());

            pipeline.add(AddFieldsOperation.addField("sumRating")
                .withValue(AccumulatorOperators.Sum.sumOf(
                    "stats_per_year.sum_rating"
                ))
                .build());
        }

        // 3. FILTER: Minimo voti
        pipeline.add(Aggregation.match(Criteria.where("totalRatings").gt(5)));

        // 4. CALCOLO MEDIA
        pipeline.add(AddFieldsOperation.addField("averageRating")
            .withValue(ArithmeticOperators.Divide.valueOf("sumRating").divideBy("totalRatings"))
            .build());

        // 5. SORT
        pipeline.add(Aggregation.sort(Sort.Direction.DESC, "averageRating"));

        // 6. LIMIT
        pipeline.add(Aggregation.limit(50));

        // 7. PROJECT
        pipeline.add(Aggregation.project("averageRating", "totalRatings")
            .and("title").as("name")
            .and("author.name").as("additionalInfo") // Assumo author abbia il campo "name"
            .and("_id").as("id"));

        // Esecuzione
        Aggregation aggregation = Aggregation.newAggregation(pipeline);
        
        AggregationResults<RankingProjection> results = mongoTemplate.aggregate(
            aggregation, "books", RankingProjection.class
        );

        return rankingMapper.toRankingDTOList(results.getMappedResults(), year);
    }


    /**
     * Get author rankings using in-memory processing
     */
    public List<RankingDTO> getAuthorRankings(Integer year) {
        logger.info("Getting author rankings for year: {}", year != null ? year : "all-time");
        
        List<BookDocument> books = bookRepository.findByStatus("ACTIVE");
        
        Map<String, List<Double>> authorRatings = new HashMap<>();
        Map<String, Long> authorCounts = new HashMap<>();
        
        for (BookDocument book : books) {
            if (book.getAuthor() != null && book.getStatsPerYear() != null && !book.getStatsPerYear().isEmpty()) {
                String authorName = book.getAuthor().getName();
                
                if (year != null) {
                    Optional<BookDocument.YearStat> yearStat = book.getStatsPerYear().stream()
                        .filter(stat -> year.equals(stat.getYear()))
                        .findFirst();
                    if (yearStat.isPresent() && yearStat.get().getAverageRating() != null) {
                        authorRatings.computeIfAbsent(authorName, k -> new ArrayList<>())
                            .add(yearStat.get().getAverageRating());
                        authorCounts.put(authorName, 
                            authorCounts.getOrDefault(authorName, 0L) + yearStat.get().getRatingsCount());
                    }
                } else {
                    int totalSum = book.getStatsPerYear().stream()
                        .mapToInt(stat -> stat.getSumRating() != null ? stat.getSumRating() : 0)
                        .sum();
                    int totalCount = book.getStatsPerYear().stream()
                        .mapToInt(stat -> stat.getRatingsCount() != null ? stat.getRatingsCount() : 0)
                        .sum();
                    if (totalCount > 0) {
                        double bookAvg = (double) totalSum / totalCount;
                        authorRatings.computeIfAbsent(authorName, k -> new ArrayList<>()).add(bookAvg);
                        authorCounts.put(authorName, authorCounts.getOrDefault(authorName, 0L) + totalCount);
                    }
                }
            }
        }
        
        List<RankingDTO> rankings = authorRatings.entrySet().stream()
            .filter(entry -> authorCounts.getOrDefault(entry.getKey(), 0L) >= 10)
            .map(entry -> {
                String authorName = entry.getKey();
                List<Double> ratings = entry.getValue();
                double avgRating = ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                
                return new RankingDTO(
                    null,
                    authorName,
                    avgRating,
                    authorCounts.get(authorName),
                    year,
                    null
                );
            })
            .sorted((a, b) -> Double.compare(b.getAverageRating(), a.getAverageRating()))
            .limit(30)
            .collect(Collectors.toList());
        
        logger.info("Found {} author rankings", rankings.size());
        return rankings;
    }

    /**
     * Get genre rankings
     */
    public List<RankingDTO> getGenreRankings(Integer year) {
        logger.info("Getting genre rankings for year: {}", year != null ? year : "all-time");
        
        List<BookDocument> books = bookRepository.findByStatus("ACTIVE");
        
        Map<String, List<Double>> genreRatings = new HashMap<>();
        Map<String, Integer> genreCounts = new HashMap<>();
        
        for (BookDocument book : books) {
            if (book.getGenres() != null && book.getStatsPerYear() != null && !book.getStatsPerYear().isEmpty()) {
                Double bookAvg = null;
                
                if (year != null) {
                    Optional<BookDocument.YearStat> yearStat = book.getStatsPerYear().stream()
                        .filter(stat -> year.equals(stat.getYear()))
                        .findFirst();
                    if (yearStat.isPresent()) {
                        bookAvg = yearStat.get().getAverageRating();
                    }
                } else {
                    int totalSum = book.getStatsPerYear().stream()
                        .mapToInt(stat -> stat.getSumRating() != null ? stat.getSumRating() : 0)
                        .sum();
                    int totalCount = book.getStatsPerYear().stream()
                        .mapToInt(stat -> stat.getRatingsCount() != null ? stat.getRatingsCount() : 0)
                        .sum();
                    if (totalCount > 0) {
                        bookAvg = (double) totalSum / totalCount;
                    }
                }
                
                if (bookAvg != null) {
                    for (String genre : book.getGenres()) {
                        genreRatings.computeIfAbsent(genre, k -> new ArrayList<>()).add(bookAvg);
                        genreCounts.put(genre, genreCounts.getOrDefault(genre, 0) + 1);
                    }
                }
            }
        }
        
        List<RankingDTO> rankings = genreRatings.entrySet().stream()
            .filter(entry -> genreCounts.getOrDefault(entry.getKey(), 0) >= 3)
            .map(entry -> {
                String genreName = entry.getKey();
                List<Double> ratings = entry.getValue();
                double avgRating = ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                
                return new RankingDTO(
                    null,
                    genreName,
                    avgRating,
                    genreCounts.get(genreName).longValue(),
                    year,
                    null
                );
            })
            .sorted((a, b) -> Double.compare(b.getAverageRating(), a.getAverageRating()))
            .limit(20)
            .collect(Collectors.toList());
        
        logger.info("Found {} genre rankings", rankings.size());
        return rankings;
    }

    /**
     * Calculate Trending Probability Index (TPI) for a book
     */
    public TpiPredictionDTO calculateTPI(String bookId) {
        logger.info("Calculating TPI for book: {}", bookId);
        
        Optional<BookDocument> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isEmpty()) {
            throw new BookNotFoundException("Book not found: " + bookId);
        }
        
        BookDocument book = bookOpt.get();
        String authorName = book.getAuthor().getName();
        
        // Calculate author's historical benchmark
        Query query = new Query();
        query.addCriteria(Criteria.where("author.name").is(authorName).and("status").is("ACTIVE"));
        List<BookDocument> authorBooks = mongoTemplate.find(query, BookDocument.class, "books");
        
        double authorBenchmark = 0.0;
        int totalBooks = 0;
        
        for (BookDocument authorBook : authorBooks) {
            if (authorBook.getStatsPerYear() != null && !authorBook.getStatsPerYear().isEmpty()) {
                int totalSum = authorBook.getStatsPerYear().stream()
                    .mapToInt(stat -> stat.getSumRating() != null ? stat.getSumRating() : 0)
                    .sum();
                int totalCount = authorBook.getStatsPerYear().stream()
                    .mapToInt(stat -> stat.getRatingsCount() != null ? stat.getRatingsCount() : 0)
                    .sum();
                if (totalCount > 0) {
                    authorBenchmark += (double) totalSum / totalCount;
                    totalBooks++;
                }
            }
        }
        
        if (totalBooks > 0) {
            authorBenchmark = authorBenchmark / totalBooks;
        }
        
        // Get book's current momentum
        double bookMomentum = 0.0;
        long currentActivity = 0L;
        
        if (book.getMonthScore() != null) {
            bookMomentum = book.getMonthScore().getRating() != null ? book.getMonthScore().getRating() : 0.0;
            currentActivity = book.getMonthScore().getRatingCount() != null ? book.getMonthScore().getRatingCount().longValue() : 0L;
        }
        
        // Generate prediction
        String prediction;
        if (currentActivity == 0) {
            prediction = "⏸️ STABLE (No recent data)";
        } else if (bookMomentum > authorBenchmark * 1.05) {
            prediction = "🚀 RISING STAR";
        } else if (bookMomentum < authorBenchmark * 0.95) {
            prediction = "📉 UNDERPERFORMING";
        } else {
            prediction = "➡️ STABLE";
        }
        
        TpiPredictionDTO result = new TpiPredictionDTO(
            bookId,
            book.getTitle(),
            Math.round(authorBenchmark * 100.0) / 100.0,
            Math.round(bookMomentum * 100.0) / 100.0,
            prediction,
            currentActivity
        );
        
        logger.info("Calculated TPI for book {}: {}", bookId, prediction);
        return result;
    }

    // Helper method to map BookDocument to BookDTO
    private BookDTO mapBookDocumentToDTO(BookDocument doc) {
        BookDTO bookDTO = new BookDTO();
        bookDTO.setId(doc.getId());
        bookDTO.setTitle(doc.getTitle());
        bookDTO.setPublicationYear(doc.getPublicationYear());
        bookDTO.setDescription(doc.getDescription());
        
        if (doc.getAuthor() != null) {
            AuthorDTO authorDTO = new AuthorDTO();
            authorDTO.setId(doc.getAuthor().getId());
            authorDTO.setName(doc.getAuthor().getName());
            bookDTO.setAuthor(authorDTO);
        }
        
        if (doc.getGenres() != null) {
            bookDTO.setGenres(doc.getGenres()); // BookDTO expects List<String>, not List<GenreDTO>
        }
        
        return bookDTO;
    }
}
