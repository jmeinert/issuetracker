package de.jmeinert.issuetracker.user;

public class UserDisabledException extends RuntimeException {

    public UserDisabledException() {
        super("User is disabled");
    }
}
