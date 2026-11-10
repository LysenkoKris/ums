package com.twitter.ums.service;

import com.twitter.ums.domain.Role;
import com.twitter.ums.domain.RoleCode;
import com.twitter.ums.domain.RoleRepository;
import com.twitter.ums.exception.ValidationException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class RoleCatalog {

    private final RoleRepository roles;

    RoleCatalog(RoleRepository roles) {
        this.roles = roles;
    }

    Role require(String code) {
        String normalized = RoleCode.normalize(code);
        return roles.findByCode(normalized).orElseThrow(() -> new ValidationException("unknown role"));
    }

    Role system(String code) {
        return roles.findByCode(code).orElseThrow(() -> new IllegalStateException("role " + code + " is not configured"));
    }

    List<Role> requireAll(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            throw new ValidationException("roles are required");
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String code : codes) {
            unique.add(RoleCode.normalize(code));
        }
        List<Role> resolved = new ArrayList<>();
        for (String code : unique) {
            resolved.add(require(code));
        }
        return resolved;
    }
}
