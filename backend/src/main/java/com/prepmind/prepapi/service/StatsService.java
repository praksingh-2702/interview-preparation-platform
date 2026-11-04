package com.prepmind.prepapi.service;

import com.prepmind.prepapi.dto.StatsResponse;
import com.prepmind.prepapi.repository.ProgressEventRepository;
import com.prepmind.prepapi.repository.ProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private static final int HEATMAP_DAYS = 365;

    private final ProgressEventRepository eventRepository;
    private final ProgressRepository progressRepository;
    private final Clock clock;

    public StatsService(ProgressEventRepository eventRepository, ProgressRepository progressRepository, Clock clock) {
        this.eventRepository = eventRepository;
        this.progressRepository = progressRepository;
        this.clock = clock;
    }

    /** @param timezone IANA zone from the browser (e.g. Asia/Kolkata) so "today" matches the user's day. */
    @Transactional(readOnly = true)
    public StatsResponse stats(Long userId, String timezone) {
        ZoneId zone = parseZone(timezone);
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, zone);
        LocalDate from = today.minusDays(HEATMAP_DAYS - 1L);

        Map<LocalDate, Long> perDay = eventRepository.findTimestampsByUserId(userId).stream()
                .map(ts -> LocalDate.ofInstant(ts, zone))
                .collect(Collectors.groupingBy(Function.identity(), TreeMap::new, Collectors.counting()));

        List<StatsResponse.ActivityDay> activity = perDay.entrySet().stream()
                .filter(e -> !e.getKey().isBefore(from) && !e.getKey().isAfter(today))
                .map(e -> new StatsResponse.ActivityDay(e.getKey().toString(), e.getValue()))
                .toList();

        return new StatsResponse(
                activity,
                Streaks.current(perDay.keySet(), today),
                Streaks.longest(perDay.keySet()),
                perDay.size(),
                progressRepository.countDue(userId, now),
                zone.getId());
    }

    private static ZoneId parseZone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (DateTimeException e) {
            return ZoneOffset.UTC;
        }
    }
}
