package it.unipi.bookSphere.repository.mongo;

import it.unipi.bookSphere.model.mongodb.AuthorDocument;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AuthorDocument MongoDB operations
 */
@Repository
public interface AuthorRepository extends MongoRepository<AuthorDocument, String> {
    
    /**
     * Find author by id
     * 
     * @param id IDs
     * @return Author if found
     */
    Optional<AuthorDocument> findById(String id);

    /**
     * Find author by name (exact match)
     * 
     * @param name Author name
     * @return Optional containing the author if found
     */
    Optional<AuthorDocument> findByName(String name);
    
    /**
     * Search authors by name (partial match, case-insensitive)
     * 
     * @param name Author name pattern
     * @return List of matching authors
     */
    List<AuthorDocument> findByNameContainingIgnoreCase(String name);
    
    /**
     * Check if author exists by name
     * 
     * @param name Author name
     * @return true if author exists
     */
    boolean existsByName(String name);
}
