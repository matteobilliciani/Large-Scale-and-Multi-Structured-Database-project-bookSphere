package it.unipi.bookSphere.service;

import it.unipi.bookSphere.dto.*;
import it.unipi.bookSphere.exceptions.InvalidCredentialsException;
import it.unipi.bookSphere.exceptions.UserAlreadyExistsException;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.model.neo4j.UserNode;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Service for handling user authentication and registration
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final RegisteredUserRepository userRepository;
    private final UserNodeRepository userNodeRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new user
     * 
     * @param registerDTO Registration data
     * @return UserDTO with user information
     * @throws UserAlreadyExistsException if username or email already exists
     */
    @Transactional
    public UserDTO register(RegisterDTO registerDTO) {
        logger.info("Attempting to register user: {}", registerDTO.getUsername());
        
        // 1. Validate that username/email doesn't exist
        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            logger.warn("Registration failed: username {} already exists", registerDTO.getUsername());
            throw new UserAlreadyExistsException("Username already exists");
        }
        
        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            logger.warn("Registration failed: email {} already exists", registerDTO.getEmail());
            throw new UserAlreadyExistsException("Email already exists");
        }

        // 2. Hash the password using BCryptPasswordEncoder
        String hashedPassword = passwordEncoder.encode(registerDTO.getPassword());

        // 3. Create user in MongoDB with default role "USER" (status: "active")
        RegisteredUser user = new RegisteredUser();
        user.setUsername(registerDTO.getUsername());
        user.setEmail(registerDTO.getEmail());
        user.setPasswordHashed(hashedPassword);
        user.setCountry(registerDTO.getCountry());
        user.setJoinedAt(Instant.now());
        user.setStatus("active");
        user.setBookshelf(new ArrayList<>());
        user.setReviewsYear(new ArrayList<>());
        
        RegisteredUser savedUser = userRepository.save(user);
        logger.info("User saved in MongoDB with id: {}", savedUser.getId());

        // 4. Create user node in Neo4j
        try {
            UserNode userNode = new UserNode();
            userNode.setMongoId(savedUser.getId());
            userNode.setUsername(savedUser.getUsername());
            userNode.setCountry(savedUser.getCountry());
            
            userNodeRepository.save(userNode);
            logger.info("User node created in Neo4j for user: {}", savedUser.getUsername());
        } catch (Exception e) {
            logger.error("Failed to create user node in Neo4j", e);
            // Rollback MongoDB transaction
            userRepository.delete(savedUser);
            throw new RuntimeException("Failed to create user in graph database", e);
        }

        // 5. Convert to DTO
        UserDTO userDTO = new UserDTO();
        userDTO.setId(savedUser.getId());
        userDTO.setUsername(savedUser.getUsername());
        userDTO.setEmail(savedUser.getEmail());
        userDTO.setCountry(savedUser.getCountry());
        userDTO.setStatus(savedUser.getStatus());
        userDTO.setJoinedAt(LocalDateTime.ofInstant(savedUser.getJoinedAt(), java.time.ZoneOffset.UTC));
        userDTO.setBookshelf(new ArrayList<>());
        userDTO.setReviewsYear(new ArrayList<>());

        logger.info("User registration completed successfully: {}", savedUser.getUsername());
        return userDTO;
    }

    /**
     * Authenticate user login
     * 
     * @param loginDTO Login credentials
     * @return UserDTO with user information
     * @throws InvalidCredentialsException if credentials are invalid
     */
    public UserDTO login(LoginDTO loginDTO) {
        logger.info("Login attempt for: {}", loginDTO.getUsernameOrEmail());

        // 1. Find user by username or email
        RegisteredUser user = userRepository.findByUsername(loginDTO.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(loginDTO.getUsernameOrEmail()))
                .orElseThrow(() -> {
                    logger.warn("Login failed: user not found - {}", loginDTO.getUsernameOrEmail());
                    return new InvalidCredentialsException("Invalid username/email or password");
                });

        // 2. Verify password using BCryptPasswordEncoder
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPasswordHashed())) {
            logger.warn("Login failed: invalid password for user {}", user.getUsername());
            throw new InvalidCredentialsException("Invalid username/email or password");
        }

        // Check if user is active
        if (!"active".equals(user.getStatus())) {
            logger.warn("Login failed: user {} is not active (status: {})", user.getUsername(), user.getStatus());
            throw new InvalidCredentialsException("Account is not active");
        }

        // 3. Convert to DTO
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setEmail(user.getEmail());
        userDTO.setCountry(user.getCountry());
        userDTO.setStatus(user.getStatus());
        userDTO.setJoinedAt(LocalDateTime.ofInstant(user.getJoinedAt(), java.time.ZoneOffset.UTC));
        userDTO.setBookshelf(new ArrayList<>());
        userDTO.setReviewsYear(new ArrayList<>());

        logger.info("Login successful for user: {}", user.getUsername());
        return userDTO;
    }
}
