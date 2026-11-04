package com.prepmind.prepapi.dto;

/** Returned after sign-up: the account exists but must be verified with the emailed code. */
public record RegisterResponse(String message, String email) {
}
