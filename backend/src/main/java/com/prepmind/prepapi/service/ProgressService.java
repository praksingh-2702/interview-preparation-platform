package com.prepmind.prepapi.service;

import com.prepmind.prepapi.dto.ProgressStatus;
import com.prepmind.prepapi.dto.ProgressView;
import com.prepmind.prepapi.dto.ReviewRequest;
import com.prepmind.prepapi.entity.Progress;
import com.prepmind.prepapi.entity.ProgressEvent;
import com.prepmind.prepapi.entity.ProgressEventType;
import com.prepmind.prepapi.exception.ApiException;
import com.prepmind.prepapi.repository.ProgressEventRepository;
import com.prepmind.prepapi.repository.ProgressRepository;
import com.prepmind.prepapi.repository.QuestionRepository;
import com.prepmind.prepapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class ProgressService {

    private final ProgressRepository progressRepository;
    private final ProgressEventRepository eventRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final Clock clock;

    public ProgressService(ProgressRepository progressRepository,
                           ProgressEventRepository eventRepository,
                           UserRepository userRepository,
                           QuestionRepository questionRepository,
                           Clock clock) {
        this.progressRepository = progressRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.clock = clock;
    }

    /** Idempotent: sets the flags to exactly the given values. Both false removes the record. */
    @Transactional
    public ProgressStatus update(Long userId, Long questionId, boolean solved, boolean revised) {
        Optional<Progress> existing = progressRepository.findByUserIdAndQuestionId(userId, questionId);
        // Only a brand-new record needs the existence check (an existing one already points at the question).
        if (existing.isEmpty() && !questionRepository.existsById(questionId)) {
            throw ApiException.notFound("Question " + questionId + " does not exist.");
        }
        if (!solved && !revised) {
            existing.ifPresent(progressRepository::delete);
            return ProgressStatus.empty();
        }

        Progress progress = existing.orElseGet(() -> {
            Progress p = new Progress();
            p.setUser(userRepository.getReferenceById(userId));
            p.setQuestion(questionRepository.getReferenceById(questionId));
            return p;
        });
        boolean wasSolved = progress.isSolved();
        boolean wasRevised = progress.isRevised();
        Instant now = clock.instant();

        progress.setSolved(solved);
        progress.setRevised(revised);

        if (solved && !wasSolved) {
            progress.setSolvedAt(now);
            progress.setReviewStage(0);
            progress.setNextReviewAt(RevisionSchedule.nextReview(now, 0));
            logEvent(progress, ProgressEventType.SOLVED, now);
        } else if (!solved) {
            progress.setSolvedAt(null);
            progress.setReviewStage(null);
            progress.setNextReviewAt(null);
        }
        if (revised && !wasRevised) {
            progress.setLastReviewedAt(now);
            logEvent(progress, ProgressEventType.REVISED, now);
        }

        progressRepository.save(progress);
        return toStatus(progress);
    }

    /** Records a review from the revision queue and schedules the next one. */
    @Transactional
    public ProgressStatus review(Long userId, Long questionId, ReviewRequest.Result result) {
        Progress progress = progressRepository.findByUserIdAndQuestionId(userId, questionId)
                .filter(Progress::isSolved)
                .orElseThrow(() -> ApiException.badRequest("NOT_SOLVED",
                        "Mark the problem as solved before reviewing it."));

        Instant now = clock.instant();
        int stage = progress.getReviewStage() == null ? 0 : progress.getReviewStage();
        int nextStage = result == ReviewRequest.Result.GOOD ? Math.min(stage + 1, RevisionSchedule.maxStage()) : 0;

        progress.setReviewStage(nextStage);
        progress.setNextReviewAt(RevisionSchedule.nextReview(now, nextStage));
        progress.setLastReviewedAt(now);
        progress.setRevised(true);
        logEvent(progress, ProgressEventType.REVIEWED, now);

        progressRepository.save(progress);
        return toStatus(progress);
    }

    /** questionId -> status; questions with no record are simply absent. */
    @Transactional(readOnly = true)
    public Map<Long, ProgressStatus> statusMap(Long userId) {
        Map<Long, ProgressStatus> result = new TreeMap<>();
        for (ProgressView v : progressRepository.findViewsByUserId(userId)) {
            result.put(v.questionId(), v.toStatus());
        }
        return result;
    }

    private void logEvent(Progress progress, ProgressEventType type, Instant at) {
        eventRepository.save(new ProgressEvent(progress.getUser(), progress.getQuestion(), type, at));
    }

    private static ProgressStatus toStatus(Progress p) {
        return new ProgressStatus(p.isSolved(), p.isRevised(), p.getSolvedAt(), p.getNextReviewAt(), p.getReviewStage());
    }
}
