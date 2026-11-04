package com.prepmind.prepapi.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Both fields blank deletes the note. */
public record NoteRequest(
        @Size(max = 5000, message = "Notes can be at most 5000 characters") String content,
        @Size(max = 500, message = "Link is too long")
        @Pattern(regexp = "^$|^https?://\\S+$", message = "Solution link must start with http:// or https://")
        String solutionUrl) {
}
