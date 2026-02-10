package it.unipi.bookSphere.repository.mongo;

import it.unipi.bookSphere.model.mongodb.BookDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for BookDocument MongoDB operations
 */
@Repository
public interface BookRepository extends MongoRepository<BookDocument, String> {
    
    /**
     * Find book by id
     * 
     * @param id IDs
     * @return Book if found
     */
    Optional<BookDocument> findById(String id);

    /**
     * Find book by title (exact match)
     * 
     * @param title Book title
     * @return Optional containing the book if found
     */
    Optional<BookDocument> findByTitle(String title);
    
    /**
     * Search books by title (partial match, case-insensitive)
     * 
     * @param title Book title pattern
     * @return List of matching books
     */
    List<BookDocument> findByTitleContainingIgnoreCase(String title);
    
    /**
     * Search books by title with pagination (partial match, case-insensitive)
     * 
     * @param title Book title pattern
     * @param pageable Pagination parameters
     * @return Page of matching books
     */
    Page<BookDocument> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    
    /**
     * Search books by title with pagination, excluding books with specific availability (partial match, case-insensitive)
     * 
     * @param title Book title pattern
     * @param availability Availability to exclude (e.g., "ARCHIVED")
     * @param pageable Pagination parameters
     * @return Page of matching books
     */
    Page<BookDocument> findByTitleContainingIgnoreCaseAndAvailabilityNot(String title, String availability, Pageable pageable);
}
