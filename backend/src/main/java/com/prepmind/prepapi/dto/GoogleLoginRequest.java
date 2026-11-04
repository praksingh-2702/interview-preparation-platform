package com.prepmind.prepapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** credential: the ID token that Google Identity Services hands to the browser. */
public record GoogleLoginRequest(@NotBlank @Size(max = 4096) String credential) {
}
