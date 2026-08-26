package de.jmeinert.issuetracker.user;

import java.util.Locale;

public final class UserNormalizer {

    private UserNormalizer() {
    }

    public static String normalizeUsername(String username) {
        return username.strip().toLowerCase(Locale.ROOT);
    }

    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
