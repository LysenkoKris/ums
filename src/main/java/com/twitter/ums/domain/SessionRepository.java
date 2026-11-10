package com.twitter.ums.domain;

import java.time.Instant;
import java.util.Optional;

public interface SessionRepository {

    long open(long userId, Instant expiresAt);

    Optional<Session> findSession(long id);

    Optional<Session> findLast(long userId);

    void close(long sessionId);
}
