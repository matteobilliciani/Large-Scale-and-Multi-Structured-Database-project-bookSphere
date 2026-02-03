package it.unipi.bookSphere.repository.mongo;

import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing RegisteredUser documents in MongoDB
 */
@Repository
public interface RegisteredUserRepository extends MongoRepository<RegisteredUser, String> {
    
    /**
     * Find user by username
     * @param username Username
     * @return Optional containing user if found
     */
    Optional<RegisteredUser> findByUsername(String username);
    
    /**
     * Find user by email
     * @param email Email address
     * @return Optional containing user if found
     */
    Optional<RegisteredUser> findByEmail(String email);
    
    /**
     * Check if username exists
     * @param username Username
     * @return true if username exists
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if email exists
     * @param email Email address
     * @return true if email exists
     */
    boolean existsByEmail(String email);
}
