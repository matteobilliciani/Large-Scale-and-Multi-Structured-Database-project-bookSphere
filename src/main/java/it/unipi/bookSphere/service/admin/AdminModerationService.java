package it.unipi.bookSphere.service.admin;

import it.unipi.bookSphere.dto.ReviewDTO;
import it.unipi.bookSphere.dto.UserDTO;
import it.unipi.bookSphere.exceptions.ReviewNotFoundException;
import it.unipi.bookSphere.exceptions.UserAlreadyBannedException;
import it.unipi.bookSphere.exceptions.UserNotFoundException;
import it.unipi.bookSphere.mapper.ReviewMapper;
import it.unipi.bookSphere.mapper.UserMapper;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.model.mongodb.Review;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.mongo.ReviewRepository;
import it.unipi.bookSphere.service.async.AsyncAdminModerationTasks;
import it.unipi.bookSphere.service.open.ReviewService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Admin service for content moderation and user management.
 * Implements both strict and eventual consistency patterns for moderation operations.
 */
@Service
@RequiredArgsConstructor
public class AdminModerationService {

    private static final Logger logger = LoggerFactory.getLogger(AdminModerationService.class);
    
    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;
    private final RegisteredUserRepository userRepository;
    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper;
    private final AsyncAdminModerationTasks asyncAdminModerationTasks;

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
        noRetryFor = {ReviewNotFoundException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void deleteReview(String reviewId) {
        logger.info("Admin deleting review with ID: {}", reviewId);
        reviewService.deleteReview(reviewId);
        logger.info("Review deletion completed successfully via ReviewService");
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
        noRetryFor = {UserNotFoundException.class, UserAlreadyBannedException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void banUser(String userId) {
        logger.info("Admin banning user with ID: {}", userId);
        
        // 1. STRICT: Update user status in MongoDB
        RegisteredUser user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // User already banned - this is not a transient error, skip gracefully
        if(user.getStatus().equals("BANNED")){
            logger.warn("User {} is already banned, skipping redundant ban operation", userId);
            throw new UserAlreadyBannedException("User already banned with ID: " + userId);
        }
        
        user.setStatus("BANNED");
        user.setUsername(null);
        userRepository.save(user);
        logger.info("User status set to BANNED in MongoDB");
        
        try {
            // 2. STRICT: Dedlete User Node
            asyncAdminModerationTasks.deleteUserNode(userId);
            
            // 3. EVENTUAL: delete all user's reviews (asynchronous)
            asyncAdminModerationTasks.removeUserReviews(user);
            
//            // 4. EVENTUAL: Remove user's reviews from book snapshots (asynchronous)
//            removeUserReviewsFromBookSnapshots(userId);
            
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
}
