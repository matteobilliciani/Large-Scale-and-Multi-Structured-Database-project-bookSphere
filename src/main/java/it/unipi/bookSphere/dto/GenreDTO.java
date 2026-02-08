package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor 
@AllArgsConstructor
public class GenreDTO {
    @Schema(description = "Genre name", example = "Fantasy")
    @NotBlank(message = "Genre name is mandatory")
    @Size(min = 1, max = 100, message = "Genre name must be between 1 and 100 characters")
    private String name;
}
