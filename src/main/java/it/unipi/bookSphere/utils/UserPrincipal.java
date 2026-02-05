package it.unipi.bookSphere.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Principal object representing an authenticated user in the security context.
 * Contains user information extracted from JWT token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal {
    
    /**
     * MongoDB user ID
     */
    private String userId;
    
    /**
     * Username
     */
    private String username;
    
    /**
     * User role (e.g., "USER", "ADMIN")
     */
    private String role;
    
    /**
     * User status (e.g., "active", "banned")
     */
    private String status;
    
    /**
     * Check if user has admin role
     */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
    
    /**
     * Check if user has user role
     */
    public boolean isUser() {
        return "USER".equals(role);
    }
    
    /**
     * Check if user is active
     */
    public boolean isActive() {
        return "active".equals(status);
    }
}
