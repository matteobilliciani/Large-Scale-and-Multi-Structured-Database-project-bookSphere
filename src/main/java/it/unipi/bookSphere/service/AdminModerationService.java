package it.unipi.bookSphere.service;

import it.unipi.bookSphere.dto.ReviewDTO;
import it.unipi.bookSphere.dto.UserDTO;
import it.unipi.bookSphere.exceptions.ReviewNotFoundException;
import it.unipi.bookSphere.exceptions.UserNotFoundException;
import it.unipi.bookSphere.mapper.ReviewMapper;
import it.unipi.bookSphere.mapper.UserMapper;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.model.mongodb.Review;
import it.unipi.bookSphere.repository.mongo.AuthorRepository;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.mongo.ReviewRepository;
import it.unipi.bookSphere.repository.neo4j.ReviewNodeRepository;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Admin service for content moderation and user management.
 * Implements both strict and eventual consistency patterns for moderation operations.
 */
@Service
@RequiredArgsConstructor
public class AdminModerationService {

    private static final Logger logger = LoggerFactory.getLogger(AdminModerationService.class);
    
    private final ReviewRepository reviewRepository;
    private final RegisteredUserRepository userRepository;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final ReviewNodeRepository reviewNodeRepository;
    private final UserNodeRepository userNodeRepository;
    private final MongoTemplate mongoTemplate;
    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper;

    /**
     * Delete a review (content moderation).
     * 
     * STRICT CONSISTENCY (Synchronous):
     * - MONGO: Delete document in reviews collection
     * - MONGO: $pull from review_ids and snapshots in books
     * - NEO4J: DETACH DELETE review node
     * 
     * EVENTUAL CONSISTENCY (Asynchronous):
     * - MONGO: Update stats in books and authors
     * - MONGO: $pull from review_ids and reviews_year in users
     * 
     * Purpose: Removal of toxic content
     * 
     * @param reviewId Review MongoDB ObjectId
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void deleteReview(String reviewId) {
        logger.info("Admin deleting review with ID: {}", reviewId);
        
        // 1. Find review to get metadata before deletion
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with ID: " + reviewId));
        
        String bookId = review.getBookSnapshot().getBookId();
        String userId = review.getUserId();
        Integer rating = review.getRating();
        Instant createdAt = review.getCreatedAt();
        
        try {
            // 2. STRICT: Delete review document from MongoDB
            reviewRepository.delete(review);
            logger.info("Review deleted from MongoDB");
            
            // 3. STRICT: Remove review from book's review_ids and snapshots
            removeReviewFromBook(bookId, reviewId);
            logger.info("Review removed from book review_ids and snapshots");
            
            // 4. STRICT: Delete ReviewNode from Neo4j
            reviewNodeRepository.deleteByMongoId(reviewId);
            logger.info("Review node deleted from Neo4j");
            
            // 5. EVENTUAL: Update book statistics (asynchronous)
            updateBookStatisticsAfterReviewDeletion(bookId, rating, createdAt);
            
            // 6. EVENTUAL: Update author statistics (asynchronous)
            updateAuthorStatisticsAfterReviewDeletion(bookId, rating, createdAt);
            
            // 7. EVENTUAL: Remove review from user's reviews_year and review_ids (asynchronous)
            removeReviewFromUser(userId, reviewId);
            
            logger.info("Review deletion completed successfully");
            
        } catch (Exception e) {
            logger.error("Failed to delete review", e);
            throw new RuntimeException("Failed to delete review: " + e.getMessage(), e);
        }
    }

    /**
     * Ban a user from the platform.
     * 
     * STRICT CONSISTENCY (Synchronous):
     * - MONGO: Set status: "BANNED" in users collection
     * - NEO4J: Set label (:BannedUser) on user node
     * 
     * EVENTUAL CONSISTENCY (Asynchronous):
     * - MONGO: Set is_banned: true on all user's reviews
     * - MONGO: $pull reviews from book snapshots
     * 
     * @param userId User MongoDB ObjectId
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void banUser(String userId) {
        logger.info("Admin banning user with ID: {}", userId);
        
        // 1. STRICT: Update user status in MongoDB
        RegisteredUser user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        
        user.setStatus("BANNED");
        userRepository.save(user);
        logger.info("User status set to BANNED in MongoDB");
        
        try {
            // 2. STRICT: Add BannedUser label in Neo4j
            userNodeRepository.addBannedLabel(userId);
            logger.info("BannedUser label added in Neo4j");
            
            // 3. EVENTUAL: Mark all user's reviews as banned (asynchronous)
            markUserReviewsAsBanned(userId);
            
            // 4. EVENTUAL: Remove user's reviews from book snapshots (asynchronous)
            removeUserReviewsFromBookSnapshots(userId);
            
            logger.info("User ban completed successfully");
            
        } catch (Exception e) {
            logger.error("Failed to ban user in Neo4j", e);
            throw new RuntimeException("Failed to ban user: " + e.getMessage(), e);
        }
    }

    /**
     * Get all users for moderation purposes.
     * 
     * @param page Page number
     * @param size Page size
     * @return Paginated list of users
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Page<UserDTO> getAllUsers(int page, int size) {
        logger.info("Admin retrieving all users (page: {}, size: {})", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<RegisteredUser> users = userRepository.findAll(pageable);
        
        logger.info("Retrieved {} users", users.getTotalElements());
        return users.map(userMapper::toDTO);
    }

    /**
     * Get all reviews for moderation purposes.
     * 
     * @param page Page number
     * @param size Page size
     * @return Paginated list of reviews
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Page<ReviewDTO> getAllReviews(int page, int size) {
        logger.info("Admin retrieving all reviews (page: {}, size: {})", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviews = reviewRepository.findAll(pageable);
        
        logger.info("Retrieved {} reviews", reviews.getTotalElements());
        return reviews.map(reviewMapper::toDTO);
    }

    // ========== PRIVATE HELPER METHODS (STRICT) ==========

    /**
     * Remove review from book's review_ids and snapshots (STRICT)
     */
    private void removeReviewFromBook(String bookId, String reviewId) {
        Query query = new Query(Criteria.where("_id").is(bookId));
        Update update = new Update()
                .pull("reviews", reviewId)
                .pull("recent_reviews_snapshot", Query.query(Criteria.where("review_id").is(reviewId)))
                .pull("popular_reviews_snapshot", Query.query(Criteria.where("review_id").is(reviewId)));
        
        mongoTemplate.updateFirst(query, update, BookDocument.class);
    }

