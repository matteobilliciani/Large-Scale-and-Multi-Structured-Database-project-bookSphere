package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenreDTO {
    @Schema(description = "Genre name", example = "Fantasy")
    @NotBlank(message = "Genre name is mandatory")
    private String name;
}
