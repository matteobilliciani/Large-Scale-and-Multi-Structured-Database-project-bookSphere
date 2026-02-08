package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Represents the rating trend of a book over time (Cult Classic vs Flop)")
public class BookTrendDTO {
    @Schema(description = "Rank position based on improvement", example = "1")
    private Integer position; 

    @Schema(description = "Book ID", example = "65b3f2a...")
    private String id;

    @Schema(description = "Book Title", example = "The Great Gatsby")
    private String title;

    @Schema(description = "Author Name", example = "F. Scott Fitzgerald")
    private String authorName;

    @Schema(description = "First year recorded", example = "2020")
    private Integer startYear;

    @Schema(description = "Last year recorded", example = "2025")
    private Integer endYear;

    @Schema(description = "Rating in the first year", example = "3.5")
    private Double startRating;

    @Schema(description = "Rating in the last year", example = "4.8")
    private Double endRating;

    @Schema(description = "Difference between end and start rating", example = "1.3")
    private Double ratingDelta;
    
    @Schema(description = "Textual representation of the trend", example = "Started at 3.5 -> Ended at 4.8 (+1.3)")
    private String trendDescription;
}
