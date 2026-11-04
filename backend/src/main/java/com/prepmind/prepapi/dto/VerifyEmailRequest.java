package com.prepmind.prepapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") String email,
        @NotBlank(message = "Enter the 6-digit code") @Pattern(regexp = "^\\s*\\d{6}\\s*$", message = "The code is 6 digits") String code) {
}