    // ========== PRIVATE HELPER METHODS (EVENTUAL - ASYNC) ==========

    /**
     * Update book statistics after review deletion (EVENTUAL - ASYNC)
     */
    @Async
    private void updateBookStatisticsAfterReviewDeletion(String bookId, Integer rating, Instant reviewCreatedAt) {
        try {
            logger.info("ASYNC: Updating book statistics after review deletion");
            
            BookDocument book = bookRepository.findById(bookId).orElse(null);
            if (book == null || book.getStatsPerYear() == null) {
                logger.warn("Book not found or no stats for update: {}", bookId);
                return;
            }
            
            // Get year from review createdAt
            int reviewYear = LocalDateTime.ofInstant(reviewCreatedAt, java.time.ZoneId.systemDefault()).getYear();
            
            // Find and update stats_per_year for that year
            Query query = new Query(Criteria.where("_id").is(bookId));
            Update update = new Update();
            
            BookDocument.YearStat yearStat = book.getStatsPerYear().stream()
                .filter(s -> s.getYear().equals(reviewYear))
                .findFirst()
                .orElse(null);
            
            if (yearStat != null && yearStat.getRatingsCount() > 0) {
                int newCount = Math.max(0, yearStat.getRatingsCount() - 1);
                int newSum = Math.max(0, yearStat.getSumRating() - rating);
                
                update.set("stats_per_year.$[elem].ratings_count", newCount);
                update.set("stats_per_year.$[elem].sum_rating", newSum);
                update.filterArray(Criteria.where("elem.year").is(reviewYear));
                
                mongoTemplate.updateFirst(query, update, BookDocument.class);
                logger.info("ASYNC: Book statistics updated successfully for year {}", reviewYear);
            } else {
                logger.warn("ASYNC: No year stat found for year {} or count already 0", reviewYear);
            }
            
        } catch (Exception e) {
            logger.error("ASYNC: Failed to update book statistics", e);
        }
    }

