package it.unipi.bookSphere.service;

import it.unipi.bookSphere.dto.UserDTO;
import it.unipi.bookSphere.exceptions.AlreadyExistsException;
import it.unipi.bookSphere.exceptions.UnauthorizedOperationException;
import it.unipi.bookSphere.exceptions.UserNotFoundException;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
import it.unipi.bookSphere.repository.neo4j.projections.UserFollowProjection;
import it.unipi.bookSphere.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for managing user follow relationships in Neo4j
 */
@Service
@RequiredArgsConstructor
public class FollowService {

    private static final Logger logger = LoggerFactory.getLogger(FollowService.class);
    
    private final UserNodeRepository userNodeRepository;
    private final RegisteredUserRepository userRepository;

    /**
     * Follow another user
     * Creates FOLLOWS relationship in Neo4j
     * Only accepts user ID (not username)
     * Validates that target user exists and is ACTIVE (not banned or admin)
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},        
        noRetryFor = {UnauthorizedOperationException.class, UserNotFoundException.class, AlreadyExistsException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void followUser(String targetUserId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // 1. Validate target user exists in MongoDB
        RegisteredUser targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + targetUserId));
        
        // 2. Validate target user is ACTIVE (not banned or admin)
        if (!"active".equals(targetUser.getStatus())) {
            throw new UnauthorizedOperationException("Cannot follow this user: user is not active (status: " + targetUser.getStatus() + ")");
        }
        
        // 3. Check not trying to follow self
        if (currentUserId.equals(targetUserId)) {
            throw new AlreadyExistsException("Cannot follow yourself");
        }
        
        // 4. Get or create current UserNode using repository method
        RegisteredUser currentUser = userRepository.findById(currentUserId).orElse(null);
        userNodeRepository.getOrCreate(
            currentUserId,
            SecurityUtils.getCurrentUsername(),
            currentUser != null ? currentUser.getCountry() : null
        );
        
        // 5. Get or create target UserNode using repository method
        userNodeRepository.getOrCreate(
            targetUserId,
            targetUser.getUsername(),
            targetUser.getCountry()
        );
        
        // 6. Check if already following using repository method
        boolean alreadyFollowing = userNodeRepository.isFollowing(currentUserId, targetUserId);
        
        if (alreadyFollowing) {
            throw new AlreadyExistsException("Already following this user");
        }
        
        // 7. Create FOLLOWS relationship using repository method
        userNodeRepository.createFollowsRelationship(currentUserId, targetUserId, LocalDateTime.now());
        
        logger.info("User {} is now following user {}", currentUserId, targetUserId);
    }

    /**
     * Unfollow a user
     * Removes FOLLOWS relationship from Neo4j
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        noRetryFor = {UnauthorizedOperationException.class, UserNotFoundException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void unfollowUser(String targetUserId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // 1. Check target user exists and is ACTIVE
        userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + targetUserId));
        
        // 2. Check if currently following
        boolean isFollowing = userNodeRepository.isFollowing(currentUserId, targetUserId);
        
        if (!isFollowing) {
            throw new UserNotFoundException("You are not following this user");
        }
        
        // 3. Remove FOLLOWS relationship using repository method
        Long deleted = userNodeRepository.deleteFollowsRelationship(currentUserId, targetUserId);
        
        if (deleted == 0) {
            logger.warn("User {} was not following user {}", currentUserId, targetUserId);
        } else {
            logger.info("User {} unfollowed user {}", currentUserId, targetUserId);
        }
    }

    /**
     * Get all users followed by current user (friends)
     * Queries Neo4j for FOLLOWS relationships using repository
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<UserDTO> getFollowedUsers() {
        String currentUserId = SecurityUtils.getCurrentUserId();
        
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // Query Neo4j for followed users using repository method
        // Should not return user that are not ACTIVE
        List<UserFollowProjection> results = userNodeRepository.getFollowedUsers(currentUserId);
        
        // Convert to DTOs
        List<UserDTO> followedUsers = new ArrayList<>();
        for (UserFollowProjection result : results) {
            UserDTO dto = new UserDTO();
            dto.setId(result.userId());
            dto.setUsername(result.username());
            dto.setCountry(result.country());
            followedUsers.add(dto);
        }
        
        logger.info("Found {} followed users for user {}", followedUsers.size(), currentUserId);
        return followedUsers;
    }
    
    /**
     * Get all users followed by current user (friends) with pagination
     * Queries Neo4j for FOLLOWS relationships using repository
     * 
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Paginated list of followed users
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Page<UserDTO> getFollowedUsers(int page, int size) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        long skip = (long) page * size;
        
        // Query Neo4j for followed users with pagination
        List<UserFollowProjection> results = userNodeRepository.getFollowedUsers(currentUserId, skip, size);
        
        // Get total count for pagination
        long total = userNodeRepository.countFollowedUsers(currentUserId);
        
        // Convert to DTOs
        List<UserDTO> followedUsers = new ArrayList<>();
        for (UserFollowProjection result : results) {
            UserDTO dto = new UserDTO();
            dto.setId(result.userId());
            dto.setUsername(result.username());
            dto.setCountry(result.country());
            followedUsers.add(dto);
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<UserDTO> pageResult = new PageImpl<>(followedUsers, pageable, total);
        
        logger.info("Found {} followed users for user {} on page {}", followedUsers.size(), currentUserId, page);
        return pageResult;
    }
}
