package it.unipi.bookSphere.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for authentication endpoints (login/register)
 * Contains JWT token and user information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    
    /**
     * JWT access token
     */
    private String token;
    
    /**
     * Token type (always "Bearer")
     */
    private String type = "Bearer";
    
    /**
     * User ID (MongoDB ObjectId)
     */
    private String userId;
    
    /**
     * Username
     */
    private String username;
    
    /**
     * User role
     */
    private String role;
    
    /**
     * Constructor without type (defaults to "Bearer")
     */
    public AuthResponseDTO(String token, String userId, String username, String role) {
        this.token = token;
        this.type = "Bearer";
        this.userId = userId;
        this.username = username;
        this.role = role;
    }
}
