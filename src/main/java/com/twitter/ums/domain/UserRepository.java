package com.twitter.ums.domain;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    long insert(NewUser user);

    Optional<User> findUser(long id);

    Optional<User> findByLogin(Login login);

    Optional<User> findByEmail(EmailAddress email);

    List<User> findUsers();

    void updateProfile(User user);

    void updateEmail(User user);

    void updateName(User user);

    void delete(long id);
}
