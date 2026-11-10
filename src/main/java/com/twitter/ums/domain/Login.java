package com.twitter.ums.domain;

import com.twitter.ums.exception.ValidationException;
import java.util.Locale;
import java.util.regex.Pattern;

public record Login(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9_]{3,32}$");

    public Login {
        if (value == null || value.isBlank()) {
            throw new ValidationException("login is required");
        }
    }

    public static Login parse(String raw) {
        Login login = lookup(raw);
        if (!PATTERN.matcher(login.value()).matches()) {
            throw new ValidationException("login must be 3-32 characters: letters, digits, underscore");
        }
        return login;
    }

    public static Login lookup(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("login is required");
        }
        return new Login(raw.trim().toLowerCase(Locale.ROOT));
    }

    public static Login restore(String stored) {
        return new Login(stored);
    }
}
