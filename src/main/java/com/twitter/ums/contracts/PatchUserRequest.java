package com.twitter.ums.contracts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

public class PatchUserRequest {

    private String email;
    private String name;
    private boolean emailSpecified;
    private boolean nameSpecified;

    @JsonSetter("email")
    public void setEmail(String email) {
        this.email = email;
        this.emailSpecified = true;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
        this.nameSpecified = true;
    }

    public String email() {
        return email;
    }

    public String name() {
        return name;
    }

    @JsonIgnore
    public boolean hasEmail() {
        return emailSpecified;
    }

    @JsonIgnore
    public boolean hasName() {
        return nameSpecified;
    }
}
