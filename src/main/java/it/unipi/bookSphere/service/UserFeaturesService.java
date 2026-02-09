package it.unipi.bookSphere.service;

import it.unipi.bookSphere.dto.RecommendationDTO;
import it.unipi.bookSphere.dto.WrappedDTO;
import it.unipi.bookSphere.exceptions.UserNotFoundException;
import it.unipi.bookSphere.mapper.WrappedMapper;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.mongo.projections.WrappedAggregationResult;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
import it.unipi.bookSphere.repository.neo4j.projections.RecommendationProjection;
import it.unipi.bookSphere.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff; 
import java.time.ZoneId; // <--- Utile per conversione date
import java.util.Date;   // <--- Utile per conversione date

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for user-specific features like recommendations and yearly wrap
 */
@Service
@RequiredArgsConstructor
public class UserFeaturesService {

    private static final Logger logger = LoggerFactory.getLogger(UserFeaturesService.class);
    private static final int DEFAULT_RECOMMENDATION_LIMIT = 10;
    
    @Autowired
    private final MongoTemplate mongoTemplate;
    private final UserNodeRepository userNodeRepository;
    private final RegisteredUserRepository userRepository;

    @Autowired
    private WrappedMapper wrappedMapper;

    /**
     * Get personalized book recommendations for the current user
     * Uses Neo4j graph to find books based on:
     * - Books liked by followed users
     * - Books by liked authors
     * - Books in liked genres
     * Excludes books already reviewed by the user
     * Reads from both Neo4j and MongoDB for validation, so retry valuable for transient failures
     * 
     * @param limit Maximum number of recommendations (default 10)
     * @return List of RecommendationDTO with scores
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        noRetryFor = {UserNotFoundException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<RecommendationDTO> getRecommendations(Integer limit) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        
        if (currentUserId == null) {
            throw new UserNotFoundException("User not authenticated");
        }
        
        logger.info("Getting recommendations for user: {}", currentUserId);
        
        // Verify user exists
        if (!userRepository.existsById(currentUserId)) {
            throw new UserNotFoundException("User not found: " + currentUserId);
        }
        
        int resultLimit = (limit != null && limit > 0) ? limit : DEFAULT_RECOMMENDATION_LIMIT;
        
        // Get recommendations from Neo4j
        List<RecommendationProjection> results = userNodeRepository.getUserRecommendations(currentUserId, resultLimit);
        
        // Convert to DTOs with additional book info from MongoDB
        List<RecommendationDTO> recommendations = results.stream()
            .map(proj -> {
                String bookId = proj.bookId();
                String title = proj.title();
                Integer year = proj.publicationYear();
                Long score = proj.score();
                String authorName = proj.author();
                return new RecommendationDTO(bookId, title, score, authorName, year);
            })
            .collect(Collectors.toList());
        
        logger.info("Generated {} recommendations for user {}", recommendations.size(), currentUserId);
        return recommendations;
    }

    /**
     * Generate yearly wrapped for the current user
     * Simplified version using current user data
     * Performs complex MongoDB aggregation pipeline with multiple stages
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        noRetryFor = {UserNotFoundException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public WrappedDTO getYearlyWrapped() {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UserNotFoundException("User not authenticated");
        }

        logger.info("Generating yearly wrapped for user: {}", currentUserId); 
        
        // --- RIMOSSO userRepository.findById() per performance ---

        int currentYear = LocalDate.now().getYear();
        
        // Date
        Date startOfYear = Date.from(LocalDate.of(currentYear, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endOfYear = Date.from(LocalDate.of(currentYear, 12, 31).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());

        // 1. Match User
        MatchOperation matchUser = match(Criteria.where("_id").is(currentUserId));

        // 2. AddFields
        AddFieldsOperation prepareData = AddFieldsOperation.addField("best_book")
            .withValue(ArrayOperators.ArrayElemAt.arrayOf(
                ArrayOperators.SortArray.sortArray(
                    // PROTEZIONE: Se reviews_year è null, usa un array vuoto []
                    ConditionalOperators.ifNull("reviews_year").then(new ArrayList<>())
                ).by(Sort.by(Sort.Direction.DESC, "rating"))
            ).elementAt(0))
            .addField("worst_book").withValue(ArrayOperators.ArrayElemAt.arrayOf(
                ArrayOperators.SortArray.sortArray(
                    ConditionalOperators.ifNull("reviews_year").then(new ArrayList<>())
                ).by(Sort.by(Sort.Direction.ASC, "rating"))
            ).elementAt(0))
            .addField("yearly_books").withValue(ArrayOperators.Filter.filter(
                    // PROTEZIONE: Se bookshelf è null, usa array vuoto
                    ConditionalOperators.ifNull("bookshelf").then(new ArrayList<>())
                )
                .as("b")
                .by(BooleanOperators.And.and(
                    ComparisonOperators.Eq.valueOf("b.status").equalToValue("read"),
                    ComparisonOperators.Gte.valueOf("b.added_at").greaterThanEqualToValue(startOfYear),
                    ComparisonOperators.Lte.valueOf("b.added_at").lessThanEqualToValue(endOfYear)
            )))
            .build();

        // 3. Facet (uguale a prima)
        FacetOperation facets = facet()
            .and(
                project("yearly_books"),
                unwind("yearly_books"),
                group("yearly_books.author.name").count().as("count"),
                sort(Sort.Direction.DESC, "count"),
                limit(3)
            ).as("top_authors")
            .and(
                project("yearly_books"),
                unwind("yearly_books"),
                unwind("yearly_books.genres"),
                group("yearly_books.genres").count().as("count"),
                sort(Sort.Direction.DESC, "count"),
                limit(3)
            ).as("top_genres")
            .and(
                project("best_book", "worst_book")
                .and("yearly_books").size().as("total_books_read")
            ).as("meta");

        Aggregation aggregation = newAggregation(matchUser, prepareData, facets);

        // 4. Esecuzione
        AggregationResults<WrappedAggregationResult> results = 
            mongoTemplate.aggregate(aggregation, "users", WrappedAggregationResult.class);
            
        WrappedAggregationResult rawResult = results.getUniqueMappedResult();

        // 5. Gestione "User Not Found" o "Empty Result"
        // Se l'aggregazione non trova l'utente (perché il match fallisce), rawResult è null.
        if (rawResult == null) {
            // Qui decidi tu: o restituisci un Wrapped vuoto, o lanci l'eccezione che lanciavi prima
            throw new UserNotFoundException("User not found: " + currentUserId);
        }

        // 6. Mapping
        return wrappedMapper.toDTO(rawResult, currentYear);
    }
}
