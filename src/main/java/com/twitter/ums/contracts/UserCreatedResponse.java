package com.twitter.ums.contracts;

public record UserCreatedResponse(long id, String login, String email) {
}
