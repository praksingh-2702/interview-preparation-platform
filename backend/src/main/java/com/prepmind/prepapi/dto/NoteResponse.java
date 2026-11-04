package com.prepmind.prepapi.dto;

import java.time.Instant;

public record NoteResponse(Long questionId, String content, String solutionUrl, Instant updatedAt) {
}
