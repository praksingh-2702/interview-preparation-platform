package com.prepmind.prepapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** Create a question. Every question links out: LeetCode for DSA, GeeksforGeeks (or similar) for theory. */
public record QuestionRequest(
        @Pattern(regexp = "(?i)DSA|OOPS|OS|CN", message = "Section must be DSA, OOPS, OS or CN") String section,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 100) String category,
        @Size(max = 2000) String description,
        @NotBlank @Pattern(regexp = "EASY|MEDIUM|HARD", message = "Difficulty must be EASY, MEDIUM or HARD") String difficulty,
        @NotBlank @Pattern(regexp = "^https?://.+", message = "Link must start with http(s)://") @Size(max = 500) String problemUrl,
        Set<@NotBlank @Size(max = 60) String> companies) {
}
