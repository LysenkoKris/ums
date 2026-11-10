package com.twitter.ums.repository;

import com.twitter.ums.domain.BirthDate;
import com.twitter.ums.domain.DisplayName;
import com.twitter.ums.domain.EmailAddress;
import com.twitter.ums.domain.Login;
import com.twitter.ums.domain.NewUser;
import com.twitter.ums.domain.Role;
import com.twitter.ums.domain.RoleRepository;
import com.twitter.ums.domain.Session;
import com.twitter.ums.domain.SessionRepository;
import com.twitter.ums.domain.User;
import com.twitter.ums.domain.UserRepository;
import com.twitter.ums.exception.ConflictException;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRepository implements UserRepository, RoleRepository, SessionRepository {

    private static final String USER_COLUMNS =
            "id, login, email, password_hash, display_name, birth_date, created_at";

    private final JdbcTemplate jdbc;
    private final DaoParser parser;

    public JdbcRepository(JdbcTemplate jdbc, DaoParser parser) {
        this.jdbc = jdbc;
        this.parser = parser;
    }

    @Override
    public long insert(NewUser user) {
        KeyHolder keys = new GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        """
                        INSERT INTO users (login, email, password_hash, display_name, birth_date)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                        new String[]{"id"}
                );
                statement.setString(1, user.login().value());
                statement.setString(2, user.email().value());
                statement.setString(3, user.passwordHash().value());
                statement.setString(4, text(user.displayName()));
                setDate(statement, 5, user.birthDate());
                return statement;
            }, keys);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("login or email already exists");
        }
        Number key = keys.getKey();
        if (key == null) {
            throw new IllegalStateException("user id was not generated");
        }
        return key.longValue();
    }

    @Override
    public Optional<User> findUser(long id) {
        return queryUser("SELECT " + USER_COLUMNS + " FROM users WHERE id = ?", id);
    }

    @Override
    public Optional<User> findByLogin(Login login) {
        return queryUser("SELECT " + USER_COLUMNS + " FROM users WHERE LOWER(login) = ?", login.value());
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return queryUser("SELECT " + USER_COLUMNS + " FROM users WHERE LOWER(email) = ?", email.value());
    }

    @Override
    public List<User> findUsers() {
        return jdbc.query(
                "SELECT " + USER_COLUMNS + " FROM users ORDER BY id",
                (rs, rowNum) -> parser.parseUser(rs)
        );
    }

    @Override
    public void updateProfile(User user) {
        writeProfile(
                """
                UPDATE users
                SET login = ?, email = ?, display_name = ?, birth_date = ?
                WHERE id = ?
                """,
                user.login().value(),
                user.email().value(),
                text(user.displayName()),
                date(user.birthDate()),
                user.id()
        );
    }

    @Override
    public void updateEmail(User user) {
        writeProfile("UPDATE users SET email = ? WHERE id = ?", user.email().value(), user.id());
    }

    @Override
    public void updateName(User user) {
        jdbc.update("UPDATE users SET display_name = ? WHERE id = ?", text(user.displayName()), user.id());
    }

    @Override
    public void delete(long id) {
        jdbc.update("DELETE FROM users WHERE id = ?", id);
    }

    @Override
    public List<Role> findRoles() {
        return jdbc.query(
                "SELECT id, code, name FROM roles ORDER BY id",
                (rs, rowNum) -> parser.parseRole(rs)
        );
    }

    @Override
    public Optional<Role> findByCode(String code) {
        List<Role> found = jdbc.query(
                "SELECT id, code, name FROM roles WHERE code = ?",
                (rs, rowNum) -> parser.parseRole(rs),
                code
        );
        return found.stream().findFirst();
    }

    @Override
    public List<Role> findByUserId(long userId) {
        return jdbc.query(
                """
                SELECT r.id, r.code, r.name
                FROM roles r
                JOIN user_roles ur ON ur.role_id = r.id
                WHERE ur.user_id = ?
                ORDER BY r.id
                """,
                (rs, rowNum) -> parser.parseRole(rs),
                userId
        );
    }

    @Override
    public Map<Long, List<Role>> findByUserIds(Collection<Long> userIds) {
        Map<Long, List<Role>> grouped = new LinkedHashMap<>();
        if (userIds.isEmpty()) {
            return grouped;
        }
        String placeholders = String.join(",", userIds.stream().map(id -> "?").toList());
        String sql = """
                SELECT ur.user_id, r.id, r.code, r.name
                FROM user_roles ur
                JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id IN (%s)
                ORDER BY r.id
                """.formatted(placeholders);
        List<RoleLink> links = jdbc.query(
                sql,
                (rs, rowNum) -> new RoleLink(rs.getLong("user_id"), parser.parseRole(rs)),
                userIds.toArray()
        );
        for (RoleLink link : links) {
            grouped.computeIfAbsent(link.userId(), id -> new ArrayList<>()).add(link.role());
        }
        return grouped;
    }

    @Override
    public void assign(long userId, Role role) {
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role.id());
    }

    @Override
    public void replace(long userId, List<Role> assigned) {
        jdbc.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        for (Role role : assigned) {
            assign(userId, role);
        }
    }

    @Override
    public long open(long userId, Instant expiresAt) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO sessions (user_id, expires_at) VALUES (?, ?)",
                    new String[]{"id"}
            );
            statement.setLong(1, userId);
            statement.setTimestamp(2, Timestamp.from(expiresAt));
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) {
            throw new IllegalStateException("session id was not generated");
        }
        return key.longValue();
    }

    @Override
    public Optional<Session> findSession(long id) {
        return querySession(
                """
                SELECT id, user_id, created_at, expires_at, closed_at, active
                FROM sessions
                WHERE id = ?
                """,
                id
        );
    }

    @Override
    public Optional<Session> findLast(long userId) {
        return querySession(
                """
                SELECT id, user_id, created_at, expires_at, closed_at, active
                FROM sessions
                WHERE user_id = ?
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """,
                userId
        );
    }

    @Override
    public void close(long sessionId) {
        jdbc.update(
                """
                UPDATE sessions
                SET active = FALSE, closed_at = CURRENT_TIMESTAMP
                WHERE id = ? AND active = TRUE
                """,
                sessionId
        );
    }

    private void writeProfile(String sql, Object... args) {
        try {
            jdbc.update(sql, args);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("login or email already exists");
        }
    }

    private Optional<User> queryUser(String sql, Object... args) {
        List<User> found = jdbc.query(sql, (rs, rowNum) -> parser.parseUser(rs), args);
        return found.stream().findFirst();
    }

    private Optional<Session> querySession(String sql, Object... args) {
        List<Session> found = jdbc.query(sql, (rs, rowNum) -> parser.parseSession(rs), args);
        return found.stream().findFirst();
    }

    private static void setDate(PreparedStatement statement, int index, BirthDate birthDate) throws java.sql.SQLException {
        if (birthDate == null) {
            statement.setNull(index, java.sql.Types.DATE);
            return;
        }
        statement.setDate(index, Date.valueOf(birthDate.value()));
    }

    private static String text(DisplayName name) {
        return name == null ? null : name.value();
    }

    private static Date date(BirthDate birthDate) {
        return birthDate == null ? null : Date.valueOf(birthDate.value());
    }

    private record RoleLink(long userId, Role role) {
    }
}
