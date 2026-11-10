package com.twitter.ums.domain;

import com.twitter.ums.exception.ValidationException;

public record PlainPassword(String value) {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 72;

    public PlainPassword {
        if (value == null || value.isBlank()) {
            throw new ValidationException("password is required");
        }
    }

    public static PlainPassword parse(String raw) {
        PlainPassword password = forCheck(raw);
        if (password.value().length() < MIN_LENGTH || password.value().length() > MAX_LENGTH) {
            throw new ValidationException("password must be 8-72 characters");
        }
        return password;
    }

    public static PlainPassword forCheck(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("password is required");
        }
        return new PlainPassword(raw);
    }

    @Override
    public String toString() {
        return "PlainPassword([hidden])";
    }
}
