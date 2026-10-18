package de.jmeinert.issuetracker.user;

public class SelfDeactivationNotAllowedException extends RuntimeException {

    public SelfDeactivationNotAllowedException() {
        super("Disabling your own account is not allowed");
    }
}
