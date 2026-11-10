package com.twitter.ums.contracts;

import com.twitter.ums.domain.Role;
import com.twitter.ums.domain.Session;
import com.twitter.ums.domain.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static RoleResponse toRole(Role role) {
        return new RoleResponse(role.id(), role.code(), role.name());
    }

    public static SessionResponse toSession(Session session) {
        return new SessionResponse(
                session.id(),
                session.userId(),
                session.createdAt(),
                session.expiresAt(),
                session.active()
        );
    }

    public static UserDetailsResponse toDetails(User user, Session lastSession) {
        return new UserDetailsResponse(
                user.id(),
                user.login().value(),
                user.email().value(),
                name(user),
                user.birthDate() == null ? null : user.birthDate().value(),
                user.roles().stream().map(UserMapper::toRole).toList(),
                lastSession == null ? null : toSession(lastSession)
        );
    }

    public static UserListItem toListItem(User user) {
        return new UserListItem(
                user.id(),
                user.login().value(),
                user.email().value(),
                user.roles().stream().map(UserMapper::toRole).toList()
        );
    }

    private static String name(User user) {
        return user.displayName() == null ? null : user.displayName().value();
    }
}
