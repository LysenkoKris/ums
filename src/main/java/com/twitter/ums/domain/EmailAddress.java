package com.twitter.ums.domain;

import com.twitter.ums.exception.ValidationException;
import java.util.Locale;
import java.util.regex.Pattern;

public record EmailAddress(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MAX_LENGTH = 255;

    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new ValidationException("email is required");
        }
    }

    public static EmailAddress parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("email is required");
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > MAX_LENGTH || !PATTERN.matcher(normalized).matches()) {
            throw new ValidationException("email is invalid");
        }
        return new EmailAddress(normalized);
    }

    public static EmailAddress restore(String stored) {
        return new EmailAddress(stored);
    }
}
