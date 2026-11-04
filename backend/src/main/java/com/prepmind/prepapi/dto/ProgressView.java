package com.prepmind.prepapi.dto;

import java.time.Instant;

/** JPQL projection used to read a user's progress without loading Question entities. */
public record ProgressView(Long questionId,
                           boolean solved,
                           boolean revised,
                           Instant solvedAt,
                           Instant nextReviewAt,
                           Integer reviewStage) {

    public ProgressStatus toStatus() {
        return new ProgressStatus(solved, revised, solvedAt, nextReviewAt, reviewStage);
    }
}
