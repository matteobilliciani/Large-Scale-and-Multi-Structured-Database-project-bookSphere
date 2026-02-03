package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class AuthorDTO {
    @Schema(description = "MongoDB ObjectId of the author")
    private String id;

    @Schema(description = "Author name", example = "J.R.R. Tolkien")
    @NotBlank(message = "Author name is mandatory")
    private String name;

    @Schema(description = "List of published books")
    private List<BookSummaryDTO> publishedBooks;

    @Schema(description = "Average rating across all author's books", example = "4.85")
    private Double averageRating;

    @Schema(description = "Total number of ratings received", example = "15000")
    private Integer ratingsCount;

    @Schema(description = "Sum of all ratings", example = "72750")
    private Integer sumRatings;
}
