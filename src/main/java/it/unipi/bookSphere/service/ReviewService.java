package it.unipi.bookSphere.service;

import it.unipi.bookSphere.dto.ReviewDTO;
import it.unipi.bookSphere.exceptions.*;
import it.unipi.bookSphere.mapper.ReviewMapper;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.model.mongodb.Review;
import it.unipi.bookSphere.model.neo4j.ReviewNode;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.mongo.ReviewRepository;
import it.unipi.bookSphere.repository.neo4j.ReviewNodeRepository;
import it.unipi.bookSphere.service.async.AsyncReviewTasks;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    
    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final BookRepository bookRepository;
    private final RegisteredUserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    
    // Neo4j repositories
    private final ReviewNodeRepository reviewNodeRepository;
    
    // Async tasks
    private final AsyncReviewTasks asyncReviewTasks;


    /**
     * Get reviews by list of IDs
     * Used to fetch all reviews for a book or user given their review IDs array
     * Maximum 100 IDs per request to prevent performance issues
     * 
     * @param reviewIds List of review MongoDB ObjectIds (max 100)
     * @return List of ReviewDTOs
     * @throws IllegalArgumentException if more than 100 IDs are requested
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<ReviewDTO> getReviewsByIds(List<String> reviewIds) {
        logger.info("Fetching {} reviews by IDs", reviewIds.size());
        
        // Validate maximum number of IDs
        if (reviewIds.size() > 100) {
            throw new IllegalArgumentException("Cannot request more than 100 reviews at once. Requested: " + reviewIds.size());
        }
        
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
        noRetryFor = {UnauthorizedOperationException.class, BookNotFoundException.class, UserNotFoundException.class, AlreadyExistsException.class, BookArchivedException.class},
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
        
        // 2. Validate book is not archived
        if ("ARCHIVED".equals(book.getAvailability())) {
            throw new BookArchivedException("Cannot review an archived book");
        }

        // 3. Validate user exists
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
        // likesCount, source). The mapper is used for DTO conversion at the end.
        Review review = new Review();
        review.setUserId(currentUserId);
        review.setUsername(currentUsername);
        review.setRating(reviewDTO.getRating());
        review.setText(reviewDTO.getText());
        review.setSummary(reviewDTO.getSummary());
        review.setCreatedAt(Instant.now());
        review.setLikesCount(0);
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
            asyncReviewTasks.updateBookStatistics(book, savedReview);
            
            // 7. Update user reviews_year and reviews array (eventual consistency)
            updateUserReviews(currentUserId, savedReview, book.getTitle());
            
            // 7. Update author statistics (eventual consistency)
            asyncReviewTasks.updateAuthorStatistics(book.getAuthor().getId(), savedReview.getRating());
            
            // 8. Update month score for trending (eventual consistency - ASYNC)
            asyncReviewTasks.updateMonthScore(savedReview, savedReview.getRating(), 1);
            
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
        noRetryFor = {UnauthorizedOperationException.class, ReviewNotFoundException.class, BookNotFoundException.class, UserNotFoundException.class, BookArchivedException.class},
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
        
        // 3. Check if book is archived (if rating changes)
        String bookId = existingReview.getBookSnapshot().getBookId();
        if (reviewDTO.getRating() != null && !existingReview.getRating().equals(reviewDTO.getRating())) {
            BookDocument book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new BookNotFoundException("Book not found"));
            if ("ARCHIVED".equals(book.getAvailability())) {
                throw new BookArchivedException("The book is now archived");
            }
        }

        // Store old values for stats update
        Integer oldRating = existingReview.getRating();
        
        // 4. Update fields
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
        } catch (Exception e) {
            logger.error("Failed to update review in Neo4j", e);
        }
        
        try {
            // 5. Update book stats if rating changed (eventual consistency)
            if (reviewDTO.getRating() != null && !oldRating.equals(reviewDTO.getRating())) {
                BookDocument book = bookRepository.findById(bookId)
                        .orElseThrow(() -> new BookNotFoundException("Book not found"));
                
                asyncReviewTasks.updateBookStatisticsAfterRatingChange(book, updatedReview, oldRating);
            }
        
            // 6. Update user reviews_year if rating changed
            if (reviewDTO.getRating() != null && !oldRating.equals(reviewDTO.getRating())) {
                updateUserReviewRating(currentUserId, reviewId, updatedReview.getRating());
            }
        
            // 7. Update author statistics if rating changed (eventual consistency)
            if (reviewDTO.getRating() != null && !oldRating.equals(reviewDTO.getRating())) {
                BookDocument book = bookRepository.findById(bookId)
                        .orElseThrow(() -> new BookNotFoundException("Book not found"));
                asyncReviewTasks.updateAuthorStatisticsAfterRatingChange(book.getAuthor().getId(), oldRating, updatedReview.getRating());
            }
        
            // 8. Update month score if rating changed (eventual consistency - ASYNC)
            if (reviewDTO.getRating() != null && !oldRating.equals(reviewDTO.getRating())) {
                asyncReviewTasks.updateMonthScoreAfterRatingChange(updatedReview, oldRating);
            }
            
        } catch (Exception e) {
            logger.error("Failed to update statistics after review change", e);
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
        noRetryFor = {UnauthorizedOperationException.class, ReviewNotFoundException.class},
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
        
        deleteReviewNoFilter(reviewId);
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Used to remove review when banning an user in async mode
     */
    public void deleteReviewNoFilter(String reviewId) {    
        // 1. Find existing review
        Review existingReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with ID: " + reviewId));
        
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
                asyncReviewTasks.removeReviewFromBookStatistics(book, existingReview);
            }
            
            // 6. Remove from user reviews and reviews_year
            removeReviewFromUser(userId, reviewId);
            
            // 7. Update author statistics (eventual consistency)
            if (book != null) {
                asyncReviewTasks.removeReviewFromAuthorStatistics(book.getAuthor().getId(), rating);
            }
            
            // 8. Update month score after removal (eventual consistency - ASYNC)
            if (book != null) {
                asyncReviewTasks.updateMonthScore(existingReview, -rating, -1);
            }
            
        } catch (Exception e) {
            logger.error("Failed to delete review from Neo4j or update statistics", e);
            // Review already deleted from MongoDB, log the error
        }
    }

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
}
