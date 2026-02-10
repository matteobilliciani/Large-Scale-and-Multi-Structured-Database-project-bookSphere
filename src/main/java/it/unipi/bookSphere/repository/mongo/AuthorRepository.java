package it.unipi.bookSphere.repository.mongo;

import it.unipi.bookSphere.model.mongodb.AuthorDocument;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Search authors by name with pagination (partial match, case-insensitive)
     * 
     * @param name Author name pattern
     * @param pageable Pagination parameters
     * @return Page of matching authors
     */
    Page<AuthorDocument> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    /**
     * Search authors by name with pagination, excluding authors with specific status (partial match, case-insensitive)
     * 
     * @param name Author name pattern
     * @param status Status to exclude (e.g., "ARCHIVED")
     * @param pageable Pagination parameters
     * @return Page of matching authors
     */
    Page<AuthorDocument> findByNameContainingIgnoreCaseAndStatusNot(String name, String status, Pageable pageable);
    
    /**
     * Check if author exists by name
     * 
     * @param name Author name
     * @return true if author exists
     */
    boolean existsByName(String name);
}
