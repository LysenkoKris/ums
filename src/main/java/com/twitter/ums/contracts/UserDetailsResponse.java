package com.twitter.ums.contracts;

import java.time.LocalDate;
import java.util.List;

public record UserDetailsResponse(
        long id,
        String login,
        String email,
        String name,
        LocalDate birthDate,
        List<RoleResponse> roles,
        SessionResponse lastSession
) {
}
