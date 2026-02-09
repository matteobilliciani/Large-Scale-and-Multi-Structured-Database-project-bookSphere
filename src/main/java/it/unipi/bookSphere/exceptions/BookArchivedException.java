package it.unipi.bookSphere.exceptions;

/**
 * Exception thrown when a book is archived in the database
 */
public class BookArchivedException extends RuntimeException {
    
    public BookArchivedException(String message) {
        super(message);
    }
    
    public BookArchivedException(String message, Throwable cause) {
        super(message, cause);
    }
}