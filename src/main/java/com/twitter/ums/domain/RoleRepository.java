package com.twitter.ums.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RoleRepository {

    List<Role> findRoles();

    Optional<Role> findByCode(String code);

    List<Role> findByUserId(long userId);

    Map<Long, List<Role>> findByUserIds(Collection<Long> userIds);

    void assign(long userId, Role role);

    void replace(long userId, List<Role> roles);
}
