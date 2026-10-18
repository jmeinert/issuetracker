package de.jmeinert.issuetracker.auth;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException() {
        super("User with the supplied registration data already exists.");
    }
}
