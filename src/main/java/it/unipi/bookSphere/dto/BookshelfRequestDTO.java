package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookshelfRequestDTO {
    @Schema(description = "MongoDB ObjectId of the book", example = "65b3f...")
    @NotBlank(message = "Book ID is mandatory")
    private String bookId;

    @Schema(description = "Book status", example = "to_read", allowableValues = {"to_read", "reading", "read"})
    @NotNull(message = "Status is mandatory")
    private String status;
}
