package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class BookSummaryDTO {
    @Schema(description = "MongoDB ObjectId of the book")
    private String id;

    @Schema(description = "Book title", example = "The Fellowship of the Ring")
    private String title;
}
