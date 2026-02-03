package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookshelfItemDTO {
    @Schema(description = "MongoDB ObjectId of the book")
    private String bookId;

    @Schema(description = "Book title", example = "The Hobbit")
    private String title;

    @Schema(description = "Author information")
    private AuthorDTO author;

    @Schema(description = "Book genres", example = "[\"Fantasy\", \"Classic\"]")
    private List<String> genres;

    @Schema(description = "Book status", example = "read", allowableValues = {"to_read", "reading", "read"})
    private String status;

    @Schema(description = "Date when book was added to bookshelf")
    private LocalDateTime addedAt;
}
