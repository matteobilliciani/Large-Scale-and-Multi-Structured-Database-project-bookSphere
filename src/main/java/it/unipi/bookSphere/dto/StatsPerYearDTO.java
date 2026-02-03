package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class StatsPerYearDTO {
    @Schema(description = "Year", example = "2024")
    private Integer year;

    @Schema(description = "Average rating for the year", example = "4.5")
    private Double averageRating;

    @Schema(description = "Number of ratings received in the year", example = "200")
    private Integer ratingsCount;

    @Schema(description = "Sum of all ratings for the year", example = "900")
    private Integer sumRating;
}
