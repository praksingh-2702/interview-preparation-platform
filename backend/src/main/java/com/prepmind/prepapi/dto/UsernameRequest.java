package com.prepmind.prepapi.dto;

import jakarta.validation.constraints.NotBlank;

/** Format rules live in Usernames so registration, the picker and the availability check agree. */
public record UsernameRequest(@NotBlank(message = "Username is required") String username) {
}
