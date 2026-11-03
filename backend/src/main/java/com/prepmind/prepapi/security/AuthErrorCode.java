package com.prepmind.prepapi.security;

/** Why a request is unauthenticated; sent to the client as the "code" field of a 401. */
public enum AuthErrorCode {
    AUTH_REQUIRED("Please log in to continue."),
    TOKEN_EXPIRED("Your session has expired. Please log in again."),
    TOKEN_INVALID("Your session is invalid. Please log in again."),
    SESSION_REVOKED("You were signed out because this account logged in somewhere else.");

    private final String message;

    AuthErrorCode(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
