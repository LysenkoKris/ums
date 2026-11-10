package com.twitter.ums.domain;

import com.twitter.ums.exception.ValidationException;
import java.util.Locale;
import java.util.regex.Pattern;

public final class RoleCode {

    public static final String ADMIN = "ADMIN";
    public static final String PRODUCER = "PRODUCER";
    /** Читает ленту и может подписываться. */
    public static final String SUBSCRIBER = "SUBSCRIBER";
    /** Минимальные права: только чтение. Назначается при регистрации. */
    public static final String GUEST = "GUEST";

    private static final Pattern PATTERN = Pattern.compile("^[A-Z_]{2,32}$");

    private RoleCode() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("role is required");
        }
        String code = raw.trim().toUpperCase(Locale.ROOT);
        if (!PATTERN.matcher(code).matches()) {
            throw new ValidationException("unknown role");
        }
        return code;
    }
}
