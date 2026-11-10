package com.twitter.ums.config;

import com.twitter.ums.domain.DisplayName;
import com.twitter.ums.domain.EmailAddress;
import com.twitter.ums.domain.Login;
import com.twitter.ums.domain.NewUser;
import com.twitter.ums.domain.PasswordHasher;
import com.twitter.ums.domain.PlainPassword;
import com.twitter.ums.domain.Role;
import com.twitter.ums.domain.RoleCode;
import com.twitter.ums.domain.RoleRepository;
import com.twitter.ums.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordHasher passwords;
    private final AppProperties properties;

    public AdminSeeder(
            UserRepository users,
            RoleRepository roles,
            PasswordHasher passwords,
            AppProperties properties
    ) {
        this.users = users;
        this.roles = roles;
        this.passwords = passwords;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.Admin admin = properties.getAdmin();
        Login login = Login.parse(admin.getLogin());
        if (users.findByLogin(login).isPresent()) {
            return;
        }
        long id = users.insert(new NewUser(
                login,
                EmailAddress.parse(admin.getEmail()),
                passwords.hash(PlainPassword.parse(admin.getPassword())),
                DisplayName.parse(admin.getName()),
                null
        ));
        Role role = roles.findByCode(RoleCode.ADMIN)
                .orElseThrow(() -> new IllegalStateException("role ADMIN is not configured"));
        roles.assign(id, role);
        log.info("Created bootstrap admin user '{}'", login.value());
    }
}
