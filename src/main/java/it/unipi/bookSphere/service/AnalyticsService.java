package it.unipi.bookSphere.service;

import it.unipi.bookSphere.dto.AuthorDTO;
import it.unipi.bookSphere.dto.BookDTO;
import it.unipi.bookSphere.dto.InfluencerDTO;
import it.unipi.bookSphere.dto.InternationalityDTO;
import it.unipi.bookSphere.dto.RankingDTO;
import it.unipi.bookSphere.exceptions.BookNotFoundException;
import it.unipi.bookSphere.exceptions.AuthorNotFoundException;
import it.unipi.bookSphere.exceptions.GenreNotFoundException;
import it.unipi.bookSphere.mapper.RankingMapper;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.repository.mongo.AuthorRepository;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import it.unipi.bookSphere.repository.mongo.projections.RankingProjection;
import it.unipi.bookSphere.repository.neo4j.AuthorNodeRepository;
import it.unipi.bookSphere.repository.neo4j.BookNodeRepository;
import it.unipi.bookSphere.repository.neo4j.GenreNodeRepository;
import lombok.RequiredArgsConstructor;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import it.unipi.bookSphere.dto.BookTrendDTO;
import it.unipi.bookSphere.repository.mongo.projections.BookTrendProjection;
import it.unipi.bookSphere.mapper.BookTrendMapper;
import org.springframework.data.mongodb.core.aggregation.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;


