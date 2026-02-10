package it.unipi.bookSphere.service.async;

import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Async tasks for profile operations.
 * These methods handle eventual consistency updates across MongoDB documents.
 */
@Service
@RequiredArgsConstructor
public class AsyncProfileTasks {

    private static final Logger logger = LoggerFactory.getLogger(AsyncProfileTasks.class);

    private final RegisteredUserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Update username in all reviews (ASYNC - eventual consistency)
     * Uses the user's linked reviews list to avoid findByUserId and leverage indexing
     */
    @Async
    public void updateUsernameInReviews(String userId, String newUsername) {
        // Get user's review IDs from the user document
        RegisteredUser user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getReviews() == null || user.getReviews().isEmpty()) {
            logger.info("No reviews to update for user {}", userId);
            return;
        }
        
        // Update each review by ID (leveraging index on _id)
        Query reviewQuery = new Query(Criteria.where("_id").in(user.getReviews()));
        Update reviewUpdate = new Update().set("username", newUsername);
        long updatedCount = mongoTemplate.updateMulti(reviewQuery, reviewUpdate, "reviews").getModifiedCount();
        logger.info("Updated username in {} reviews for user {} using linked reviews list", updatedCount, userId);
    }

    /**
     * Remove username field from all reviews (ASYNC - eventual consistency)
     * Uses the user's linked reviews list to avoid findByUserId and leverage indexing
     */
    @Async
    public void removeUsernameFromReviews(String userId) {
        // Get user's review IDs from the user document
        RegisteredUser user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getReviews() == null || user.getReviews().isEmpty()) {
            logger.info("No reviews to update for user {}", userId);
            return;
        }
        
        // Remove username from each review by ID (leveraging index on _id)
        Query reviewQuery = new Query(Criteria.where("_id").in(user.getReviews()));
        Update reviewUpdate = new Update().unset("username");
        long updatedCount = mongoTemplate.updateMulti(reviewQuery, reviewUpdate, "reviews").getModifiedCount();
        logger.info("Removed username field from {} reviews for user {} using linked reviews list", updatedCount, userId);
    }

    /**
     * Update username in book snapshots (recent_reviews_snapshot and popular_reviews_snapshot)
     * This is an eventual consistency update (ASYNC)
     */
    @Async
    public void updateUsernameInBookSnapshots(String userId, String oldUsername, String newUsername) {
        // Find all books that have reviews from this user in their snapshots
        Query bookQuery = new Query(
            new Criteria().orOperator(
                Criteria.where("recent_reviews_snapshot.user_id").is(userId),
                Criteria.where("popular_reviews_snapshot.user_id").is(userId)
            )
        );
        
        List<BookDocument> books = mongoTemplate.find(bookQuery, BookDocument.class);
        
        for (BookDocument book : books) {
            boolean needsUpdate = false;
            
            // Update recent_reviews_snapshot
            if (book.getRecentReviewsSnapshot() != null) {
                for (BookDocument.ReviewSnapshot snapshot : book.getRecentReviewsSnapshot()) {
                    if (userId.equals(snapshot.getUserId())) {
                        snapshot.setUsername(newUsername);
                        needsUpdate = true;
                    }
                }
            }
            
            // Update popular_reviews_snapshot
            if (book.getPopularReviewsSnapshot() != null) {
                for (BookDocument.ReviewSnapshot snapshot : book.getPopularReviewsSnapshot()) {
                    if (userId.equals(snapshot.getUserId())) {
                        snapshot.setUsername(newUsername);
                        needsUpdate = true;
                    }
                }
            }
            
            // Save the updated book
            if (needsUpdate) {
                Query updateQuery = new Query(Criteria.where("_id").is(book.getId()));
                Update update = new Update()
                    .set("recent_reviews_snapshot", book.getRecentReviewsSnapshot())
                    .set("popular_reviews_snapshot", book.getPopularReviewsSnapshot());
                mongoTemplate.updateFirst(updateQuery, update, BookDocument.class);
            }
        }
        
        logger.info("Updated username in book snapshots from {} to {} for {} books", 
                    oldUsername, newUsername, books.size());
    }

    /**
     * Remove username field from book snapshots (recent_reviews_snapshot and popular_reviews_snapshot)
     * This is an eventual consistency update (ASYNC)
     */
    @Async
    public void removeUsernameFromBookSnapshots(String userId, String oldUsername) {
        // Find all books that have reviews from this user in their snapshots
        Query bookQuery = new Query(
            new Criteria().orOperator(
                Criteria.where("recent_reviews_snapshot.user_id").is(userId),
                Criteria.where("popular_reviews_snapshot.user_id").is(userId)
            )
        );
        
        List<BookDocument> books = mongoTemplate.find(bookQuery, BookDocument.class);
        
        for (BookDocument book : books) {
            boolean needsUpdate = false;
            
            // Remove username from recent_reviews_snapshot
            if (book.getRecentReviewsSnapshot() != null) {
                for (BookDocument.ReviewSnapshot snapshot : book.getRecentReviewsSnapshot()) {
                    if (userId.equals(snapshot.getUserId())) {
                        snapshot.setUsername(null);
                        needsUpdate = true;
                    }
                }
            }
            
            // Remove username from popular_reviews_snapshot
            if (book.getPopularReviewsSnapshot() != null) {
                for (BookDocument.ReviewSnapshot snapshot : book.getPopularReviewsSnapshot()) {
                    if (userId.equals(snapshot.getUserId())) {
                        snapshot.setUsername(null);
                        needsUpdate = true;
                    }
                }
            }
            
            // Save the updated book
            if (needsUpdate) {
                Query updateQuery = new Query(Criteria.where("_id").is(book.getId()));
                Update update = new Update()
                    .set("recent_reviews_snapshot", book.getRecentReviewsSnapshot())
                    .set("popular_reviews_snapshot", book.getPopularReviewsSnapshot());
                mongoTemplate.updateFirst(updateQuery, update, BookDocument.class);
            }
        }
        
        logger.info("Removed username field from book snapshots for user {} in {} books", 
                    userId, books.size());
    }
}
