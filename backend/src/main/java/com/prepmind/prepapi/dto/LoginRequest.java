package com.prepmind.prepapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** "username" accepts either the username or the account's email address. */
public record LoginRequest(
        @NotBlank(message = "Username or email is required") @Size(max = 254) String username,
        @NotBlank(message = "Password is required") @Size(max = 200) String password) {
}
