package de.jmeinert.issuetracker.user;

import java.util.Locale;

public final class UserNormalizer {

    private UserNormalizer() {
    }

    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
