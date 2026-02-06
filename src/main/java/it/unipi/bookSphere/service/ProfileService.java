package it.unipi.bookSphere.service;

import it.unipi.bookSphere.exceptions.AlreadyExistsException;
import it.unipi.bookSphere.exceptions.UnauthorizedOperationException;
import it.unipi.bookSphere.exceptions.UserNotFoundException;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.model.neo4j.UserNode;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.mongo.ReviewRepository;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
import it.unipi.bookSphere.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user profile operations
 * Handles cross-database updates between MongoDB and Neo4j
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);
    
    private final RegisteredUserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final UserNodeRepository userNodeRepository;
    private final MongoTemplate mongoTemplate;
    private final Neo4jTemplate neo4jTemplate;

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
        String currentUsername = SecurityUtils.getCurrentUsername();
        
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
            
            // 6. Update username in all reviews (eventual consistency)
            Query reviewQuery = new Query(Criteria.where("user_id").is(currentUserId));
            Update reviewUpdate = new Update().set("username", newUsername);
            mongoTemplate.updateMulti(reviewQuery, reviewUpdate, "reviews");
            logger.info("Updated username in all reviews for user {}", currentUserId);
            
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
     * Removes user from both MongoDB and Neo4j
     * Also deletes all user's reviews
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
        
        logger.info("Deleting account for user: {}", currentUserId);
        
        try {
            // 2. Delete all user's reviews from MongoDB
            reviewRepository.deleteByUserId(currentUserId);
            logger.info("Deleted all reviews for user {}", currentUserId);
            
            // 3. Delete UserNode from Neo4j (with all relationships via DETACH DELETE)
            userNodeRepository.deleteByMongoId(currentUserId);
            logger.info("Deleted UserNode from Neo4j for user {}", currentUserId);
            
            // 4. Delete user from MongoDB
            userRepository.deleteById(currentUserId);
            logger.info("Deleted user from MongoDB: {}", currentUserId);
            
        } catch (Exception e) {
            logger.error("Error deleting account for user {}", currentUserId, e);
            throw new RuntimeException("Failed to delete account: " + e.getMessage(), e);
        }
    }
}
