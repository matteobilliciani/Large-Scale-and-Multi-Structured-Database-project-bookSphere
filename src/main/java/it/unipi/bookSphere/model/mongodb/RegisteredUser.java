package it.unipi.bookSphere.model.mongodb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class RegisteredUser {
    
    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    @Field("password_hashed")
    private String passwordHashed;

    private String country;

    @Field("joined_at")
    private Instant joinedAt;

    private String status; // "active", "banned", etc.

    // PATTERN: Bucket (Bookshelf managed as array of status objects)
    private List<BookshelfItem> bookshelf;

    // PATTERN: Subset / Report (Only reviews from current year)
    @Field("reviews_year")
    private List<ReviewYear> reviewsYear;

    // Array of ObjectIds linking to all reviews of this user
    private List<String> reviews;

    // Nested classes

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookshelfItem {
        @Field("book_id")
        private String bookId;

        private String status; // "read", "want_to_read", "reading"

        @Field("added_at")
        private Instant addedAt;

        private String title;

        private Author author;

        private List<String> genres;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Author {
        @Field("id")
        private String id;

        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewYear {
        @Field("id")
        private String id;

        private Integer rating;

        private String book; // Denormalized book title
    }
}
