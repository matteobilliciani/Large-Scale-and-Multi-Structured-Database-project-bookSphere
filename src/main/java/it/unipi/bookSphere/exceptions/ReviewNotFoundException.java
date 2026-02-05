package it.unipi.bookSphere.exceptions;

/**
 * Exception thrown when a review is not found in the database
 */
public class ReviewNotFoundException extends RuntimeException {
    
    public ReviewNotFoundException(String message) {
        super(message);
    }
    
    public ReviewNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
