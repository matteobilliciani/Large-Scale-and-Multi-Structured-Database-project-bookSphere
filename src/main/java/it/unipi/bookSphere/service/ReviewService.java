package it.unipi.bookSphere.service;

import it.unipi.bookSphere.dto.ReviewDTO;
import it.unipi.bookSphere.exceptions.*;
import it.unipi.bookSphere.mapper.ReviewMapper;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.model.mongodb.Review;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.model.neo4j.ReviewNode;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.mongo.ReviewRepository;
import it.unipi.bookSphere.repository.neo4j.ReviewNodeRepository;
import it.unipi.bookSphere.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling review operations with eventual consistency between MongoDB and Neo4j
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);
    private static final int MAX_RECENT_REVIEWS = 3;
    
    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final BookRepository bookRepository;
    private final RegisteredUserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    
    // Neo4j repositories
    private final ReviewNodeRepository reviewNodeRepository;


    /**
     * Get reviews by list of IDs
     * Used to fetch all reviews for a book or user given their review IDs array
     * 
     * @param reviewIds List of review MongoDB ObjectIds
     * @return List of ReviewDTOs
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<ReviewDTO> getReviewsByIds(List<String> reviewIds) {
        logger.info("Fetching {} reviews by IDs", reviewIds.size());
        
        List<Review> reviews = reviewRepository.findByIdIn(reviewIds);
        
        if (reviews.isEmpty()) {
            logger.warn("No reviews found for the provided IDs");
        }
        
        logger.info("Found {} reviews", reviews.size());
        return reviews.stream()
                .map(reviewMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new review with eventual consistency
     * - Saves in MongoDB
     * - Creates ReviewNode in Neo4j with relationships
     * - Updates book stats_per_year and snapshots
     * - Updates user reviews_year and reviews array
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ReviewDTO createReview(ReviewDTO reviewDTO) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        String currentUsername = SecurityUtils.getCurrentUsername();
        
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // 1. Validate book exists
        BookDocument book = bookRepository.findById(reviewDTO.getBookId())
                .orElseThrow(() -> new BookNotFoundException("Book not found with ID: " + reviewDTO.getBookId()));
        
        // 2. Validate user exists
        RegisteredUser user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + currentUserId));
        
        // 3. Check if user already has a review for this book (prevent duplicates)
        if (user.getReviews() != null && !user.getReviews().isEmpty()) {
            // Check if any of the user's reviews is for this book
            List<Review> existingReviews = reviewRepository.findByIdIn(user.getReviews());
            boolean alreadyReviewed = existingReviews.stream()
                    .anyMatch(r -> r.getBookSnapshot() != null && 
                                   reviewDTO.getBookId().equals(r.getBookSnapshot().getBookId()));
            if (alreadyReviewed) {
                throw new AlreadyExistsException("You have already reviewed this book");
            }
        }
        
        // 4. Create Review in MongoDB
        // Note: We create the Review manually instead of using reviewMapper.toDocument() 
        // because we need to set several fields that are not in the DTO (userId, createdAt, 
        // likesCount, isBanned, source). The mapper is used for DTO conversion at the end.
        Review review = new Review();
        review.setUserId(currentUserId);
        review.setUsername(currentUsername);
        review.setRating(reviewDTO.getRating());
        review.setText(reviewDTO.getText());
        review.setSummary(reviewDTO.getSummary());
        review.setCreatedAt(Instant.now());
        review.setLikesCount(0);
        review.setIsBanned(false);
        review.setSource("app");
        
        // Set book snapshot
        Review.BookSnapshot bookSnapshot = new Review.BookSnapshot();
        bookSnapshot.setBookId(book.getId());
        bookSnapshot.setTitle(book.getTitle());
        review.setBookSnapshot(bookSnapshot);
        
        Review savedReview = reviewRepository.save(review);
        logger.info("Created review in MongoDB: {}", savedReview.getId());
        
        try {
            // 5. Create ReviewNode in Neo4j with relationships
            createReviewNodeWithRelationships(savedReview, currentUserId, book.getId());
            
            // 6. Update book stats and snapshots (eventual consistency)
            updateBookStatistics(book, savedReview);
            
            // 7. Update user reviews_year and reviews array (eventual consistency)
            updateUserReviews(currentUserId, savedReview, book.getTitle());
            
            // 7. Update author statistics (eventual consistency)
            updateAuthorStatistics(book.getAuthor().getId(), savedReview.getRating());
            
            // 8. Update month score for trending (eventual consistency - ASYNC)
            updateMonthScore(book.getId(), savedReview.getRating(), 1);
            
        } catch (Exception e) {
            // Rollback MongoDB if Neo4j or updates fail
            logger.error("Failed to create review in Neo4j or update statistics, rolling back", e);
            reviewRepository.delete(savedReview);
            throw new RuntimeException("Failed to create review: " + e.getMessage(), e);
        }
        
        ReviewDTO result = reviewMapper.toDTO(savedReview);
        result.setBookTitle(book.getTitle());
        return result;
    }

    /**
     * Update an existing review
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ReviewDTO updateReview(String reviewId, ReviewDTO reviewDTO) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // 1. Find existing review
        Review existingReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with ID: " + reviewId));
        
        // 2. Check ownership
        if (!existingReview.getUserId().equals(currentUserId)) {
            throw new UnauthorizedOperationException("You can only update your own reviews");
        }
        
        // Store old values for stats update
        Integer oldRating = existingReview.getRating();
        String bookId = existingReview.getBookSnapshot().getBookId();
        
        // 3. Update fields
        if (reviewDTO.getRating() != null) {
            existingReview.setRating(reviewDTO.getRating());
        }
        if (reviewDTO.getText() != null) {
            existingReview.setText(reviewDTO.getText());
        }
        if (reviewDTO.getSummary() != null) {
            existingReview.setSummary(reviewDTO.getSummary());
        }
        
        Review updatedReview = reviewRepository.save(existingReview);
        logger.info("Updated review in MongoDB: {}", reviewId);
        
        try {
            // 4. Update ReviewNode in Neo4j
            updateReviewNode(reviewId, updatedReview.getRating());
            
            // 5. Update book stats if rating changed (eventual consistency)
            if (reviewDTO.getRating() != null && !oldRating.equals(reviewDTO.getRating())) {
                BookDocument book = bookRepository.findById(bookId)
                        .orElseThrow(() -> new BookNotFoundException("Book not found"));
                updateBookStatisticsAfterRatingChange(book, oldRating, updatedReview.getRating());
            }
            
            // 6. Update user reviews_year if rating changed
            if (reviewDTO.getRating() != null && !oldRating.equals(reviewDTO.getRating())) {
                updateUserReviewRating(currentUserId, reviewId, updatedReview.getRating());
            }
            
            // 7. Update author statistics if rating changed (eventual consistency)
            if (reviewDTO.getRating() != null && !oldRating.equals(reviewDTO.getRating())) {
                BookDocument book = bookRepository.findById(bookId)
                        .orElseThrow(() -> new BookNotFoundException("Book not found"));
                updateAuthorStatisticsAfterRatingChange(book.getAuthor().getId(), oldRating, updatedReview.getRating());
            }
            
            // 8. Update month score if rating changed (eventual consistency - ASYNC)
            if (reviewDTO.getRating() != null && !oldRating.equals(reviewDTO.getRating())) {
                updateMonthScoreAfterRatingChange(bookId, oldRating, updatedReview.getRating());
            }
            
        } catch (Exception e) {
            logger.error("Failed to update review in Neo4j or statistics", e);
            // Don't rollback MongoDB, log error and continue (eventual consistency will fix)
        }
        
        return reviewMapper.toDTO(updatedReview);
    }

    /**
     * Delete a review
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void deleteReview(String reviewId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // 1. Find existing review
        Review existingReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with ID: " + reviewId));
        
        // 2. Check ownership (or admin)
        boolean isAdmin = SecurityUtils.isAdmin();
        if (!existingReview.getUserId().equals(currentUserId) && !isAdmin) {
            throw new UnauthorizedOperationException("You can only delete your own reviews");
        }
        
        String bookId = existingReview.getBookSnapshot().getBookId();
        Integer rating = existingReview.getRating();
        String userId = existingReview.getUserId();
        
        // 3. Delete from MongoDB
        reviewRepository.deleteById(reviewId);
        logger.info("Deleted review from MongoDB: {}", reviewId);
        
        try {
            // 4. Delete ReviewNode from Neo4j
            reviewNodeRepository.deleteByMongoId(reviewId);
            logger.info("Deleted review from Neo4j: {}", reviewId);
            
            // 5. Remove from book stats and snapshots (eventual consistency)
            BookDocument book = bookRepository.findById(bookId).orElse(null);
            if (book != null) {
                removeReviewFromBookStatistics(book, rating, reviewId);
            }
            
            // 6. Remove from user reviews and reviews_year
            removeReviewFromUser(userId, reviewId);
            
            // 7. Update author statistics (eventual consistency)
            if (book != null) {
                removeReviewFromAuthorStatistics(book.getAuthor().getId(), rating);
            }
            
            // 8. Update month score after removal (eventual consistency - ASYNC)
            if (book != null) {
                updateMonthScore(bookId, -rating, -1);
            }
            
        } catch (Exception e) {
            logger.error("Failed to delete review from Neo4j or update statistics", e);
            // Review already deleted from MongoDB, log the error
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Create ReviewNode in Neo4j with relationships to User and Book
     */
    private void createReviewNodeWithRelationships(Review review, String userId, String bookId) {
        ReviewNode reviewNode = new ReviewNode();
        reviewNode.setMongoId(review.getId());
        reviewNode.setRating(review.getRating());
        reviewNode.setCreatedAt(review.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDateTime());
        
        reviewNodeRepository.save(reviewNode);
        
        // Create relationships
        reviewNodeRepository.createPostedRelationship(userId, review.getId());
        reviewNodeRepository.createReferToRelationship(review.getId(), bookId);
        
        logger.info("Created ReviewNode in Neo4j with relationships: {}", review.getId());
    }

    /**
     * Update ReviewNode rating in Neo4j
     */
    private void updateReviewNode(String reviewId, Integer newRating) {
        ReviewNode reviewNode = reviewNodeRepository.findByMongoId(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("ReviewNode not found in Neo4j"));
        
        reviewNode.setRating(newRating);
        reviewNodeRepository.save(reviewNode);
        logger.info("Updated ReviewNode rating in Neo4j: {}", reviewId);
    }

    /**
     * Update book statistics after new review (eventual consistency - ASYNC)
     * - Updates stats_per_year
     * - Updates recent_reviews_snapshot
     * - Adds review ID to reviews array
     */
    @Async
    private void updateBookStatistics(BookDocument book, Review review) {
        int currentYear = LocalDateTime.now().getYear();
        
        Query query = new Query(Criteria.where("_id").is(book.getId()));
        Update update = new Update();
        
        // 1. Update or create stats_per_year for current year
        boolean yearExists = book.getStatsPerYear() != null && 
            book.getStatsPerYear().stream().anyMatch(s -> s.getYear().equals(currentYear));
        
        if (yearExists) {
            // Increment existing year
            update.inc("stats_per_year.$[elem].ratings_count", 1);
            update.inc("stats_per_year.$[elem].sum_rating", review.getRating());
            update.filterArray(Criteria.where("elem.year").is(currentYear));
        } else {
            // Add new year stat
            BookDocument.YearStat newStat = new BookDocument.YearStat();
            newStat.setYear(currentYear);
            newStat.setRatingsCount(1);
            newStat.setSumRating(review.getRating());
            
            update.push("stats_per_year", newStat);
        }
        
        // 2. Add to recent_reviews_snapshot (keep last 3)
        BookDocument.ReviewSnapshot snapshot = new BookDocument.ReviewSnapshot();
        snapshot.setId(review.getId());
        snapshot.setUserId(review.getUserId());
        snapshot.setUsername(review.getUsername());
        snapshot.setRating(review.getRating());
        snapshot.setSummary(review.getSummary());
        snapshot.setNumOfLike(0);
        snapshot.setDate(review.getCreatedAt());
        
        update.push("recent_reviews_snapshot")
            .slice(-MAX_RECENT_REVIEWS)
            .each(snapshot);
        
        // 3. Add review ID to reviews array
        update.addToSet("reviews", review.getId());
        
        mongoTemplate.updateFirst(query, update, BookDocument.class);
        logger.info("Updated book statistics for book: {}", book.getId());
    }

    /**
     * Update book statistics after rating change (ASYNC)
     */
    @Async
    private void updateBookStatisticsAfterRatingChange(BookDocument book, Integer oldRating, Integer newRating) {
        int currentYear = LocalDateTime.now().getYear();
        
        Query query = new Query(Criteria.where("_id").is(book.getId()));
        Update update = new Update();
        
        // Update stats_per_year
        BookDocument.YearStat stat = book.getStatsPerYear().stream()
            .filter(s -> s.getYear().equals(currentYear))
            .findFirst()
            .orElse(null);
        
        if (stat != null) {
            int newSum = stat.getSumRating() - oldRating + newRating;
            
            update.set("stats_per_year.$[elem].sum_rating", newSum);
            update.filterArray(Criteria.where("elem.year").is(currentYear));
            
            mongoTemplate.updateFirst(query, update, BookDocument.class);
            logger.info("Updated book statistics after rating change for book: {}", book.getId());
        }
    }

    /**
     * Remove review from book statistics after deletion (ASYNC)
     */
    @Async
    private void removeReviewFromBookStatistics(BookDocument book, Integer rating, String reviewId) {
        int currentYear = LocalDateTime.now().getYear();
        
        Query query = new Query(Criteria.where("_id").is(book.getId()));
        Update update = new Update();
        
        // 1. Update stats_per_year
        BookDocument.YearStat stat = book.getStatsPerYear() != null 
            ? book.getStatsPerYear().stream()
                .filter(s -> s.getYear().equals(currentYear))
                .findFirst()
                .orElse(null)
            : null;
        
        if (stat != null && stat.getRatingsCount() > 1) {
            int newSum = stat.getSumRating() - rating;
            
            update.inc("stats_per_year.$[elem].ratings_count", -1);
            update.set("stats_per_year.$[elem].sum_rating", newSum);
            update.filterArray(Criteria.where("elem.year").is(currentYear));
        } else if (stat != null && stat.getRatingsCount() == 1) {
            // Remove the year stat entirely
            update.pull("stats_per_year", Query.query(Criteria.where("year").is(currentYear)));
        }
        
        // 2. Remove from snapshots  
        update.pull("recent_reviews_snapshot", Query.query(Criteria.where("_id").is(reviewId)));
        update.pull("popular_reviews_snapshot", Query.query(Criteria.where("_id").is(reviewId)));
        
        // 3. Remove from reviews array
        update.pull("reviews", reviewId);
        
        mongoTemplate.updateFirst(query, update, BookDocument.class);
        logger.info("Removed review from book statistics: {}", reviewId);
    }

    /**
     * Update user reviews_year and reviews array after new review
     */
    private void updateUserReviews(String userId, Review review, String bookTitle) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update();
        
        // 1. Add to reviews_year
        RegisteredUser.ReviewYear reviewYear = new RegisteredUser.ReviewYear();
        reviewYear.setId(review.getId());
        reviewYear.setRating(review.getRating());
        reviewYear.setBook(bookTitle);
        
        update.addToSet("reviews_year", reviewYear);
        
        // 2. Add to reviews array
        update.addToSet("reviews", review.getId());
        
        mongoTemplate.updateFirst(query, update, RegisteredUser.class);
        logger.info("Updated user reviews for user: {}", userId);
    }

    /**
     * Update user review rating in reviews_year
     */
    private void updateUserReviewRating(String userId, String reviewId, Integer newRating) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update();
        
        update.set("reviews_year.$[elem].rating", newRating);
        update.filterArray(Criteria.where("elem.id").is(reviewId));
        
        mongoTemplate.updateFirst(query, update, RegisteredUser.class);
        logger.info("Updated user review rating for user: {}", userId);
    }

    /**
     * Remove review from user reviews and reviews_year
     */
    private void removeReviewFromUser(String userId, String reviewId) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update();
        
        update.pull("reviews_year", Query.query(Criteria.where("id").is(reviewId)));
        update.pull("reviews", reviewId);
        
        mongoTemplate.updateFirst(query, update, RegisteredUser.class);
        logger.info("Removed review from user: {}", userId);
    }

    /**
     * Update author statistics after new review (eventual consistency - ASYNC)
     */
    @Async
    private void updateAuthorStatistics(String authorId, Integer rating) {
        Query query = new Query(Criteria.where("_id").is(authorId));
        Update update = new Update();
        
        update.inc("ratings_count", 1);
        update.inc("sum_ratings", rating);
        
        mongoTemplate.updateFirst(query, update, AuthorDocument.class);
        
        logger.info("Updated author statistics for author: {}", authorId);
    }

    /**
     * Update author statistics after rating change (eventual consistency - ASYNC)
     */
    @Async
    private void updateAuthorStatisticsAfterRatingChange(String authorId, Integer oldRating, Integer newRating) {
        Query query = new Query(Criteria.where("_id").is(authorId));
        Update update = new Update();
        
        int ratingDiff = newRating - oldRating;
        update.inc("sum_ratings", ratingDiff);
        
        mongoTemplate.updateFirst(query, update, AuthorDocument.class);
        
        logger.info("Updated author statistics after rating change for author: {}", authorId);
    }

    /**
     * Remove review from author statistics (eventual consistency - ASYNC)
     */
    @Async
    private void removeReviewFromAuthorStatistics(String authorId, Integer rating) {
        Query query = new Query(Criteria.where("_id").is(authorId));
        Update update = new Update();
        
        update.inc("ratings_count", -1);
        update.inc("sum_ratings", -rating);
        
        mongoTemplate.updateFirst(query, update, AuthorDocument.class);
        
        logger.info("Removed review from author statistics for author: {}", authorId);
    }

    /**
     * Update month score for trending analysis (eventual consistency - ASYNC)
     * This is called when a new review is added or removed
     * 
     * @param bookId The book ID
     * @param ratingDelta The rating change (positive for add, negative for remove)
     * @param countDelta The count change (1 for add, -1 for remove)
     */
    @Async
    private void updateMonthScore(String bookId, Integer ratingDelta, Integer countDelta) {
        String currentMonth = java.time.YearMonth.now().toString();

        Query query = new Query(Criteria.where("_id").is(bookId));
        
        // Fetch to determine if a monthly reset is needed
        BookDocument book = mongoTemplate.findOne(query, BookDocument.class);

        if (book != null) {
            Update update = new Update();
            BookDocument.MonthScore currentScore = book.getMonthScore();

            // Check for month transition or missing data
            boolean isNewMonth = currentScore == null || 
                                currentScore.getCurrentMonth() == null || 
                                !currentScore.getCurrentMonth().equals(currentMonth);

            if (isNewMonth) {
                // New Month: Full reset
                BookDocument.MonthScore newScore = new BookDocument.MonthScore();
                newScore.setCurrentMonth(currentMonth);
                
                // Initialize values (prevent negatives on reset)
                newScore.setRatingCount(Math.max(countDelta, 0));
                newScore.setSumRating(Math.max(ratingDelta, 0));
                
                update.set("month_score", newScore);
            } else {
                // Same Month: Atomic increment
                update.inc("month_score.rating_count", countDelta);
                update.inc("month_score.sum_rating", ratingDelta);
            }
            
            mongoTemplate.updateFirst(query, update, BookDocument.class);
            logger.info("Updated month score stats for book: {}", bookId);
        }
    }

    /**
     * Update month score after rating change (eventual consistency - ASYNC)
     */
    @Async
    private void updateMonthScoreAfterRatingChange(String bookId, Integer oldRating, Integer newRating) {
        int ratingDelta = newRating - oldRating;
        updateMonthScore(bookId, ratingDelta, 0); // Count stays the same
    }
}
