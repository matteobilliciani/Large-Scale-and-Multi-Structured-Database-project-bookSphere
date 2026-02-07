package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDTO {
    @Schema(description = "MongoDB ObjectId of the book")
    private String bookId;
    
    @Schema(description = "Book title", example = "The Name of the Rose")
    private String title;
    
    @Schema(description = "Recommendation score (higher is better)", example = "15")
    private Long score;
    
    @Schema(description = "Author name", example = "Umberto Eco")
    private String authorName;
    
    @Schema(description = "Publication year", example = "1980")
    private Integer publicationYear;
}
