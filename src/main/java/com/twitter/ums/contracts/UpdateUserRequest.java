package com.twitter.ums.contracts;

import java.time.LocalDate;
import java.util.List;

public record UpdateUserRequest(
        String login,
        String email,
        String name,
        LocalDate birthDate,
        List<String> roles
) {
}
