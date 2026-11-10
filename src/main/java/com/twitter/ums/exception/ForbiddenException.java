package com.twitter.ums.exception;

public class ForbiddenException extends RuntimeException {

    public static final String ADMIN_REQUIRED = "у вызывающего нет роли ADMIN";

    public ForbiddenException(String message) {
        super(message);
    }

    public static ForbiddenException adminRequired() {
        return new ForbiddenException(ADMIN_REQUIRED);
    }
}