    /**
     * Update author statistics after review deletion (EVENTUAL - ASYNC)
     * Note: Author statistics are aggregated across all books, so we update global counters
     */
    @Async
    private void updateAuthorStatisticsAfterReviewDeletion(String bookId, Integer rating, Instant reviewCreatedAt) {
        try {
            logger.info("ASYNC: Updating author statistics after review deletion");
            
            BookDocument book = bookRepository.findById(bookId).orElse(null);
            if (book == null || book.getAuthor() == null) {
                logger.warn("Book or author not found for stats update");
                return;
            }
            
            String authorId = book.getAuthor().getId();
            AuthorDocument author = authorRepository.findById(authorId).orElse(null);
            if (author == null) {
                logger.warn("Author not found: {}", authorId);
                return;
            }
            
            // Update author global statistics
            int currentCount = author.getRatingsCount() != null ? author.getRatingsCount() : 0;
            int currentSum = author.getSumRatings() != null ? author.getSumRatings() : 0;
            
            int newCount = Math.max(0, currentCount - 1);
            int newSum = Math.max(0, currentSum - rating);
            
            author.setRatingsCount(newCount);
            author.setSumRatings(newSum);
            
            authorRepository.save(author);
            logger.info("ASYNC: Author statistics updated successfully");
            
        } catch (Exception e) {
            logger.error("ASYNC: Failed to update author statistics", e);
        }
    }

    /**
     * Remove review from user's reviews_year and review_ids (EVENTUAL - ASYNC)
     */
    @Async
    private void removeReviewFromUser(String userId, String reviewId) {
        try {
            logger.info("ASYNC: Removing review from user's data");
            
            Query query = new Query(Criteria.where("_id").is(userId));
            Update update = new Update()
                    .pull("reviews", reviewId)
                    .pull("reviews_year.$[].review_ids", reviewId);
            
            mongoTemplate.updateFirst(query, update, RegisteredUser.class);
            logger.info("ASYNC: Review removed from user's data successfully");
            
        } catch (Exception e) {
            logger.error("ASYNC: Failed to remove review from user", e);
        }
    }

    /**
     * Mark all user's reviews as banned (EVENTUAL - ASYNC)
     */
    @Async
    private void markUserReviewsAsBanned(String userId) {
        try {
            logger.info("ASYNC: Marking all reviews from user {} as banned", userId);
            
            Query query = new Query(Criteria.where("user_id").is(userId));
            Update update = new Update().set("is_banned", true);
            
            mongoTemplate.updateMulti(query, update, Review.class);
            logger.info("ASYNC: User reviews marked as banned successfully");
            
        } catch (Exception e) {
            logger.error("ASYNC: Failed to mark user reviews as banned", e);
        }
    }

    /**
     * Remove user's reviews from all book snapshots (EVENTUAL - ASYNC)
     */
    @Async
    private void removeUserReviewsFromBookSnapshots(String userId) {
        try {
            logger.info("ASYNC: Removing user reviews from book snapshots");
            
            // Find all reviews by the user
            List<Review> userReviews = reviewRepository.findByUserId(userId);
            
            // For each review, remove it from the book's snapshots
            for (Review review : userReviews) {
                try {
                    String bookId = review.getBookSnapshot().getBookId();
                    String reviewId = review.getId();
                    
                    Query query = new Query(Criteria.where("_id").is(bookId));
                    Update update = new Update()
                            .pull("recent_reviews_snapshot", Query.query(Criteria.where("review_id").is(reviewId)))
                            .pull("popular_reviews_snapshot", Query.query(Criteria.where("review_id").is(reviewId)));
                    
                    mongoTemplate.updateFirst(query, update, BookDocument.class);
                    
                } catch (Exception e) {
                    logger.warn("Failed to remove review snapshot for review: {}", review.getId(), e);
                }
            }
            
            logger.info("ASYNC: User reviews removed from book snapshots successfully");
            
        } catch (Exception e) {
            logger.error("ASYNC: Failed to remove user reviews from book snapshots", e);
        }
    }
}
