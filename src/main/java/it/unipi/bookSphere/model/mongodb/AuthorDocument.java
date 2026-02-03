package it.unipi.bookSphere.model.mongodb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "authors")
public class AuthorDocument {
    
    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    // PATTERN: Extended Reference (List of published books)
    @Field("published_books")
    private List<PublishedBook> publishedBooks;

    // PATTERN: Computed (Pre-calculated aggregations from all reviews of their books)
    @Field("average_rating")
    private Double averageRating;

    @Field("ratings_count")
    private Integer ratingsCount; // Total votes received (Counter)

    @Field("sum_ratings")
    private Integer sumRatings; // Sum of votes (Accumulator)

    // Nested class

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublishedBook {
        @Field("_id")
        private String id;

        private String title;
    }
}
