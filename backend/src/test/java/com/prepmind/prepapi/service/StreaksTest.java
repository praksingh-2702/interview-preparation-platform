package com.prepmind.prepapi.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreaksTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);

    @Test
    void currentStreakCountsBackFromToday() {
        assertEquals(3, Streaks.current(Set.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2)), TODAY));
    }

    @Test
    void currentStreakSurvivesUntilEndOfToday() {
        // nothing yet today, but yesterday + the day before count
        assertEquals(2, Streaks.current(Set.of(TODAY.minusDays(1), TODAY.minusDays(2)), TODAY));
    }

    @Test
    void currentStreakBreaksAfterAMissedDay() {
        assertEquals(0, Streaks.current(Set.of(TODAY.minusDays(2), TODAY.minusDays(3)), TODAY));
        assertEquals(0, Streaks.current(Set.of(), TODAY));
    }

    @Test
    void longestStreakFindsBestRun() {
        Set<LocalDate> days = Set.of(
                TODAY.minusDays(10), TODAY.minusDays(9), TODAY.minusDays(8), TODAY.minusDays(7),
                TODAY.minusDays(3), TODAY);
        assertEquals(4, Streaks.longest(days));
        assertEquals(0, Streaks.longest(Set.of()));
    }

    @Test
    void revisionIntervalsGrowAndCap() {
        Instant t = Instant.parse("2026-11-04T00:00:00Z");
        assertEquals(t.plusSeconds(86_400), RevisionSchedule.nextReview(t, 0));
        assertEquals(t.plusSeconds(3 * 86_400), RevisionSchedule.nextReview(t, 1));
        assertEquals(t.plusSeconds(60L * 86_400), RevisionSchedule.nextReview(t, 99));
        assertEquals(t.plusSeconds(86_400), RevisionSchedule.nextReview(t, -1));
    }
}
