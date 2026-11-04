package com.prepmind.prepapi.dto;

import java.util.List;

/**
 * Activity summary for the dashboard.
 * activity: one entry per active day in the last 365 days (date as yyyy-MM-dd in the requested timezone).
 */
public record StatsResponse(List<ActivityDay> activity,
                            int currentStreak,
                            int longestStreak,
                            int activeDays,
                            long dueForRevision,
                            String timezone) {

    public record ActivityDay(String date, long count) {
    }
}
