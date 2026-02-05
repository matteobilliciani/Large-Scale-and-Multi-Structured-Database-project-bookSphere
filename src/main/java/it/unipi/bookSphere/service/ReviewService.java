package it.unipi.bookSphere.service;

import it.unipi.bookSphere.dto.ReviewDTO;
import it.unipi.bookSphere.mapper.ReviewMapper;
import it.unipi.bookSphere.model.mongodb.Review;
import it.unipi.bookSphere.repository.mongo.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for handling review operations
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);
    
    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;

    /**
     * Get reviews by list of IDs
     * Used to fetch all reviews for a book or user given their review IDs array
     * 
     * @param reviewIds List of review MongoDB ObjectIds
     * @return List of ReviewDTOs
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<ReviewDTO> getReviewsByIds(List<String> reviewIds) {
        logger.info("Fetching {} reviews by IDs", reviewIds.size());
        
        List<Review> reviews = reviewRepository.findByIdIn(reviewIds);
        
        if (reviews.isEmpty()) {
            logger.warn("No reviews found for the provided IDs");
        }
        
        logger.info("Found {} reviews", reviews.size());
        return reviews.stream()
                .map(reviewMapper::toDTO)
                .collect(Collectors.toList());
    }
}
