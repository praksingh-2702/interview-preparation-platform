package com.prepmind.prepapi.dto;

import java.time.Instant;

/** needsUsername: true after a first Google sign-in, until the user picks a username. */
public record AuthResponse(String token, String username, Instant expiresAt, boolean needsUsername, String role) {
}
