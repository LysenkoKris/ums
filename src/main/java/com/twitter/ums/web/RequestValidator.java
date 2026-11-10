package com.twitter.ums.web;

import com.twitter.ums.contracts.AssignRoleRequest;
import com.twitter.ums.contracts.LoginRequest;
import com.twitter.ums.contracts.PatchUserRequest;
import com.twitter.ums.contracts.RegisterRequest;
import com.twitter.ums.contracts.UpdateUserRequest;
import com.twitter.ums.domain.BirthDate;
import com.twitter.ums.domain.Credentials;
import com.twitter.ums.domain.DisplayName;
import com.twitter.ums.domain.EmailAddress;
import com.twitter.ums.domain.Login;
import com.twitter.ums.domain.PlainPassword;
import com.twitter.ums.domain.PositiveId;
import com.twitter.ums.domain.ProfilePatch;
import com.twitter.ums.domain.ProfileReplacement;
import com.twitter.ums.domain.Registration;
import com.twitter.ums.domain.RoleCode;
import com.twitter.ums.exception.ValidationException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RequestValidator {

    private final Clock clock;

    public RequestValidator(Clock clock) {
        this.clock = clock;
    }

    public Registration validateRegister(RegisterRequest request) {
        if (request == null) {
            throw new ValidationException("request body is required");
        }
        return new Registration(
                Login.parse(request.login()),
                PlainPassword.parse(request.password()),
                EmailAddress.parse(request.email()),
                DisplayName.optional(request.name()),
                BirthDate.optional(request.birthDate(), today())
        );
    }

    public Credentials validateLogin(LoginRequest request) {
        if (request == null) {
            throw new ValidationException("request body is required");
        }
        return new Credentials(Login.lookup(request.login()), PlainPassword.forCheck(request.password()));
    }

    public ProfileReplacement validateUpdate(UpdateUserRequest request) {
        if (request == null) {
            throw new ValidationException("request body is required");
        }
        if (request.roles() == null || request.roles().isEmpty()) {
            throw new ValidationException("roles are required");
        }
        List<String> roleCodes = new ArrayList<>();
        for (String role : request.roles()) {
            roleCodes.add(RoleCode.normalize(role));
        }
        return new ProfileReplacement(
                Login.parse(request.login()),
                EmailAddress.parse(request.email()),
                DisplayName.optional(request.name()),
                BirthDate.optional(request.birthDate(), today()),
                List.copyOf(roleCodes)
        );
    }

    public ProfilePatch validatePatch(PatchUserRequest request) {
        if (request == null || (!request.hasEmail() && !request.hasName())) {
            throw new ValidationException("at least one of email or name is required");
        }
        EmailAddress email = request.hasEmail() ? EmailAddress.parse(request.email()) : null;
        DisplayName name = request.hasName() ? DisplayName.parse(request.name()) : null;
        return new ProfilePatch(email, name, request.hasEmail(), request.hasName());
    }

    public String validateAssignRole(AssignRoleRequest request) {
        if (request == null) {
            throw new ValidationException("request body is required");
        }
        return RoleCode.normalize(request.role());
    }

    public long validateUserId(long userId) {
        return PositiveId.require(userId);
    }

    public long requireSessionId(String raw) {
        return PositiveId.sessionHeader(raw);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
