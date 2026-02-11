package it.unipi.bookSphere.service;

import it.unipi.bookSphere.exceptions.AlreadyExistsException;
import it.unipi.bookSphere.exceptions.UnauthorizedOperationException;
import it.unipi.bookSphere.exceptions.UserNotFoundException;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.model.neo4j.UserNode;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
import it.unipi.bookSphere.service.async.AsyncProfileTasks;
import it.unipi.bookSphere.utils.SecurityUtils;
import it.unipi.bookSphere.validation.NormalizationUtils;
import it.unipi.bookSphere.validation.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final UserNodeRepository userNodeRepository;
    private final AsyncProfileTasks asyncProfileTasks;

    /**
     * Update username
     * Updates both MongoDB and Neo4j to maintain consistency
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},        
        noRetryFor = {UnauthorizedOperationException.class, AlreadyExistsException.class},        
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void updateUsername(String newUsername) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // 1. Normalize and validate new username
        String normalizedUsername = NormalizationUtils.normalizeUsername(newUsername);
        ValidationUtils.validateUsername(normalizedUsername);
        
        // 2. Check if username is already taken
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new AlreadyExistsException("Username already exists: " + normalizedUsername);
        }
        
        // 3. Find user
        RegisteredUser user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + currentUserId));
        
        String oldUsername = user.getUsername();
        
        // 4. Update username in MongoDB
        user.setUsername(normalizedUsername);
        userRepository.save(user);
        logger.info("Updated username in MongoDB from {} to {}", oldUsername, normalizedUsername);
        
        try {
            // 5. Update username in Neo4j UserNode
            UserNode userNode = userNodeRepository.findByMongoId(currentUserId).orElse(null);
            if (userNode != null) {
                userNode.setUsername(normalizedUsername);
                userNodeRepository.save(userNode);
                logger.info("Updated username in Neo4j UserNode from {} to {}", oldUsername, normalizedUsername);
            }
            
            // 6. Update username in all reviews (eventual consistency - ASYNC)
            asyncProfileTasks.updateUsernameInReviews(currentUserId, normalizedUsername);
            
            // 7. Update username in book snapshots (eventual consistency - ASYNC)
            asyncProfileTasks.updateUsernameInBookSnapshots(currentUserId, oldUsername, normalizedUsername);
            
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
        noRetryFor = {UnauthorizedOperationException.class},
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
            asyncProfileTasks.removeUsernameFromReviews(currentUserId);
            
            // 5. Remove username field from book snapshots (eventual consistency - ASYNC)
            asyncProfileTasks.removeUsernameFromBookSnapshots(currentUserId, oldUsername);
            
        } catch (Exception e) {
            logger.error("Error deleting account for user {}", currentUserId, e);
            throw new RuntimeException("Failed to delete account: " + e.getMessage(), e);
        }
    }

    // ========== PRIVATE HELPER METHODS ==========
}

