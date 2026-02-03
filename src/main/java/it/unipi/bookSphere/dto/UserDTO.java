package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDTO {
    @Schema(description = "MongoDB ObjectId of the user")
    private String id;

    @Schema(description = "Username", example = "User_12345")
    @NotBlank(message = "Username is mandatory")
    private String username;

    @Schema(description = "User email", example = "user@example.com")
    private String email;

    @Schema(description = "User country code", example = "IT")
    private String country;

    @Schema(description = "Account status", example = "active")
    private String status;

    @Schema(description = "Date when user joined")
    private LocalDateTime joinedAt;

    @Schema(description = "User's bookshelf containing books with their status")
    private List<BookshelfItemDTO> bookshelf;

    @Schema(description = "User's reviews from the current year")
    private List<ReviewYearDTO> reviewsYear;
}
