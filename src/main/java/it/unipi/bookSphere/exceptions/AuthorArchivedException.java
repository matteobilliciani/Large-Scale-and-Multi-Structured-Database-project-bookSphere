package it.unipi.bookSphere.exceptions;

public class AuthorArchivedException extends RuntimeException{
    public AuthorArchivedException(String message) {
        super(message);
    }
    
    public AuthorArchivedException(String message, Throwable cause) {
        super(message, cause);
    }
}
