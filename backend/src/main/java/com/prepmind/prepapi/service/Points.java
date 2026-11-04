package com.prepmind.prepapi.service;

/** Leaderboard points per ticked row: Easy 1, Medium 2, Hard 3 (any section). */
public final class Points {

    private Points() {
    }

    public static long of(String difficulty) {
        if (difficulty == null) {
            return 0;
        }
        return switch (difficulty) {
            case "EASY" -> 1;
            case "MEDIUM" -> 2;
            case "HARD" -> 3;
            default -> 0;
        };
    }

    /** Totals per user from rows of [userId, difficulty, count]. */
    public static java.util.Map<Long, Tally> tally(java.util.List<Object[]> rows) {
        java.util.Map<Long, Tally> result = new java.util.HashMap<>();
        for (Object[] row : rows) {
            Long userId = (Long) row[0];
            long count = ((Number) row[2]).longValue();
            result.merge(userId, new Tally(count * of((String) row[1]), count), Tally::plus);
        }
        return result;
    }

    public record Tally(long points, long completed) {
        public static final Tally ZERO = new Tally(0, 0);

        Tally plus(Tally other) {
            return new Tally(points + other.points, completed + other.completed);
        }
    }
}
