package com.twitter.ums.domain;

import com.twitter.ums.exception.ConflictException;
import com.twitter.ums.exception.ForbiddenException;
import java.time.Instant;
import java.util.List;

public final class User {

    private final long id;
    private final Login login;
    private final EmailAddress email;
    private final PasswordHash passwordHash;
    private final DisplayName displayName;
    private final BirthDate birthDate;
    private final Instant createdAt;
    private final List<Role> roles;

    private User(
            long id,
            Login login,
            EmailAddress email,
            PasswordHash passwordHash,
            DisplayName displayName,
            BirthDate birthDate,
            Instant createdAt,
            List<Role> roles
    ) {
        this.id = id;
        this.login = login;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.birthDate = birthDate;
        this.createdAt = createdAt;
        this.roles = List.copyOf(roles);
    }

    public static User restore(
            long id,
            Login login,
            EmailAddress email,
            PasswordHash passwordHash,
            DisplayName displayName,
            BirthDate birthDate,
            Instant createdAt,
            List<Role> roles
    ) {
        return new User(id, login, email, passwordHash, displayName, birthDate, createdAt, roles);
    }

    public long id() {
        return id;
    }

    public Login login() {
        return login;
    }

    public EmailAddress email() {
        return email;
    }

    public DisplayName displayName() {
        return displayName;
    }

    public BirthDate birthDate() {
        return birthDate;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public List<Role> roles() {
        return roles;
    }

    public boolean passwordMatches(PlainPassword password, PasswordHasher hasher) {
        return hasher.matches(password, passwordHash);
    }

    public User withRoles(List<Role> assigned) {
        return new User(id, login, email, passwordHash, displayName, birthDate, createdAt, assigned);
    }

    public User replaceProfile(Login newLogin, EmailAddress newEmail, DisplayName newName, BirthDate newBirthDate) {
        return new User(id, newLogin, newEmail, passwordHash, newName, newBirthDate, createdAt, roles);
    }

    public User changeEmail(EmailAddress newEmail) {
        return new User(id, login, newEmail, passwordHash, displayName, birthDate, createdAt, roles);
    }

    public User rename(DisplayName newName) {
        return new User(id, login, email, passwordHash, newName, birthDate, createdAt, roles);
    }

    public boolean hasRole(String code) {
        return roles.stream().anyMatch(role -> role.hasCode(code));
    }

    public void requireAdmin() {
        if (!hasRole(RoleCode.ADMIN)) {
            throw ForbiddenException.adminRequired();
        }
    }

    public void ensureNotAssigned(Role role) {
        if (hasRole(role.code())) {
            throw new ConflictException("role already assigned");
        }
    }
}
