package com.twitter.ums.domain;

public record ProfilePatch(EmailAddress email, DisplayName name, boolean emailSpecified, boolean nameSpecified) {
}
