package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InfluencerDTO {
    @Schema(description = "Username of the influencer", example = "john_doe")
    private String username;
    
    @Schema(description = "Total engagement (likes received on reviews)", example = "523")
    private Long totalEngagement;
    
    @Schema(description = "Number of reviews written", example = "42")
    private Long numReviews;
    
    @Schema(description = "Average likes per review", example = "12.45")
    private Double avgLikesPerReview;
}
