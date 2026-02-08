package it.unipi.bookSphere.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.bookSphere.validation.ValidYear;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class BookDTO {
    @Schema(description = "MongoDB ObjectId of the book")
    private String id;

    @Schema(description = "The title of the book", example = "Il giorno della civetta")
    @NotBlank(message = "Title is mandatory")
    @Size(min = 1, max = 500, message = "Title must be between 1 and 500 characters")
    private String title;

    @Schema(description = "Publication year", example = "2013")
    @NotNull(message = "Publication year is mandatory")
    @ValidYear(minYear = 1000, allowFuture = false)
    private Integer publicationYear;

    @Schema(description = "Brief summary or synopsis", example = "A young boy grows up that shoots a 15...")
    @Size(max = 10000, message = "Description cannot exceed 10000 characters")
    private String description;

    @Schema(description = "Author details")
    @NotNull(message = "Author is mandatory")
    @Valid
    private AuthorDTO author;

    @Schema(description = "List of genres", example = "[\"General\", \"Historical\"]")
    private List<String> genres;

    @Schema(description = "List of ISBNs of different editions of the book", example = "[\"044023722X\", \"038550120X\"]")
    private List<String> isbns;

    @Schema(description = "Most recent reviews (last 3)")
    private List<ReviewSnapshotDTO> recentReviewsSnapshot;

    @Schema(description = "Most popular reviews (top 3 by likes)")
    private List<ReviewSnapshotDTO> popularReviewsSnapshot;

    @Schema(description = "List of MongoDB ObjectIds of all reviews for this book")
    private List<String> reviews;

    @Schema(description = "Statistics aggregated by year")
    private List<StatsPerYearDTO> statsPerYear;

    @Schema(description = "Overall average rating")
    private Double averageRating;

    @Schema(description = "Total number of ratings")
    private Integer totalRatingsCount;

    @Schema(description = "Sum of all ratings")
    private Integer totalSumRating;

    @Schema(description = "Current trending score")
    private TrendScoreDTO trendScore;

    @Schema(description = "Source of the book data", example = "amazon_master")
    private String source;
}
