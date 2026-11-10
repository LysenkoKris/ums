package com.twitter.ums.domain;

import java.time.Instant;

public record Session(
        long id,
        long userId,
        Instant createdAt,
        Instant expiresAt,
        Instant closedAt,
        boolean active
) {

    public boolean isUsableAt(Instant moment) {
        return active && expiresAt.isAfter(moment);
    }

    public boolean belongsTo(long ownerId) {
        return userId == ownerId;
    }
}
