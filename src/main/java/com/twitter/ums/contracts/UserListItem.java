package com.twitter.ums.contracts;

import java.util.List;

public record UserListItem(long id, String login, String email, List<RoleResponse> roles) {
}
