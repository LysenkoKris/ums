package com.twitter.ums.domain;

import com.twitter.ums.exception.ValidationException;
import java.time.LocalDate;

public record BirthDate(LocalDate value) {

    private static final LocalDate EARLIEST = LocalDate.of(1900, 1, 1);

    public BirthDate {
        if (value == null) {
            throw new ValidationException("birthDate is invalid");
        }
    }

    public static BirthDate parse(LocalDate value, LocalDate today) {
        if (value.isAfter(today)) {
            throw new ValidationException("birthDate is in the future");
        }
        if (value.isBefore(EARLIEST)) {
            throw new ValidationException("birthDate is invalid");
        }
        return new BirthDate(value);
    }

    public static BirthDate optional(LocalDate value, LocalDate today) {
        if (value == null) {
            return null;
        }
        return parse(value, today);
    }

    public static BirthDate restore(LocalDate stored) {
        if (stored == null) {
            return null;
        }
        return new BirthDate(stored);
    }
}
