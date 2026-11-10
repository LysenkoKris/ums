package com.twitter.ums.contracts;

import java.time.Instant;

public record SessionResponse(
        long id,
        long userId,
        Instant createdAt,
        Instant expiresAt,
        boolean active
) {
}
