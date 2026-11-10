package com.twitter.ums.repository;

import com.twitter.ums.domain.BirthDate;
import com.twitter.ums.domain.DisplayName;
import com.twitter.ums.domain.EmailAddress;
import com.twitter.ums.domain.Login;
import com.twitter.ums.domain.PasswordHash;
import com.twitter.ums.domain.Role;
import com.twitter.ums.domain.Session;
import com.twitter.ums.domain.User;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DaoParser {

    public User parseUser(ResultSet rs) throws SQLException {
        return User.restore(
                rs.getLong("id"),
                Login.restore(rs.getString("login")),
                EmailAddress.restore(rs.getString("email")),
                PasswordHash.restore(rs.getString("password_hash")),
                DisplayName.restore(rs.getString("display_name")),
                BirthDate.restore(readDate(rs, "birth_date")),
                readInstant(rs, "created_at"),
                List.of()
        );
    }

    public Role parseRole(ResultSet rs) throws SQLException {
        return new Role(rs.getLong("id"), rs.getString("code"), rs.getString("name"));
    }

    public Session parseSession(ResultSet rs) throws SQLException {
        return new Session(
                rs.getLong("id"),
                rs.getLong("user_id"),
                readInstant(rs, "created_at"),
                readInstant(rs, "expires_at"),
                readInstant(rs, "closed_at"),
                rs.getBoolean("active")
        );
    }

    private static LocalDate readDate(ResultSet rs, String column) throws SQLException {
        Date date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private static Instant readInstant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
