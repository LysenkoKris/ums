package com.twitter.ums.domain;

public interface PasswordHasher {

    PasswordHash hash(PlainPassword password);

    boolean matches(PlainPassword password, PasswordHash hash);
}
