package it.unipi.bookSphere.exceptions;

/**
 * Exception thrown when a book is not found in the database
 */
public class BookNotFoundException extends RuntimeException {
    
    public BookNotFoundException(String message) {
        super(message);
    }
    
    public BookNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
