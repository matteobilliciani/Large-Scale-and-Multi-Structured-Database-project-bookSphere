package it.unipi.bookSphere.exceptions;

/**
 * Exception thrown when an author is not found in the database
 */
public class AuthorNotFoundException extends RuntimeException {
    
    public AuthorNotFoundException(String message) {
        super(message);
    }
    
    public AuthorNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
