package com.twitter.ums.config;

import com.twitter.ums.domain.PasswordHash;
import com.twitter.ums.domain.PasswordHasher;
import com.twitter.ums.domain.PlainPassword;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder;

    public BcryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public PasswordHash hash(PlainPassword password) {
        return new PasswordHash(encoder.encode(password.value()));
    }

    @Override
    public boolean matches(PlainPassword password, PasswordHash hash) {
        return encoder.matches(password.value(), hash.value());
    }
}
