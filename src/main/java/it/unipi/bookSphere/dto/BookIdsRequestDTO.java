package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.bookSphere.validation.ValidObjectId;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookIdsRequestDTO {
    @Schema(
            description = "List of MongoDB ObjectIds of books",
            example = "[\"65b3f1a2c5d8e9f1a2b3c4d5\", \"65b3f1a2c5d8e9f1a2b3c4d6\"]"
    )
    @NotEmpty(message = "Book IDs list cannot be empty")
    private List<@ValidObjectId String> bookIds;
}
