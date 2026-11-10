package com.twitter.ums.service;

import com.twitter.ums.config.AppProperties;
import com.twitter.ums.contracts.LoginResponse;
import com.twitter.ums.contracts.MessageResponse;
import com.twitter.ums.contracts.RoleAssignmentResponse;
import com.twitter.ums.contracts.RoleResponse;
import com.twitter.ums.contracts.SessionResponse;
import com.twitter.ums.contracts.UserCreatedResponse;
import com.twitter.ums.contracts.UserDetailsResponse;
import com.twitter.ums.contracts.UserListItem;
import com.twitter.ums.contracts.UserMapper;
import com.twitter.ums.domain.Credentials;
import com.twitter.ums.domain.NewUser;
import com.twitter.ums.domain.PasswordHash;
import com.twitter.ums.domain.PasswordHasher;
import com.twitter.ums.domain.ProfilePatch;
import com.twitter.ums.domain.ProfileReplacement;
import com.twitter.ums.domain.Registration;
import com.twitter.ums.domain.Role;
import com.twitter.ums.domain.RoleCode;
import com.twitter.ums.domain.RoleRepository;
import com.twitter.ums.domain.Session;
import com.twitter.ums.domain.SessionRepository;
import com.twitter.ums.domain.User;
import com.twitter.ums.domain.UserRepository;
import com.twitter.ums.exception.ForbiddenException;
import com.twitter.ums.exception.InvalidCredentialsException;
import com.twitter.ums.exception.SessionNotFoundException;
import com.twitter.ums.exception.UserNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final SessionRepository sessions;
    private final PasswordHasher passwords;
    private final UniqueUserPolicy uniqueness;
    private final RoleCatalog catalog;
    private final AppProperties properties;
    private final Clock clock;

    public UserService(
            UserRepository users,
            RoleRepository roles,
            SessionRepository sessions,
            PasswordHasher passwords,
            AppProperties properties,
            Clock clock
    ) {
        this.users = users;
        this.roles = roles;
        this.sessions = sessions;
        this.passwords = passwords;
        this.uniqueness = new UniqueUserPolicy(users);
        this.catalog = new RoleCatalog(roles);
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public UserCreatedResponse register(Registration registration) {
        uniqueness.ensureLoginAvailable(registration.login(), null);
        uniqueness.ensureEmailAvailable(registration.email(), null);
        PasswordHash hash = passwords.hash(registration.password());
        long id = users.insert(new NewUser(
                registration.login(),
                registration.email(),
                hash,
                registration.name(),
                registration.birthDate()
        ));
        roles.assign(id, catalog.system(RoleCode.GUEST));
        return new UserCreatedResponse(id, registration.login().value(), registration.email().value());
    }

    @Transactional
    public LoginResponse login(Credentials credentials) {
        User user = users.findByLogin(credentials.login()).orElseThrow(InvalidCredentialsException::new);
        if (!user.passwordMatches(credentials.password(), passwords)) {
            throw new InvalidCredentialsException();
        }
        Instant expiresAt = clock.instant().plus(properties.getSessionTtl());
        long sessionId = sessions.open(user.id(), expiresAt);
        return new LoginResponse(user.id(), user.login().value(), sessionId, expiresAt);
    }

    @Transactional
    public MessageResponse logout(long sessionId) {
        if (sessions.findSession(sessionId).isEmpty()) {
            throw new SessionNotFoundException();
        }
        sessions.close(sessionId);
        return new MessageResponse("session closed");
    }

    public List<RoleResponse> listRoles() {
        return roles.findRoles().stream().map(UserMapper::toRole).toList();
    }

    public List<UserListItem> listUsers() {
        List<User> found = users.findUsers();
        Map<Long, List<Role>> grouped = roles.findByUserIds(found.stream().map(User::id).toList());
        return found.stream()
                .map(user -> UserMapper.toListItem(user.withRoles(grouped.getOrDefault(user.id(), List.of()))))
                .toList();
    }

    public UserDetailsResponse getUser(long userId) {
        return details(userId);
    }

    @Transactional
    public UserDetailsResponse update(long userId, ProfileReplacement replacement) {
        User user = requireUser(userId);
        uniqueness.ensureLoginAvailable(replacement.login(), user.id());
        uniqueness.ensureEmailAvailable(replacement.email(), user.id());
        List<Role> assigned = catalog.requireAll(replacement.roleCodes());
        users.updateProfile(user.replaceProfile(
                replacement.login(),
                replacement.email(),
                replacement.name(),
                replacement.birthDate()
        ));
        roles.replace(user.id(), assigned);
        return details(user.id());
    }

    @Transactional
    public UserDetailsResponse patch(long userId, ProfilePatch patch) {
        User user = requireUser(userId);
        if (patch.emailSpecified()) {
            uniqueness.ensureEmailAvailable(patch.email(), user.id());
            users.updateEmail(user.changeEmail(patch.email()));
        }
        if (patch.nameSpecified()) {
            users.updateName(user.rename(patch.name()));
        }
        return details(user.id());
    }

    @Transactional
    public MessageResponse delete(long userId) {
        requireUser(userId);
        users.delete(userId);
        return new MessageResponse("deleted");
    }

    @Transactional
    public RoleAssignmentResponse assignRole(long userId, String roleCode, long callerSessionId) {
        Session session = sessions.findSession(callerSessionId)
                .filter(candidate -> candidate.isUsableAt(clock.instant()))
                .orElseThrow(ForbiddenException::adminRequired);
        requireUser(session.userId()).withRoles(roles.findByUserId(session.userId())).requireAdmin();
        User target = requireUser(userId).withRoles(roles.findByUserId(userId));
        Role role = catalog.require(roleCode);
        target.ensureNotAssigned(role);
        roles.assign(target.id(), role);
        return new RoleAssignmentResponse(userId, role.code(), "role assigned");
    }

    public SessionResponse lastSession(long userId) {
        requireUser(userId);
        Session session = sessions.findLast(userId).orElseThrow(SessionNotFoundException::new);
        return UserMapper.toSession(session);
    }

    private UserDetailsResponse details(long userId) {
        User user = requireUser(userId).withRoles(roles.findByUserId(userId));
        Session lastSession = sessions.findLast(userId).orElse(null);
        return UserMapper.toDetails(user, lastSession);
    }

    private User requireUser(long userId) {
        return users.findUser(userId).orElseThrow(UserNotFoundException::new);
    }
}
