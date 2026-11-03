package com.prepmind.prepapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record QuestionRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 100) String category,
        @Size(max = 2000) String description,
        @NotBlank @Pattern(regexp = "EASY|MEDIUM|HARD", message = "Difficulty must be EASY, MEDIUM or HARD") String difficulty,
        @NotBlank @Pattern(regexp = "^https?://.+", message = "Problem URL must start with http(s)://") @Size(max = 500) String problemUrl,
        Set<@NotBlank @Size(max = 60) String> companies) {
}
