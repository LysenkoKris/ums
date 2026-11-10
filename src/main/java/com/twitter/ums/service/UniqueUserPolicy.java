package com.twitter.ums.service;

import com.twitter.ums.domain.EmailAddress;
import com.twitter.ums.domain.Login;
import com.twitter.ums.domain.UserRepository;
import com.twitter.ums.exception.ConflictException;

final class UniqueUserPolicy {

    private final UserRepository users;

    UniqueUserPolicy(UserRepository users) {
        this.users = users;
    }

    void ensureLoginAvailable(Login login, Long currentUserId) {
        users.findByLogin(login)
                .filter(user -> currentUserId == null || user.id() != currentUserId)
                .ifPresent(user -> {
                    throw new ConflictException("login already exists");
                });
    }

    void ensureEmailAvailable(EmailAddress email, Long currentUserId) {
        users.findByEmail(email)
                .filter(user -> currentUserId == null || user.id() != currentUserId)
                .ifPresent(user -> {
                    throw new ConflictException("email already exists");
                });
    }
}
