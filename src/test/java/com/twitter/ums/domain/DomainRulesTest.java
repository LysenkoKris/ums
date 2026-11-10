package com.twitter.ums.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.twitter.ums.exception.ConflictException;
import com.twitter.ums.exception.ForbiddenException;
import com.twitter.ums.exception.ValidationException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainRulesTest {

    @Test
    void loginNormalizesAndRejectsAShortValue() {
        assertEquals("denis", Login.parse(" Denis ").value());
        assertThrows(ValidationException.class, () -> Login.parse("ab"));
    }

    @Test
    void loginLookupKeepsAValueThatIsNotARegistrationLogin() {
        assertEquals("missing-user", Login.lookup("missing-user").value());
    }

    @Test
    void passwordRulesApplyOnlyWhenChoosingAPassword() {
        assertThrows(ValidationException.class, () -> PlainPassword.parse("short"));
        assertEquals("short", PlainPassword.forCheck("short").value());
    }

    @Test
    void birthDateRejectsTheFuture() {
        assertThrows(ValidationException.class, () -> BirthDate.parse(LocalDate.of(2999, 1, 1), LocalDate.of(2026, 11, 9)));
    }

    @Test
    void sessionIsUsableOnlyWhileActiveAndUnexpired() {
        Instant now = Instant.parse("2026-11-09T08:00:00Z");
        Session open = new Session(1, 4, now, now.plusSeconds(60), null, true);
        Session expired = new Session(2, 4, now.minusSeconds(120), now.minusSeconds(1), null, true);
        Session closed = new Session(3, 4, now, now.plusSeconds(60), now, false);

        assertTrue(open.isUsableAt(now));
        assertTrue(open.belongsTo(4));
        assertFalse(expired.isUsableAt(now));
        assertFalse(closed.isUsableAt(now));
    }

    @Test
    void onlyAdminCanPassTheRoleCheckAndARoleIsAssignedOnce() {
        User subscriber = userWith(new Role(3, RoleCode.SUBSCRIBER, "Подписчик"));
        User admin = userWith(new Role(1, RoleCode.ADMIN, "Администратор"));
        Role producer = new Role(2, RoleCode.PRODUCER, "Автор");

        assertThrows(ForbiddenException.class, subscriber::requireAdmin);
        admin.requireAdmin();
        assertThrows(ConflictException.class, () -> admin.withRoles(List.of(producer)).ensureNotAssigned(producer));
    }

    private static User userWith(Role role) {
        return User.restore(
                1,
                Login.parse("denis"),
                EmailAddress.parse("denis@example.com"),
                PasswordHash.restore("hash"),
                null,
                null,
                Instant.parse("2026-11-09T08:00:00Z"),
                List.of(role)
        );
    }
}
