package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ReviewYearDTO {
    @Schema(description = "MongoDB ObjectId of the review")
    private String id;

    @Schema(description = "Rating (0-100)", example = "85")
    private Integer rating;

    @Schema(description = "Book title", example = "Dune")
    private String book;
}
