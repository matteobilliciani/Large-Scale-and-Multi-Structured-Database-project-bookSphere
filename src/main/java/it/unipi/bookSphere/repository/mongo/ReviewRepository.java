package it.unipi.bookSphere.repository.mongo;

import it.unipi.bookSphere.model.mongodb.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Review MongoDB operations
 */
@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {
    
    /**
     * Find reviews by list of IDs
     * Used to fetch all reviews for a book or user given their review IDs array
     * 
     * @param ids List of review IDs
     * @return List of reviews
     */
    List<Review> findByIdIn(List<String> ids);
    
    /**
     * Find reviews by user ID
     */
    List<Review> findByUserId(String userId);
    
    /**
     * Find reviews by book ID (from book_snapshot)
     */
    List<Review> findByBookSnapshot_BookId(String bookId);
    
    /**
     * Count reviews by user ID
     */
    long countByUserId(String userId);
    
    /**
     * Delete all reviews by user ID
     */
    void deleteByUserId(String userId);
}
