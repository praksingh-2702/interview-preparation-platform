package com.prepmind.prepapi.dto;

import jakarta.validation.constraints.NotNull;

/** Body for POST /api/progress/{questionId}/review. */
public record ReviewRequest(@NotNull(message = "result is required (GOOD or AGAIN)") Result result) {

    public enum Result {
        /** Remembered it: push the next review further out. */
        GOOD,
        /** Struggled: bring it back tomorrow. */
        AGAIN
    }
}
