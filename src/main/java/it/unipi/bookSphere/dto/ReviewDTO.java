package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewDTO {
    @Schema(description = "MongoDB ObjectId of the review")
    private String id;

    @Schema(description = "Username of the user who wrote the review", example = "User_12345")
    private String username;

    @Schema(description = "Rating (0-100)", example = "85")
    @NotNull(message = "Rating is mandatory")
    @Min(value = 0, message = "Rating must be at least 0")
    @Max(value = 100, message = "Rating must be at most 100")
    private Integer rating;

    @Schema(description = "Review text", example = "An amazing book that changed my life...")
    private String text;

    @Schema(description = "Review summary", example = "Must read")
    private String summary;

    @Schema(description = "MongoDB ObjectId of the book being reviewed")
    @NotNull(message = "Book ID is mandatory")
    private String bookId;

    @Schema(description = "Book title", example = "The Fellowship of the Ring")
    private String bookTitle;

    @Schema(description = "Author name", example = "J.R.R. Tolkien")
    private String authorName;

    @Schema(description = "Number of likes received")
    private Integer likesCount;

    @Schema(description = "Review creation date")
    private LocalDateTime createdAt;

    @Schema(description = "Review source", example = "amazon", allowableValues = {"amazon", "bookcrossing"})
    private String source;

    @Schema(description = "Whether the review has been banned")
    private Boolean isBanned;
}
