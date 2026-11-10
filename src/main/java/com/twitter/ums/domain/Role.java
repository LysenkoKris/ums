package com.twitter.ums.domain;

public record Role(long id, String code, String name) {

    public boolean hasCode(String expected) {
        return code.equals(expected);
    }
}
