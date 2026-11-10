package com.twitter.ums.domain;

import com.twitter.ums.exception.ValidationException;

public record DisplayName(String value) {

    private static final int MAX_LENGTH = 100;

    public DisplayName {
        if (value == null || value.isBlank()) {
            throw new ValidationException("name is required");
        }
    }

    public static DisplayName parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("name is required");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new ValidationException("name must be at most 100 characters");
        }
        return new DisplayName(trimmed);
    }

    public static DisplayName optional(String raw) {
        if (raw == null) {
            return null;
        }
        return parse(raw);
    }

    public static DisplayName restore(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        return new DisplayName(stored);
    }
}
