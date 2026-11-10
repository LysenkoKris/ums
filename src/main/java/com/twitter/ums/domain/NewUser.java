package com.twitter.ums.domain;

public record NewUser(
        Login login,
        EmailAddress email,
        PasswordHash passwordHash,
        DisplayName displayName,
        BirthDate birthDate
) {
}
