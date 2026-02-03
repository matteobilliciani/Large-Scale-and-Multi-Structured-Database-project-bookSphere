package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {
    @Schema(description = "Username or email", example = "user@example.com")
    @NotBlank(message = "Username/email is mandatory")
    private String usernameOrEmail;

    @Schema(description = "Password", example = "SecurePass123!")
    @NotBlank(message = "Password is mandatory")
    private String password;
}
