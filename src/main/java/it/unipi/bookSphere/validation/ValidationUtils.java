package it.unipi.bookSphere.validation;

import it.unipi.bookSphere.exceptions.ValidationException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class for input validation
 */
public class ValidationUtils {

    private static final Pattern OBJECT_ID_PATTERN = Pattern.compile("^[0-9a-fA-F]{24}$");
    private static final List<String> VALID_BOOKSHELF_STATUSES = Arrays.asList("to_read", "reading", "read");
    
    /**
     * Validate that a string is a valid MongoDB ObjectId
     */
    public static void validateObjectId(String id, String fieldName) {
        if (id == null || id.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
        
        if (!OBJECT_ID_PATTERN.matcher(id).matches()) {
            throw new ValidationException(fieldName + " must be a valid 24-character hex string");
        }
    }
    
    /**
     * Validate that a bookshelf status is valid
     */
    public static void validateBookshelfStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ValidationException("Status is required");
        }
        
        if (!VALID_BOOKSHELF_STATUSES.contains(status.toLowerCase())) {
            throw new ValidationException("Status must be one of: to_read, reading, read");
        }
    }
    
    /**
     * Validate that a username is valid
     */
    public static void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username is required");
        }
        
        if (username.length() < 3 || username.length() > 50) {
            throw new ValidationException("Username must be between 3 and 50 characters");
        }
        
        if (!username.matches("^[a-zA-Z0-9_-]+$")) {
            throw new ValidationException("Username can only contain letters, numbers, underscores and hyphens");
        }
    }

    public static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required");
        }
        
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$")) {
            throw new ValidationException("Email must be a valid email address");
        }
    }
    
    /**
     * Validate that a genre name is valid
     */
    public static void validateGenreName(String genreName) {
        if (genreName == null || genreName.isBlank()) {
            throw new ValidationException("Genre name is required");
        }
        
        if (genreName.length() > 100) {
            throw new ValidationException("Genre name cannot exceed 100 characters");
        }
    }
    
    /**
     * Extract and validate required field from request body
     */
    public static String extractAndValidateField(Map<String, String> requestBody, String fieldName) {
        if (requestBody == null) {
            throw new ValidationException("Request body is required");
        }
        
        String value = requestBody.get(fieldName);
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
        
        return value.trim();
    }
}
