package com.prepmind.prepapi.service;

import com.prepmind.prepapi.entity.Progress;
import com.prepmind.prepapi.entity.ProgressEvent;
import com.prepmind.prepapi.entity.ProgressEventType;
import com.prepmind.prepapi.repository.ProgressEventRepository;
import com.prepmind.prepapi.repository.ProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * One-time upgrade of v1 data, safe to run on every start:
 *  1. If the activity log is empty, create one event per existing solved/revised record so the
 *     heatmap isn't blank for existing users.
 *  2. Give every solved problem without a revision schedule one (due 1 day after it was solved).
 */
@Component
@Order(100)
public class ProgressBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProgressBackfill.class);

    private final ProgressRepository progressRepository;
    private final ProgressEventRepository eventRepository;
    private final Clock clock;

    public ProgressBackfill(ProgressRepository progressRepository, ProgressEventRepository eventRepository, Clock clock) {
        this.progressRepository = progressRepository;
        this.eventRepository = eventRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Instant now = clock.instant();

        // Events first: they need the original updated_at, which saving a Progress row overwrites.
        if (eventRepository.count() == 0) {
            List<ProgressEvent> events = new ArrayList<>();
            for (Progress p : progressRepository.findAll()) {
                Instant at = originalTime(p, now);
                if (p.isSolved()) {
                    events.add(new ProgressEvent(p.getUser(), p.getQuestion(), ProgressEventType.SOLVED, at));
                }
                if (p.isRevised()) {
                    events.add(new ProgressEvent(p.getUser(), p.getQuestion(), ProgressEventType.REVISED, at));
                }
            }
            if (!events.isEmpty()) {
                eventRepository.saveAll(events);
                log.info("Backfilled {} activity events from existing progress", events.size());
            }
        }

        List<Progress> unscheduled = progressRepository.findBySolvedTrueAndNextReviewAtIsNull();
        for (Progress p : unscheduled) {
            Instant solvedAt = p.getSolvedAt() != null ? p.getSolvedAt() : originalTime(p, now);
            p.setSolvedAt(solvedAt);
            p.setReviewStage(0);
            p.setNextReviewAt(RevisionSchedule.nextReview(solvedAt, 0));
        }
        if (!unscheduled.isEmpty()) {
            progressRepository.saveAll(unscheduled);
            log.info("Scheduled revisions for {} previously solved problems", unscheduled.size());
        }
    }

    /** v1 stored updated_at as server-local time; Render runs in UTC. */
    private static Instant originalTime(Progress p, Instant fallback) {
        return p.getUpdatedAt() != null ? p.getUpdatedAt().toInstant(ZoneOffset.UTC) : fallback;
    }
}
