package com.prepmind.prepapi.google;

/** The verified claims we use from a Google ID token. */
public record GoogleIdentity(String subject, String email, boolean emailVerified, String name) {
}
