package com.prepmind.prepapi.dto;

/** Body for PUT /api/progress/{questionId}. Both flags false removes the record. */
public record ProgressUpdateRequest(boolean solved, boolean revised) {
}
