package com.prepmind.prepapi.dto;

import java.util.List;

/**
 * entries: the top of the board. me: the viewer's own row (rank null when hidden or not ranked yet).
 * Points: Easy 1, Medium 2, Hard 3 for every row ticked Solved / Read, across all sections.
 * Ties are broken by current streak; equal points and streak share a rank.
 */
public record LeaderboardResponse(List<Entry> entries, Entry me, int totalRanked) {

    public record Entry(Integer rank,
                        String username,
                        long points,
                        long completed,
                        int currentStreak,
                        boolean you,
                        boolean listed) {
    }
}
