package it.unipi.bookSphere.service.open;

import it.unipi.bookSphere.dto.*;
import it.unipi.bookSphere.exceptions.InvalidCredentialsException;
import it.unipi.bookSphere.exceptions.UserAlreadyExistsException;
import it.unipi.bookSphere.mapper.UserMapper;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
import it.unipi.bookSphere.validation.NormalizationUtils;
import it.unipi.bookSphere.validation.ValidationUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.ArrayList;

/**
 * Service for handling user authentication and registration
 */
@Service
@RequiredArgsConstructor
@Validated
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final RegisteredUserRepository userRepository;
    private final UserNodeRepository userNodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    /**
     * Register a new user
     * 
     * @param registerDTO Registration data
     * @return UserDTO with user information
     * @throws UserAlreadyExistsException if username or email already exists
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},        
        noRetryFor = {UserAlreadyExistsException.class},        
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public UserDTO register(@Valid RegisterDTO registerDTO) {
        logger.info("Attempting to register user: {}", registerDTO.getUsername());
        
        // Normalize username for consistency
        String normalizedUsername = NormalizationUtils.normalizeUsername(registerDTO.getUsername());
        ValidationUtils.validateUsername(normalizedUsername);        
        // 1. Validate that username/email doesn't exist
        if (userRepository.existsByUsername(normalizedUsername)) {
            logger.warn("Registration failed: username {} already exists", normalizedUsername);
            throw new UserAlreadyExistsException("Username already exists");
        }
        
        ValidationUtils.validateEmail(registerDTO.getEmail());
        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            logger.warn("Registration failed: email {} already exists", registerDTO.getEmail());
            throw new UserAlreadyExistsException("Email already exists");
        }

        // 2. Hash the password using BCryptPasswordEncoder
        String hashedPassword = passwordEncoder.encode(registerDTO.getPassword());

        // 3. Create user in MongoDB with default role "USER" (status: "active")
        RegisteredUser user = new RegisteredUser();
        user.setUsername(normalizedUsername);
        user.setEmail(registerDTO.getEmail());
        user.setPasswordHashed(hashedPassword);
        user.setCountry(registerDTO.getCountry());
        user.setJoinedAt(Instant.now());
        user.setStatus("active");
        user.setBookshelf(new ArrayList<>());
        user.setReviewsYear(new ArrayList<>());
        user.setReviews(new ArrayList<>());
        
        RegisteredUser savedUser = userRepository.save(user);
        logger.info("User saved in MongoDB with id: {}", savedUser.getId());

        // 4. Create user node in Neo4j using repository getOrCreate method
        try {
            userNodeRepository.getOrCreate(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getCountry()
            );
            logger.info("User node created in Neo4j for user: {}", savedUser.getUsername());
        } catch (Exception e) {
            logger.error("Failed to create user node in Neo4j", e);
            // Rollback MongoDB transaction
            userRepository.delete(savedUser);
            throw new RuntimeException("Failed to create user in graph database", e);
        }

        // 5. Convert to DTO using mapper
        UserDTO userDTO = userMapper.toDTO(savedUser);

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

        // Normalize username for search (email doesn't need normalization)
        String normalizedUsernameOrEmail = loginDTO.getUsernameOrEmail();
        if (!normalizedUsernameOrEmail.contains("@")) {
            normalizedUsernameOrEmail = NormalizationUtils.normalizeUsername(normalizedUsernameOrEmail);
        }

        // 1. Find user by username or email
        RegisteredUser user = userRepository.findByUsername(normalizedUsernameOrEmail)
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

        // Check if user is active or is an admin (ADMIN status bypasses active check)
        if (!"active".equals(user.getStatus()) && !"ADMIN".equals(user.getStatus())) {
            logger.warn("Login failed: user {} is not active (status: {})", user.getUsername(), user.getStatus());
            String msg = "banned".equalsIgnoreCase(user.getStatus())
                    ? "Account is banned"
                    : "Account is not active (status: " + user.getStatus() + ")";
            throw new InvalidCredentialsException(msg);
        }

        // 3. Convert to DTO using mapper
        UserDTO userDTO = userMapper.toDTO(user);

        logger.info("Login successful for user: {}", user.getUsername());
        return userDTO;
    }
}
