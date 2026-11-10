package com.twitter.ums.contracts;

import java.time.Instant;

public record LoginResponse(long userId, String login, long sessionId, Instant expiresAt) {
}
