package it.unipi.bookSphere.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class BookDTO {
    @Schema(description = "MongoDB ObjectId of the book")
    private String id;

    @Schema(description = "The title of the book", example = "Il giorno della civetta")
    @NotBlank(message = "Title is mandatory")
    private String title;

    @Schema(description = "Publication year", example = "2013")
    @NotNull(message = "Publication year is mandatory")
    private Integer publicationYear;

    @Schema(description = "Brief summary or synopsis", example = "A young boy grows up that shoots a 15...")
    private String description;

    @Schema(description = "Author details")
    @NotNull(message = "Author is mandatory")
    private AuthorDTO author;

    @Schema(description = "List of genres", example = "[\"General\", \"Historical\"]")
    private List<String> genres;

    @Schema(description = "List of ISBNs of different editions of the book", example = "[\"044023722X\", \"038550120X\"]")
    private List<String> isbns;

    @Schema(description = "Most recent reviews (last 3)")
    private List<ReviewSnapshotDTO> recentReviewsSnapshot;

    @Schema(description = "Most popular reviews (top 3 by likes)")
    private List<ReviewSnapshotDTO> popularReviewsSnapshot;

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
