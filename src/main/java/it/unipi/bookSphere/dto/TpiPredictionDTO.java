package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Trending Probability Index prediction result")
public class TpiPredictionDTO {
    
    @Schema(description = "MongoDB ObjectId of the book", example = "65b3f...")
    private String bookId;
    
    @Schema(description = "Book title", example = "The Shining")
    private String bookTitle;
    
    @Schema(description = "Author's historical average rating", example = "4.25")
    private Double authorBenchmark;
    
    @Schema(description = "Book's current momentum rating", example = "4.56")
    private Double bookMomentum;
    
    @Schema(description = "Prediction result", example = "🚀 RISING STAR")
    private String prediction;
    
    @Schema(description = "Current activity level (number of recent ratings)", example = "125")
    private Long currentActivity;
}