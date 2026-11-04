package com.prepmind.prepapi.service;

import com.prepmind.prepapi.dto.LeaderboardResponse;
import com.prepmind.prepapi.dto.LeaderboardResponse.Entry;
import com.prepmind.prepapi.entity.User;
import com.prepmind.prepapi.repository.ProgressRepository;
import com.prepmind.prepapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Global leaderboard. Ranked: verified users who are listed, have picked a username and have at least
 * one point. Order: points, then current streak; equal points and streak share a rank.
 */
@Service
public class LeaderboardService {

    public static final int MAX_LIMIT = 100;

    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public LeaderboardService(ProgressRepository progressRepository, UserRepository userRepository, Clock clock) {
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse leaderboard(Long viewerId, ZoneId zone, int limit) {
        int top = Math.max(1, Math.min(limit, MAX_LIMIT));
        Map<Long, Points.Tally> tallies = Points.tally(progressRepository.countSolvedByUserAndDifficulty());
        Map<Long, Set<LocalDate>> days = activeDays(zone);
        LocalDate today = LocalDate.ofInstant(clock.instant(), zone);

        Set<Long> ids = new HashSet<>(tallies.keySet());
        ids.add(viewerId);
        Map<Long, User> users = new HashMap<>();
        userRepository.findAllById(ids).forEach(u -> users.put(u.getId(), u));

        record Row(User user, Points.Tally tally, int streak) {
        }
        List<Row> ranked = new ArrayList<>();
        for (Map.Entry<Long, Points.Tally> e : tallies.entrySet()) {
            User u = users.get(e.getKey());
            if (u != null && rankable(u) && e.getValue().points() > 0) {
                ranked.add(new Row(u, e.getValue(), Streaks.current(days.getOrDefault(u.getId(), Set.of()), today)));
            }
        }
        ranked.sort(Comparator.comparingLong((Row r) -> r.tally().points()).reversed()
                .thenComparing(Comparator.comparingInt(Row::streak).reversed())
                .thenComparing(r -> r.user().getUsername().toLowerCase(Locale.ROOT)));

        List<Entry> entries = new ArrayList<>();
        Entry me = null;
        int rank = 0;
        Row previous = null;
        for (int i = 0; i < ranked.size(); i++) {
            Row r = ranked.get(i);
            if (previous == null || previous.tally().points() != r.tally().points() || previous.streak() != r.streak()) {
                rank = i + 1; // standard competition ranking: 1, 2, 2, 4
            }
            previous = r;
            boolean you = r.user().getId().equals(viewerId);
            Entry entry = new Entry(rank, r.user().getUsername(), r.tally().points(), r.tally().completed(),
                    r.streak(), you, true);
            if (i < top) {
                entries.add(entry);
            }
            if (you) {
                me = entry;
            }
        }

        if (me == null) { // hidden, or no points yet
            User viewer = users.get(viewerId);
            Points.Tally t = tallies.getOrDefault(viewerId, Points.Tally.ZERO);
            me = new Entry(null, viewer == null ? null : viewer.getUsername(), t.points(), t.completed(),
                    Streaks.current(days.getOrDefault(viewerId, Set.of()), today), true,
                    viewer != null && viewer.isListedOnLeaderboard());
        }
        return new LeaderboardResponse(entries, me, ranked.size());
    }

    private static boolean rankable(User u) {
        return u.hasVerifiedEmail() && u.isListedOnLeaderboard() && !u.mustChooseUsername();
    }

    private Map<Long, Set<LocalDate>> activeDays(ZoneId zone) {
        Map<Long, Set<LocalDate>> result = new HashMap<>();
        for (Object[] row : progressRepository.findAllSolvedTimestamps()) {
            result.computeIfAbsent((Long) row[0], k -> new HashSet<>())
                    .add(LocalDate.ofInstant((Instant) row[1], zone));
        }
        return result;
    }
}
