package it.unipi.bookSphere.service.open;

import it.unipi.bookSphere.dto.UserDTO;
import it.unipi.bookSphere.exceptions.UserNotFoundException;
import it.unipi.bookSphere.mapper.UserMapper;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Service for handling user operations (public profile views)
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final RegisteredUserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Find user by username
     * 
     * @param username Username
     * @return UserDTO with user information
     * @throws UserNotFoundException if user is not found
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public UserDTO findByUsername(String username) {
        logger.info("Finding user by username: {}", username);
        
        RegisteredUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("User not found with username: {}", username);
                    return new UserNotFoundException("User not found with username: " + username);
                });
        
        // Verify that the user has active or admin status
        if (!"active".equals(user.getStatus()) && !"ADMIN".equals(user.getStatus())) {
            logger.warn("User {} is not active (status: {})", username, user.getStatus());
            throw new UserNotFoundException("User not found with username: " + username);
        }
        
        UserDTO userDTO = userMapper.toDTO(user);
        logger.info("User found: {}", user.getUsername());
        return userDTO;
    }

    /**
     * Find user by ID
     * 
     * @param id MongoDB ObjectId of the user
     * @return UserDTO with user information
     * @throws UserNotFoundException if user is not found
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public UserDTO findById(String id) {
        logger.info("Finding user by id: {}", id);
        
        RegisteredUser user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("User not found with id: {}", id);
                    return new UserNotFoundException("User not found with id: " + id);
                });
        
        // Verify that the user has active or admin status
        if (!"active".equals(user.getStatus()) && !"ADMIN".equals(user.getStatus())) {
            logger.warn("User with id {} is not active (status: {})", id, user.getStatus());
            throw new UserNotFoundException("User not found with id: " + id);
        }
        
        UserDTO userDTO = userMapper.toDTO(user);
        logger.info("User found: {}", user.getUsername());
        return userDTO;
    }
}
