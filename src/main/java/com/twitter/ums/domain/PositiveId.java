package com.twitter.ums.domain;

import com.twitter.ums.exception.ForbiddenException;
import com.twitter.ums.exception.ValidationException;

public final class PositiveId {

    private PositiveId() {
    }

    public static long require(long value) {
        if (value <= 0) {
            throw new ValidationException("некорректный идентификатор");
        }
        return value;
    }

    public static long sessionHeader(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ForbiddenException.adminRequired();
        }
        try {
            return require(Long.parseLong(raw.trim()));
        } catch (NumberFormatException exception) {
            throw new ValidationException("некорректный идентификатор");
        }
    }
}
