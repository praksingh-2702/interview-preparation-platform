package com.prepmind.prepapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Username is required") @Size(max = 100) String username,
        @NotBlank(message = "Password is required") @Size(max = 200) String password) {
}
