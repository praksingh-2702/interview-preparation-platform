package com.prepmind.prepapi.dto;

import java.time.Instant;

/**
 * Per-question progress for the current user. solvedAt / nextReviewAt / reviewStage are null while
 * the problem is unsolved.
 */
public record ProgressStatus(boolean solved,
                             boolean revised,
                             Instant solvedAt,
                             Instant nextReviewAt,
                             Integer reviewStage) {

    public static ProgressStatus empty() {
        return new ProgressStatus(false, false, null, null, null);
    }
}
