package it.unipi.bookSphere.model.mongodb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reviews")
public class Review {
    
    @Id
    private String id;

    @Field("user_id")
    private String userId;

    private String username;

    private Integer rating; // Unified scale 0-100

    private String source; // "amazon", "bookcrossing"

    private String text;

    private String summary; // Only for Amazon reviews

    @Field("created_at")
    private Instant createdAt;

    // PATTERN: Eventual Consistency (Updated asynchronously with respect to Graph)
    @Field("likes_count")
    private Integer likesCount;

    @Field("is_banned")
    private Boolean isBanned;

    // PATTERN: Subset (Minimum book data to display review in user feed)
    @Field("book_snapshot")
    private BookSnapshot bookSnapshot;

    // Nested class

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookSnapshot {
        @Field("book_id")
        private String bookId;

        private String title;
    }
}
