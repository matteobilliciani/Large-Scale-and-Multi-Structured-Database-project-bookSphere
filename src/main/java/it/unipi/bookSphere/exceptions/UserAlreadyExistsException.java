package it.unipi.bookSphere.exceptions;

/**
 * Exception thrown when user already exists
 */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
