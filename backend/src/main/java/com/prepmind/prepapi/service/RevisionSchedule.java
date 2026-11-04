package com.prepmind.prepapi.service;

import java.time.Duration;
import java.time.Instant;

/**
 * Spaced repetition: after solving, a problem comes back in 1 day; each successful review pushes the
 * next one further out (3, 7, 14, 30, 60 days). "Again" resets it to 1 day.
 */
public final class RevisionSchedule {

    static final int[] INTERVAL_DAYS = {1, 3, 7, 14, 30, 60};

    private RevisionSchedule() {
    }

    public static int maxStage() {
        return INTERVAL_DAYS.length - 1;
    }

    public static Instant nextReview(Instant from, int stage) {
        int s = Math.max(0, Math.min(stage, maxStage()));
        return from.plus(Duration.ofDays(INTERVAL_DAYS[s]));
    }
}
