package com.prepmind.prepapi.dto;

import jakarta.validation.constraints.NotNull;

/** Body for the legacy POST /api/progress/toggle endpoint. */
public record ProgressRequest(@NotNull(message = "questionId is required") Long questionId,
                              boolean solved,
                              boolean revised) {
}
