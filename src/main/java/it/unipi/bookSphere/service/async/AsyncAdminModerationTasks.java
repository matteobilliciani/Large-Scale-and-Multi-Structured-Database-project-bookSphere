package it.unipi.bookSphere.service.async;

import it.unipi.bookSphere.exceptions.ReviewNotFoundException;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
import it.unipi.bookSphere.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Async tasks for admin moderation operations.
 * These methods are called asynchronously to avoid blocking the main request thread.
 */
@Service
@RequiredArgsConstructor
public class AsyncAdminModerationTasks {

    private static final Logger logger = LoggerFactory.getLogger(AsyncAdminModerationTasks.class);

    private final ReviewService reviewService;
    private final UserNodeRepository userNodeRepository;

    /**
     * Mark all user's reviews as banned (EVENTUAL - ASYNC)
     */
    @Async
    public void removeUserReviews(RegisteredUser user) {
        // 2. REUSE LOGIC: Delete all reviews associated with the user
        // We create a copy of the list to avoid concurrent modification issues during iteration
        List<String> userReviews = user.getReviews();
        
        if (userReviews != null && !userReviews.isEmpty()) {
            logger.info("Deleting {} reviews for banned user {}", userReviews.size(), user.getId());
            
            // Create a safe copy of the IDs to iterate over
            List<String> reviewsToDelete = List.copyOf(userReviews);
            
            for (String reviewId : reviewsToDelete) {
                try {
                    // Reuse existing logic
                    reviewService.deleteReviewNoFilter(reviewId);
                } catch (ReviewNotFoundException e) {
                    logger.warn("Review {} already deleted or not found during ban process", reviewId);
                } catch (Exception e) {
                    logger.error("Error deleting review {} during user ban. Continuing...", reviewId, e);
                    // We continue the loop to ensure we delete as much as possible
                }
            }
        }
    }

    /**
     * Delete user node from Neo4j (ASYNC)
     */
    @Async
    public void deleteUserNode(String userId) {
        userNodeRepository.deleteByMongoId(userId);
        logger.info("Deleted user Node");
    }
}
