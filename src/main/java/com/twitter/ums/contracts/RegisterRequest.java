package com.twitter.ums.contracts;

import java.time.LocalDate;

public record RegisterRequest(
        String login,
        String password,
        String email,
        String name,
        LocalDate birthDate
) {
}
