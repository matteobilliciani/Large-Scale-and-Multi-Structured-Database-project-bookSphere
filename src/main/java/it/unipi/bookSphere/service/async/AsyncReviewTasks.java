package it.unipi.bookSphere.service.async;

import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.model.mongodb.Review;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.ZoneOffset;

/**
 * Async tasks for review operations.
 * These methods handle eventual consistency updates between MongoDB and other databases.
 */
@Service
@RequiredArgsConstructor
public class AsyncReviewTasks {

    private static final Logger logger = LoggerFactory.getLogger(AsyncReviewTasks.class);
    private static final int MAX_RECENT_REVIEWS = 3;

    private final MongoTemplate mongoTemplate;

    /**
     * Update book statistics after a review is added (ASYNC)
     * 
     * Updates:
     * - Sum and count of reviews per year
     * - Recent reviews snapshot (last 3)
     * - Adds review ID to reviews array
     */
    @Async
    public void updateBookStatistics(BookDocument book, Review review) {
        int reviewYear = review.getCreatedAt().atZone(ZoneOffset.UTC).getYear();
        
        Query query = new Query(Criteria.where("_id").is(book.getId()));
        Update update = new Update();
        
        // 1. Update or create stats_per_year for review year
        boolean yearExists = book.getStatsPerYear() != null && 
            book.getStatsPerYear().stream().anyMatch(s -> s.getYear().equals(reviewYear));
        
        if (yearExists) {
            // Increment existing year
            update.inc("stats_per_year.$[elem].ratings_count", 1);
            update.inc("stats_per_year.$[elem].sum_rating", review.getRating());
            update.filterArray(Criteria.where("elem.year").is(reviewYear));
        } else {
            // Add new year stat
            BookDocument.YearStat newStat = new BookDocument.YearStat();
            newStat.setYear(reviewYear);
            newStat.setRatingsCount(1);
            newStat.setSumRating(review.getRating());
            
            update.push("stats_per_year", newStat);
        }
        
        // 2. Add to recent_reviews_snapshot (keep last 3)
        BookDocument.ReviewSnapshot snapshot = new BookDocument.ReviewSnapshot();
        snapshot.setId(review.getId());
        snapshot.setUserId(review.getUserId());
        snapshot.setUsername(review.getUsername());
        snapshot.setRating(review.getRating());
        snapshot.setSummary(review.getSummary());
        snapshot.setNumOfLike(0);
        snapshot.setDate(review.getCreatedAt());
        
        update.push("recent_reviews_snapshot")
            .slice(-MAX_RECENT_REVIEWS)
            .each(snapshot);
        
        // 3. Add review ID to reviews array
        update.addToSet("reviews", review.getId());
        
        mongoTemplate.updateFirst(query, update, BookDocument.class);
        logger.info("Updated book statistics for book: {}", book.getId());
    }

    /**
     * Update book statistics after rating change (ASYNC)
     */
    @Async
    public void updateBookStatisticsAfterRatingChange(BookDocument book, Review review, Integer oldRating) {
        int reviewYear = review.getCreatedAt().atZone(ZoneOffset.UTC).getYear();
        
        Query query = new Query(Criteria.where("_id").is(book.getId()));
        Update update = new Update();
        
        // Update stats_per_year
        BookDocument.YearStat stat = book.getStatsPerYear().stream()
            .filter(s -> s.getYear().equals(reviewYear))
            .findFirst()
            .orElse(null);
        
        if (stat != null) {
            int newSum = stat.getSumRating() - oldRating + review.getRating();
            
            update.set("stats_per_year.$[elem].sum_rating", newSum);
            update.filterArray(Criteria.where("elem.year").is(reviewYear));
            
            mongoTemplate.updateFirst(query, update, BookDocument.class);
            logger.info("Updated book statistics after rating change for book: {}", book.getId());
        }
    }

    /**
     * Remove review from book statistics after deletion (ASYNC)
     */
    @Async
    public void removeReviewFromBookStatistics(BookDocument book, Review review) {
        int reviewYear = review.getCreatedAt().atZone(ZoneOffset.UTC).getYear();
        Integer rating = review.getRating();
        String reviewId = review.getId();

        Query query = new Query(Criteria.where("_id").is(book.getId()));
        Update update = new Update();
        
        // 1. Update stats_per_year
        BookDocument.YearStat stat = book.getStatsPerYear() != null 
            ? book.getStatsPerYear().stream()
                .filter(s -> s.getYear().equals(reviewYear))
                .findFirst()
                .orElse(null)
            : null;
        
        if (stat != null && stat.getRatingsCount() > 1) {
            int newSum = stat.getSumRating() - rating;
            
            update.inc("stats_per_year.$[elem].ratings_count", -1);
            update.set("stats_per_year.$[elem].sum_rating", newSum);
            update.filterArray(Criteria.where("elem.year").is(reviewYear));
        } else if (stat != null && stat.getRatingsCount() == 1) {
            // Remove the year stat entirely
            update.pull("stats_per_year", Query.query(Criteria.where("year").is(reviewYear)));
        }
        
        // 2. Remove from snapshots  
        update.pull("recent_reviews_snapshot", Query.query(Criteria.where("_id").is(reviewId)));
        update.pull("popular_reviews_snapshot", Query.query(Criteria.where("_id").is(reviewId)));
        
        // 3. Remove from reviews array
        update.pull("reviews", reviewId);
        
        mongoTemplate.updateFirst(query, update, BookDocument.class);
        logger.info("Removed review from book statistics: {}", reviewId);
    }

