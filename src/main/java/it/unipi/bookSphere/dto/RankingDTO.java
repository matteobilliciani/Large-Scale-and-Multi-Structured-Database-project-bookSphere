package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Generic ranking DTO for books, authors, and genres")
public class RankingDTO {
    
    @Schema(description = "Rank position in the list", example = "1")
    private Integer position;

    @Schema(description = "MongoDB ObjectId", example = "65b3f...")
    private String id;
    
    @Schema(description = "Name/title of the entity", example = "The Lord of the Rings")
    private String name;
    
    @Schema(description = "Average rating (Calculated dynamically)", example = "4.85")
    private Double averageRating;
    
    @Schema(description = "Total number of ratings/reviews", example = "1250")
    private Long totalRatings;
    
    @Schema(description = "Year (if applicable)", example = "2025")
    private Integer year;
    
    @Schema(description = "Additional info (e.g., author name for books)", example = "J.R.R. Tolkien")
    private String additionalInfo;
}