package it.unipi.bookSphere.service.async;

import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.model.mongodb.Review;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Async tasks for like operations.
 * These methods handle eventual consistency updates for popular reviews snapshots.
 */
@Service
@RequiredArgsConstructor
public class AsyncLikeTasks {

    private static final Logger logger = LoggerFactory.getLogger(AsyncLikeTasks.class);

    private final MongoTemplate mongoTemplate;

    /**
     * Update popular_reviews_snapshot in book (eventual consistency - ASYNC)
     * Keeps the top 3 reviews with most likes
     */
    @Async
    public void updateBookPopularReviews(String bookId) {
        // Find all reviews for this book sorted by likes_count descending
        Query reviewQuery = new Query(Criteria.where("book_snapshot.book_id").is(bookId))
                .with(Sort.by(Sort.Direction.DESC, "likes_count"))
                .limit(3);
        
        List<Review> topReviews = mongoTemplate.find(reviewQuery, Review.class);
        
        // Build the popular_reviews_snapshot array
        List<BookDocument.ReviewSnapshot> snapshots = new ArrayList<>();
        for (Review review : topReviews) {
            BookDocument.ReviewSnapshot snapshot = new BookDocument.ReviewSnapshot();
            snapshot.setId(review.getId());
            snapshot.setUserId(review.getUserId());
            snapshot.setUsername(review.getUsername());
            snapshot.setRating(review.getRating());
            snapshot.setSummary(review.getSummary());
            snapshot.setNumOfLike(review.getLikesCount());
            snapshot.setDate(review.getCreatedAt());
            snapshots.add(snapshot);
        }
        
        // Update the book's popular_reviews_snapshot
        Query bookQuery = new Query(Criteria.where("_id").is(bookId));
        Update update = new Update().set("popular_reviews_snapshot", snapshots);
        mongoTemplate.updateFirst(bookQuery, update, BookDocument.class);
        
        logger.info("Updated popular_reviews_snapshot for book: {}", bookId);
    }
}
