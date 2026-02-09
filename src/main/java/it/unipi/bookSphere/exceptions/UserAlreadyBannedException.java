package it.unipi.bookSphere.exceptions;

public class UserAlreadyBannedException extends RuntimeException {
    public UserAlreadyBannedException(String message) {
        super(message);
    }
}