    /**
     * Update author statistics when a review is added (eventual consistency - ASYNC)
     */
    @Async
    public void updateAuthorStatistics(String authorId, Integer rating) {
        Query query = new Query(Criteria.where("_id").is(authorId));
        Update update = new Update();
        
        update.inc("ratings_count", 1);
        update.inc("sum_ratings", rating);
        
        mongoTemplate.updateFirst(query, update, AuthorDocument.class);
        
        logger.info("Updated author statistics for author: {}", authorId);
    }

    /**
     * Update author statistics after rating change (eventual consistency - ASYNC)
     */
    @Async
    public void updateAuthorStatisticsAfterRatingChange(String authorId, Integer oldRating, Integer newRating) {
        Query query = new Query(Criteria.where("_id").is(authorId));
        Update update = new Update();
        
        int ratingDiff = newRating - oldRating;
        update.inc("sum_ratings", ratingDiff);
        
        mongoTemplate.updateFirst(query, update, AuthorDocument.class);
        
        logger.info("Updated author statistics after rating change for author: {}", authorId);
    }

    /**
     * Remove review from author statistics (eventual consistency - ASYNC)
     */
    @Async
    public void removeReviewFromAuthorStatistics(String authorId, Integer rating) {
        Query query = new Query(Criteria.where("_id").is(authorId));
        Update update = new Update();
        
        update.inc("ratings_count", -1);
        update.inc("sum_ratings", -rating);
        
        mongoTemplate.updateFirst(query, update, AuthorDocument.class);
        
        logger.info("Removed review from author statistics for author: {}", authorId);
    }

    /**
     * Update month score for trending analysis (eventual consistency - ASYNC)
     * This is called when a new review is added or removed
     * 
     * @param review The review object
     * @param ratingDelta The rating change (positive for add, negative for remove)
     * @param countDelta The count change (1 for add, -1 for remove)
     */
    @Async
    public void updateMonthScore(Review review, Integer ratingDelta, Integer countDelta) {
        String bookId = review.getBookSnapshot().getBookId();
        String currentMonth = YearMonth.now().toString();
        String reviewMonth = YearMonth.from(review.getCreatedAt().atZone(ZoneOffset.UTC)).toString();

        if (!reviewMonth.equals(currentMonth)) {
            logger.info("Skipping month score update for book {}: review made in {}, current month is {}", 
                    bookId, reviewMonth, currentMonth);
            return;
        }

        Query query = new Query(Criteria.where("_id").is(bookId));
        
        // Fetch to determine if a monthly reset is needed
        BookDocument book = mongoTemplate.findOne(query, BookDocument.class);

        if (book != null) {
            Update update = new Update();
            BookDocument.MonthScore currentScore = book.getMonthScore();

            // Check for month transition or missing data
            boolean isNewMonth = currentScore == null || 
                                currentScore.getCurrentMonth() == null || 
                                !currentScore.getCurrentMonth().equals(currentMonth);

            if (isNewMonth) {
                // New Month: Full reset
                BookDocument.MonthScore newScore = new BookDocument.MonthScore();
                newScore.setCurrentMonth(currentMonth);
                
                // Initialize values (prevent negatives on reset)
                newScore.setRatingCount(Math.max(countDelta, 0));
                newScore.setSumRating(Math.max(ratingDelta, 0));
                
                update.set("month_score", newScore);
            } else {
                // Same Month: Atomic increment
                update.inc("month_score.rating_count", countDelta);
                update.inc("month_score.sum_rating", ratingDelta);
            }
            
            mongoTemplate.updateFirst(query, update, BookDocument.class);
            logger.info("Updated month score stats for book: {}", bookId);
        }
    }

    /**
     * Update month score after rating change (eventual consistency - ASYNC)
     */
    @Async
    public void updateMonthScoreAfterRatingChange(Review review, Integer oldRating) {
        int ratingDelta = review.getRating() - oldRating;
        updateMonthScore(review, ratingDelta, 0);
    }
}
