package it.unipi.bookSphere.model.mongodb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "books")

public class BookDocument {
    @Id
    private String id;

    // Title of the book
    private String title;

    @Field("publication_year")
    private  Integer publicationYear;

    private String description;
    private Author author;

    // List of genres associated with the book
    private List<String> genres;


    @Field("external_ids")
    private ExternalIds externalIds;

    @Field("recent_reviews_snapshot")
    private List<ReviewSnapshot> recentReviewsSnapshot;

    @Field("popular_reviews_snapshot")
    private List<ReviewSnapshot> popularReviewsSnapshot;

    @Field("stats_per_year")
    private List<YearStat> statsPerYear;

    @Field("trend_score")
    private TrendScore trendScore;

    private String source;

    // Privates Classes related to Book

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Author {
        // This maps the "id" inside the author object
        @Field("id")
        private String id;

        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalIds {
        private List<String> isbns;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewSnapshot {
        // This maps the nested "_id" inside the review array
        @Field("_id")
        private String id;

        private String username;
        private Integer rating;
        private String snippet;

        // Using Integer object (not int primitive) allows it to be null safely.
        @Field("num_of_like")
        private Integer numOfLike;

        // Maps the {"$date": ...} structure to a Java Instant
        private Instant date;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YearStat {
        private Integer year;

        @Field("average_rating")
        private Double averageRating;

        @Field("ratings_count")
        private Integer ratingsCount;

        @Field("sum_rating")
        private Integer sumRating;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendScore {
        private Double rating;

        @Field("updated_at")
        private Instant updatedAt;
    }

}
