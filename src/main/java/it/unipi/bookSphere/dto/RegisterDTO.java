package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.bookSphere.validation.ValidCountryCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
    @Schema(description = "Username", example = "newuser123")
    @NotBlank(message = "Username is mandatory")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscores and hyphens")
    private String username;

    @Schema(description = "Email address", example = "user@example.com")
    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email must be valid")
    private String email;

    @Schema(description = "Password", example = "SecurePass123!")
    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
             message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit")
    private String password;

    @Schema(description = "Country code", example = "IT")
    @NotBlank(message = "Country is mandatory")
    @ValidCountryCode
    private String country;
}
