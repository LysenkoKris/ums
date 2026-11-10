package com.twitter.ums.domain;

public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("password hash is required");
        }
    }

    public static PasswordHash restore(String stored) {
        return new PasswordHash(stored);
    }

    @Override
    public String toString() {
        return "PasswordHash([hidden])";
    }
}
