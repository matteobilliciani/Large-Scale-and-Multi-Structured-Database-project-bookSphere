package it.unipi.bookSphere.service;

import it.unipi.bookSphere.exceptions.AlreadyExistsException;
import it.unipi.bookSphere.exceptions.UnauthorizedOperationException;
import it.unipi.bookSphere.exceptions.UserNotFoundException;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.model.neo4j.UserNode;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
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

import java.util.List;

/**
 * Service for managing user profile operations
 * Handles cross-database updates between MongoDB and Neo4j
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);
    
    private final RegisteredUserRepository userRepository;
    private final UserNodeRepository userNodeRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Update username
     * Updates both MongoDB and Neo4j to maintain consistency
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void updateUsername(String newUsername) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // 1. Validate new username is not empty
        if (newUsername == null || newUsername.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        
        // 2. Check if username is already taken
        if (userRepository.existsByUsername(newUsername)) {
            throw new AlreadyExistsException("Username already exists: " + newUsername);
        }
        
        // 3. Find user
        RegisteredUser user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + currentUserId));
        
        String oldUsername = user.getUsername();
        
        // 4. Update username in MongoDB
        user.setUsername(newUsername);
        userRepository.save(user);
        logger.info("Updated username in MongoDB from {} to {}", oldUsername, newUsername);
        
        try {
            // 5. Update username in Neo4j UserNode
            UserNode userNode = userNodeRepository.findByMongoId(currentUserId).orElse(null);
            if (userNode != null) {
                userNode.setUsername(newUsername);
                userNodeRepository.save(userNode);
                logger.info("Updated username in Neo4j UserNode from {} to {}", oldUsername, newUsername);
            }
            
            // 6. Update username in all reviews (eventual consistency - ASYNC)
            updateUsernameInReviews(currentUserId, newUsername);
            
            // 7. Update username in book snapshots (eventual consistency - ASYNC)
            updateUsernameInBookSnapshots(currentUserId, oldUsername, newUsername);
            
        } catch (Exception e) {
            // If Neo4j update fails, rollback MongoDB
            logger.error("Failed to update username in Neo4j or reviews, rolling back", e);
            user.setUsername(oldUsername);
            userRepository.save(user);
            throw new RuntimeException("Failed to update username: " + e.getMessage(), e);
        }
    }

    /**
     * Delete user account
     * Anonymizes user in both MongoDB and Neo4j according to the consistency table:
     * - Sets status to "deleted"
     * - Removes email
     * - REMOVES username field (unset)
     * - All user's reviews remain but username field is removed
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void deleteAccount() {
        String currentUserId = SecurityUtils.getCurrentUserId();
        
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // 1. Check user exists
        RegisteredUser user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + currentUserId));
        
        String oldUsername = user.getUsername();
        
        logger.info("Deleting account for user: {}", currentUserId);
        
        try {
            // 2. Anonymize user in MongoDB (strict consistency)
            user.setStatus("deleted");
            user.setEmail(null);
            user.setUsername(null); // Remove username
            user.setPasswordHashed(null); // Remove password
            userRepository.save(user);
            logger.info("Deleted user data in MongoDB: {}", currentUserId);
            
            // 3. Update UserNode in Neo4j to "" (strict consistency)
            UserNode userNode = userNodeRepository.findByMongoId(currentUserId).orElse(null);
            if (userNode != null) {
                userNode.setUsername("");
                userNodeRepository.save(userNode);
                logger.info("Anonymized UserNode in Neo4j: {}", currentUserId);
            }
            
            // 4. Remove username field from all reviews (eventual consistency - ASYNC)
            removeUsernameFromReviews(currentUserId);
            
            // 5. Remove username field from book snapshots (eventual consistency - ASYNC)
            removeUsernameFromBookSnapshots(currentUserId, oldUsername);
            
        } catch (Exception e) {
            logger.error("Error deleting account for user {}", currentUserId, e);
            throw new RuntimeException("Failed to delete account: " + e.getMessage(), e);
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Update username in all reviews (ASYNC - eventual consistency)
     * Uses the user's linked reviews list to avoid findByUserId and leverage indexing
     */
    @Async
    private void updateUsernameInReviews(String userId, String newUsername) {
        // Get user's review IDs from the user document
        RegisteredUser user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getReviews() == null || user.getReviews().isEmpty()) {
            logger.info("No reviews to update for user {}", userId);
            return;
        }
        
        // Update each review by ID (leveraging index on _id)
        Query reviewQuery = new Query(Criteria.where("_id").in(user.getReviews()));
        Update reviewUpdate = new Update().set("username", newUsername);
        long updatedCount = mongoTemplate.updateMulti(reviewQuery, reviewUpdate, "reviews").getModifiedCount();
        logger.info("Updated username in {} reviews for user {} using linked reviews list", updatedCount, userId);
    }

    /**
     * Remove username field from all reviews (ASYNC - eventual consistency)
     * Uses the user's linked reviews list to avoid findByUserId and leverage indexing
     */
    @Async
    private void removeUsernameFromReviews(String userId) {
        // Get user's review IDs from the user document
        RegisteredUser user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getReviews() == null || user.getReviews().isEmpty()) {
            logger.info("No reviews to update for user {}", userId);
            return;
        }
        
        // Remove username from each review by ID (leveraging index on _id)
        Query reviewQuery = new Query(Criteria.where("_id").in(user.getReviews()));
        Update reviewUpdate = new Update().unset("username");
        long updatedCount = mongoTemplate.updateMulti(reviewQuery, reviewUpdate, "reviews").getModifiedCount();
        logger.info("Removed username field from {} reviews for user {} using linked reviews list", updatedCount, userId);
    }

    /**
     * Update username in book snapshots (recent_reviews_snapshot and popular_reviews_snapshot)
     * This is an eventual consistency update (ASYNC)
     */
    @Async
    private void updateUsernameInBookSnapshots(String userId, String oldUsername, String newUsername) {
        // Find all books that have reviews from this user in their snapshots
        Query bookQuery = new Query(
            new Criteria().orOperator(
                Criteria.where("recent_reviews_snapshot.user_id").is(userId),
                Criteria.where("popular_reviews_snapshot.user_id").is(userId)
            )
        );
        
        List<BookDocument> books = mongoTemplate.find(bookQuery, BookDocument.class);
        
        for (BookDocument book : books) {
            boolean needsUpdate = false;
            
            // Update recent_reviews_snapshot
            if (book.getRecentReviewsSnapshot() != null) {
                for (BookDocument.ReviewSnapshot snapshot : book.getRecentReviewsSnapshot()) {
                    if (userId.equals(snapshot.getUserId())) {
                        snapshot.setUsername(newUsername);
                        needsUpdate = true;
                    }
                }
            }
            
            // Update popular_reviews_snapshot
            if (book.getPopularReviewsSnapshot() != null) {
                for (BookDocument.ReviewSnapshot snapshot : book.getPopularReviewsSnapshot()) {
                    if (userId.equals(snapshot.getUserId())) {
                        snapshot.setUsername(newUsername);
                        needsUpdate = true;
                    }
                }
            }
            
            // Save the updated book
            if (needsUpdate) {
                Query updateQuery = new Query(Criteria.where("_id").is(book.getId()));
                Update update = new Update()
                    .set("recent_reviews_snapshot", book.getRecentReviewsSnapshot())
                    .set("popular_reviews_snapshot", book.getPopularReviewsSnapshot());
                mongoTemplate.updateFirst(updateQuery, update, BookDocument.class);
            }
        }
        
        logger.info("Updated username in book snapshots from {} to {} for {} books", 
                    oldUsername, newUsername, books.size());
    }

    /**
     * Remove username field from book snapshots (recent_reviews_snapshot and popular_reviews_snapshot)
     * This is an eventual consistency update (ASYNC)
     */
    @Async
    private void removeUsernameFromBookSnapshots(String userId, String oldUsername) {
        // Find all books that have reviews from this user in their snapshots
        Query bookQuery = new Query(
            new Criteria().orOperator(
                Criteria.where("recent_reviews_snapshot.user_id").is(userId),
                Criteria.where("popular_reviews_snapshot.user_id").is(userId)
            )
        );
        
        List<BookDocument> books = mongoTemplate.find(bookQuery, BookDocument.class);
        
        for (BookDocument book : books) {
            boolean needsUpdate = false;
            
            // Remove username from recent_reviews_snapshot
            if (book.getRecentReviewsSnapshot() != null) {
                for (BookDocument.ReviewSnapshot snapshot : book.getRecentReviewsSnapshot()) {
                    if (userId.equals(snapshot.getUserId())) {
                        snapshot.setUsername(null);
                        needsUpdate = true;
                    }
                }
            }
            
            // Remove username from popular_reviews_snapshot
            if (book.getPopularReviewsSnapshot() != null) {
                for (BookDocument.ReviewSnapshot snapshot : book.getPopularReviewsSnapshot()) {
                    if (userId.equals(snapshot.getUserId())) {
                        snapshot.setUsername(null);
                        needsUpdate = true;
                    }
                }
            }
            
            // Save the updated book
            if (needsUpdate) {
                Query updateQuery = new Query(Criteria.where("_id").is(book.getId()));
                Update update = new Update()
                    .set("recent_reviews_snapshot", book.getRecentReviewsSnapshot())
                    .set("popular_reviews_snapshot", book.getPopularReviewsSnapshot());
                mongoTemplate.updateFirst(updateQuery, update, BookDocument.class);
            }
        }
        
        logger.info("Removed username field from book snapshots for user {} in {} books", 
                    userId, books.size());
    }
}