import java.util.ArrayList;
import java.util.Collections;

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

    @Autowired
    private BookTrendMapper bookTrendMapper;

    /**
     * Calculate Internationality Index for a book or author
     * Analyzes geographical distribution of likes and reviews
     * Reads from both Mongo and Neo4j, so retry is valuable for transient failures
     * 
     * @param entityId MongoDB ObjectId of the book or author
     * @param entityType Type of entity: "BOOK" or "AUTHOR"
     * @return List of InternationalityDTO with country distribution
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        noRetryFor = {BookNotFoundException.class, AuthorNotFoundException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<InternationalityDTO> calculateInternationality(String entityId, String entityType) {
        logger.info("Calculating internationality for entity: {} of type: {}", entityId, entityType);
        
        List<? extends it.unipi.bookSphere.repository.neo4j.projections.InternationalityProjection> results;
        
        //If a BOOK/AUTHOR is archived it should not appear in Neo4j so an exception will be thrown
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
     * Should not consider banned user and their reviews (They should be removed from Neo4j when banend)
     * Reads from Neo4j which may have transient failures, so retry is useful
     * 
     * @param genre Optional genre name to filter influencers. If null, returns top influencers across all genres
     * @param limit Maximum number of influencers to return (default 10)
     * @return List of InfluencerDTO with engagement metrics
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        noRetryFor = {GenreNotFoundException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<InfluencerDTO> getInfluencers(String genre, Integer limit) {
        int resultLimit = (limit != null && limit > 0) ? limit : DEFAULT_INFLUENCER_LIMIT;
        
        List<? extends it.unipi.bookSphere.repository.neo4j.projections.InfluencerProjection> results;
        
        if (genre != null && !genre.isEmpty()) {
            // Normalize genre name for consistency
            String normalizedGenre = it.unipi.bookSphere.utils.NormalizationUtils.normalizeGenreName(genre);
            logger.info("Finding influencers for genre: {} (normalized: {})", genre, normalizedGenre);
            
            // Verify genre exists
            if (!genreNodeRepository.existsByName(normalizedGenre)) {
                throw new GenreNotFoundException("Genre not found: " + genre);
            }
            
            results = genreNodeRepository.findGenreInfluencers(normalizedGenre, resultLimit);
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
     * Performs a relatively simple MongoDB query with no dual-DB read.
     * Retry is still useful for transient MongoDB connection issues.
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<BookDTO> getTrendingBooks() {
        // 1. Calcola il mese corrente nel formato "yyyy-MM"
        String currentMonthStr = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        logger.info("Getting trending books based on activity for month: {}", currentMonthStr);

        Query query = new Query();
        
        // 2. Aggiungi il controllo sul campo 'month_score.Current_Month'
        query.addCriteria(Criteria.where("availability").is("ACTIVE")
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
     * Performs complex MongoDB aggregation pipeline that may timeout transiently
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<RankingDTO> getBookRankings(Integer year, String author, String genre) {
    
        // 1. Setup Pipeline & Match Criteria
        List<AggregationOperation> pipeline = new ArrayList<>();
        List<Criteria> filters = new ArrayList<>();
        
        filters.add(Criteria.where("availability").is("ACTIVE"));
        
        if (author != null) filters.add(Criteria.where("author.id").is(author));
        if (genre != null) filters.add(Criteria.where("genres").is(genre));

        pipeline.add(Aggregation.match(new Criteria().andOperator(filters.toArray(new Criteria[0]))));

        // 2. Project Stats (Handle Year Specific vs All-Time)
        if (year != null) {
            // Year context: Filter the array first to isolate the target year
            pipeline.add(Aggregation.project("title", "author")
                .and(ArrayOperators.Filter.filter("stats_per_year")
                    .as("stat")
                    .by(ComparisonOperators.Eq.valueOf("stat.year").equalToValue(year)))
                .as("targetStat"));

            // Sum the filtered array (safe way to extract value or 0 if missing)
            pipeline.add(Aggregation.project("title", "author")
                .and(AccumulatorOperators.Sum.sumOf("targetStat.ratings_count")).as("totalRatings")
                .and(AccumulatorOperators.Sum.sumOf("targetStat.sum_rating")).as("sumRating"));

        } else {
            // All-time context: Sum over the entire array
            pipeline.add(Aggregation.project("title", "author")
                .and(AccumulatorOperators.Sum.sumOf("stats_per_year.ratings_count")).as("totalRatings")
                .and(AccumulatorOperators.Sum.sumOf("stats_per_year.sum_rating")).as("sumRating"));
        }

        // 3. Threshold Filter (avoid noise & div/0 errors)
        pipeline.add(Aggregation.match(Criteria.where("totalRatings").gt(5)));

        // 4. Calculate Average & Format Output
        pipeline.add(Aggregation.project("totalRatings")
            .and("title").as("name")
            .and("author.name").as("additionalInfo")
            .and(ArithmeticOperators.Divide.valueOf("sumRating").divideBy("totalRatings")).as("averageRating"));

        // 5. Sort & Limit
        pipeline.add(Aggregation.sort(Sort.Direction.DESC, "averageRating"));
        pipeline.add(Aggregation.limit(25));

        // Execute
        AggregationResults<RankingProjection> results = mongoTemplate.aggregate(
            Aggregation.newAggregation(pipeline), "books", RankingProjection.class
        );

        return rankingMapper.toRankingDTOList(results.getMappedResults(), year);
    }


    /**
     * Get author rankings using in-memory processing
     * Performs complex MongoDB aggregation pipeline on authors collection
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<RankingDTO> getAuthorRankings() {
        logger.info("Getting author rankings (Calculated from Sum/Count) via Projection");

        List<AggregationOperation> pipeline = new ArrayList<>();

        // 1. Filter only ACTIVE authors AND at least ten rating
        pipeline.add(match(Criteria.where("status").is("ACTIVE")
            .and("ratings_count").gt(10)));

        // 2. PROJECT: Map fields and calculate average
        pipeline.add(project()
            .and("_id").as("id")
            .and("name").as("name")
            .and("ratings_count").as("totalRatings") // Maps to RankingProjection field
            .and(ConditionalOperators.when(Criteria.where("ratings_count").gt(0))
                .then(ArithmeticOperators.Divide.valueOf("sum_ratings").divideBy("ratings_count"))
                .otherwise(0.0))
            .as("averageRating")
        );

        // 3. SORT: Sort by the calculated average
        pipeline.add(sort(Sort.Direction.DESC, "averageRating"));

        // 4. LIMIT: Top 25
        pipeline.add(limit(25));

        // Execute aggregation
        Aggregation aggregation = newAggregation(pipeline);
        
        AggregationResults<RankingProjection> results = mongoTemplate.aggregate(
            aggregation, "authors", RankingProjection.class
        );

        // Final mapping (Year is null for "All-Time")
        return rankingMapper.toRankingDTOList(results.getMappedResults(), null);
    }
    

    /**
     * Get book revaluation trends
     * Performs complex MongoDB aggregation pipeline with multiple stages
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<BookTrendDTO> getBookRevaluation() {
        logger.info("Calculating book rating trends using dynamic averages (Sum/Count)");

        List<AggregationOperation> pipeline = new ArrayList<>();

        // 1. Filter only ACTIVE books & at least two years of data
        pipeline.add(match(Criteria.where("availability").is("ACTIVE")
            .and("stats_per_year.1").exists(true)));

        // 2. Project: Extract first and last snapshots
        pipeline.add(project()
                .andInclude("title")
                .and("author.name").as("author")
                .and(ArrayOperators.ArrayElemAt.arrayOf("stats_per_year").elementAt(0)).as("firstStat")
                .and(ArrayOperators.ArrayElemAt.arrayOf("stats_per_year").elementAt(-1)).as("lastStat")
        );

        // 3. Calculate start/end averages (handle division by zero)
        pipeline.add(project()
                .and("_id").as("id")
                .and("title").as("title")
                .and("author").as("author")
                .and("firstStat.year").as("startYear")
                .and("lastStat.year").as("endYear")
                .and(ConditionalOperators.when(Criteria.where("firstStat.ratings_count").gt(0))
                        .then(ArithmeticOperators.Divide.valueOf("firstStat.sum_rating").divideBy("firstStat.ratings_count"))
                        .otherwise(0.0))
                .as("startRating")
                .and(ConditionalOperators.when(Criteria.where("lastStat.ratings_count").gt(0))
                        .then(ArithmeticOperators.Divide.valueOf("lastStat.sum_rating").divideBy("lastStat.ratings_count"))
                        .otherwise(0.0))
                .as("endRating")
        );

        // 4. Compute rating delta
        pipeline.add(AddFieldsOperation.addField("ratingDelta")
                .withValue(ArithmeticOperators.Subtract.valueOf("endRating").subtract("startRating"))
                .build());

        // 5. Sort by highest improvement
        pipeline.add(sort(Sort.Direction.DESC, "ratingDelta"));

        pipeline.add(limit(10));

        Aggregation aggregation = newAggregation(pipeline);

        AggregationResults<BookTrendProjection> results = mongoTemplate.aggregate(
                aggregation, "books", BookTrendProjection.class
        );

        return bookTrendMapper.toDtoList(results.getMappedResults());
    }

    /**
     * Get book rankings FOR A SPECIFIC AUTHOR utilizing the Author's published_books array.
     * App-Side Join approach: Fetch IDs from Author -> Query Books by IDs.
     * Performs complex aggregation with validation, so retry is valuable for transient failures
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        noRetryFor = {AuthorNotFoundException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<RankingDTO> getBookRankingsAuthorV2(Integer year, String authorID) {
        logger.info("Starting ranking calculation for author: '{}', year: {}", authorID, year);

        // 1. Fetch Author using Repository
        Optional<AuthorDocument> authorOpt = authorRepository.findById(authorID);


        if (authorOpt.isEmpty()) {
            logger.info("Author '{}' not found in database.", authorID);
            return Collections.emptyList();
        }

        AuthorDocument authorDoc = authorOpt.get();

        //CHECK IF AUTHOR IS ARCHIVED
        if(authorDoc.getStatus().equals("ARCHIVED")){
            logger.info("Author '{}' is archived.", authorID);
            return Collections.emptyList();
        }

        // Check if the list of published books is empty or null
        if (authorDoc.getPublishedBooks() == null || authorDoc.getPublishedBooks().isEmpty()) {
            logger.info("Author '{}' found, but has no published books associated.", authorID);
            return Collections.emptyList();
        }

        // 2. Extract IDs and CONVERT String -> ObjectId
        List<ObjectId> bookIds = authorDoc.getPublishedBooks().stream()
                .map(book -> new ObjectId(book.getId()))
                .collect(Collectors.toList());

        logger.info("Found {} book IDs for author '{}'. Proceeding with aggregation.", bookIds.size(), authorID);

        // 3. Setup Pipeline on BOOKS collection using IDs
        List<AggregationOperation> pipeline = new ArrayList<>();

        // MATCH by IDs (Primary Key) AND ensure availability is ACTIVE
        pipeline.add(Aggregation.match(Criteria.where("_id").in(bookIds).and("availability").is("ACTIVE")));

        if (year != null) {
            // Year context: Filter the array first to isolate the target year
            pipeline.add(Aggregation.project("title", "author")
                    .and(ArrayOperators.Filter.filter("stats_per_year")
                            .as("stat")
                            .by(ComparisonOperators.Eq.valueOf("stat.year").equalToValue(year)))
                    .as("targetStat"));

            // Sum the filtered array
            pipeline.add(Aggregation.project("title", "author")
                    .and(AccumulatorOperators.Sum.sumOf("targetStat.ratings_count")).as("totalRatings")
                    .and(AccumulatorOperators.Sum.sumOf("targetStat.sum_rating")).as("sumRating"));

        } else {
            // All-time context: Sum over the entire array
            pipeline.add(Aggregation.project("title", "author")
                    .and(AccumulatorOperators.Sum.sumOf("stats_per_year.ratings_count")).as("totalRatings")
                    .and(AccumulatorOperators.Sum.sumOf("stats_per_year.sum_rating")).as("sumRating"));
        }

        // 3. Threshold Filter
        pipeline.add(Aggregation.match(Criteria.where("totalRatings").gt(5)));

        // 4. Calculate Average & Format Output
        pipeline.add(Aggregation.project("totalRatings")
                .and("title").as("name")
                .and("author.name").as("additionalInfo")
                .and(ArithmeticOperators.Divide.valueOf("sumRating").divideBy("totalRatings")).as("averageRating"));

        // 5. Sort & Limit
        pipeline.add(Aggregation.sort(Sort.Direction.DESC, "averageRating"));
        pipeline.add(Aggregation.limit(25));

        // Execute Aggregation
        AggregationResults<RankingProjection> results = mongoTemplate.aggregate(
                Aggregation.newAggregation(pipeline), "books", RankingProjection.class
        );

        List<RankingProjection> mappedResults = results.getMappedResults();
        logger.info("Aggregation finished. Found {} ranked books matching criteria.", mappedResults.size());

        return rankingMapper.toRankingDTOList(mappedResults, year);
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
