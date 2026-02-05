package it.unipi.bookSphere.repository.mongo;

import it.unipi.bookSphere.model.mongodb.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
