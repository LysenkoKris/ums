package com.twitter.ums.domain;

import java.util.List;

public record ProfileReplacement(
        Login login,
        EmailAddress email,
        DisplayName name,
        BirthDate birthDate,
        List<String> roleCodes
) {
}
