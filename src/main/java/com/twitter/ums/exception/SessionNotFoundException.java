package com.twitter.ums.exception;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException() {
        super("session not found");
    }
}
