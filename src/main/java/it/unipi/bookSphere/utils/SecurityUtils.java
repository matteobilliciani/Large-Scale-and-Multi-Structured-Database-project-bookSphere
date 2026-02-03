package it.unipi.bookSphere.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility class for retrieving the current authenticated user from Spring Security context
 */
@Component
public class SecurityUtils {

    /**
     * Get the current authenticated user principal
     * 
     * @return UserPrincipal of the authenticated user, or null if not authenticated
     */
    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) authentication.getPrincipal();
        }
        
        return null;
    }

    /**
     * Get the current user's ID
     * 
     * @return User ID (MongoDB ObjectId) or null if not authenticated
     */
    public static String getCurrentUserId() {
        UserPrincipal user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }

    /**
     * Get the current user's username
     * 
     * @return Username or null if not authenticated
     */
    public static String getCurrentUsername() {
        UserPrincipal user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    /**
     * Get the current user's role
     * 
     * @return Role or null if not authenticated
     */
    public static String getCurrentUserRole() {
        UserPrincipal user = getCurrentUser();
        return user != null ? user.getRole() : null;
    }

    /**
     * Check if current user is authenticated
     * 
     * @return true if user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        return getCurrentUser() != null;
    }

    /**
     * Check if current user has admin role
     * 
     * @return true if user is admin, false otherwise
     */
    public static boolean isAdmin() {
        UserPrincipal user = getCurrentUser();
        return user != null && user.isAdmin();
    }
}
