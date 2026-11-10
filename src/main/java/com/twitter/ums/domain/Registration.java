package com.twitter.ums.domain;

import java.util.List;

public record Registration(
        Login login,
        PlainPassword password,
        EmailAddress email,
        DisplayName name,
        BirthDate birthDate
) {
}
