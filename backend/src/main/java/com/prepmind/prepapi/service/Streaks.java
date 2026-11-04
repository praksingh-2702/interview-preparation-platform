package com.prepmind.prepapi.service;

import java.time.LocalDate;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/** Pure streak arithmetic over the set of days a user was active. */
public final class Streaks {

    private Streaks() {
    }

    /**
     * Consecutive active days ending today. If there is no activity yet today, the streak is still
     * alive and counted up to yesterday (you have until midnight to keep it).
     */
    public static int current(Set<LocalDate> activeDays, LocalDate today) {
        LocalDate day = activeDays.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (activeDays.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }

    public static int longest(Set<LocalDate> activeDays) {
        SortedSet<LocalDate> sorted = new TreeSet<>(activeDays);
        int best = 0;
        int run = 0;
        LocalDate previous = null;
        for (LocalDate d : sorted) {
            run = (previous != null && previous.plusDays(1).equals(d)) ? run + 1 : 1;
            best = Math.max(best, run);
            previous = d;
        }
        return best;
    }
}
