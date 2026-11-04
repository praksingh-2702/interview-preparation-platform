package com.prepmind.prepapi.service;

import com.prepmind.prepapi.entity.Role;
import com.prepmind.prepapi.entity.User;
import com.prepmind.prepapi.repository.EmailOtpRepository;
import com.prepmind.prepapi.repository.NoteRepository;
import com.prepmind.prepapi.repository.ProgressEventRepository;
import com.prepmind.prepapi.repository.ProgressRepository;
import com.prepmind.prepapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Deletes accounts together with everything that belongs to them (progress, activity, notes, codes).
 * Also removes abandoned sign-ups: accounts that never verified their email, after
 * app.auth.unverified-retention-hours (0 turns the automatic cleanup off).
 */
@Service
public class AccountCleanup {

    private static final Logger log = LoggerFactory.getLogger(AccountCleanup.class);

    private final UserRepository userRepository;
    private final ProgressRepository progressRepository;
    private final ProgressEventRepository eventRepository;
    private final NoteRepository noteRepository;
    private final EmailOtpRepository otpRepository;
    private final Clock clock;
    private final long retentionHours;

    public AccountCleanup(UserRepository userRepository, ProgressRepository progressRepository,
                          ProgressEventRepository eventRepository, NoteRepository noteRepository,
                          EmailOtpRepository otpRepository, Clock clock,
                          @Value("${app.auth.unverified-retention-hours:24}") long retentionHours) {
        this.userRepository = userRepository;
        this.progressRepository = progressRepository;
        this.eventRepository = eventRepository;
        this.noteRepository = noteRepository;
        this.otpRepository = otpRepository;
        this.clock = clock;
        this.retentionHours = retentionHours;
    }

    /** Removes one account and all its data. Admin accounts are never removed here. */
    @Transactional
    public boolean delete(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.effectiveRole() == Role.ADMIN) {
            return false;
        }
        eventRepository.deleteByUserId(userId);
        progressRepository.deleteByUserId(userId);
        noteRepository.deleteByUserId(userId);
        otpRepository.deleteByUserId(userId);
        userRepository.delete(user);
        return true;
    }

    /** Unverified accounts created more than `age` ago. Returns how many were removed. */
    @Transactional
    public int removeUnverifiedOlderThan(Duration age) {
        List<Long> ids = userRepository.findUnverifiedIdsCreatedBefore(clock.instant().minus(age));
        int removed = 0;
        for (Long id : ids) {
            if (delete(id)) {
                removed++;
            }
        }
        if (removed > 0) {
            log.info("Removed {} unverified account(s) older than {}", removed, age);
        }
        return removed;
    }

    /** Every 6 hours (first run 5 minutes after startup). */
    @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT6H")
    public void scheduledCleanup() {
        if (retentionHours > 0) {
            removeUnverifiedOlderThan(Duration.ofHours(retentionHours));
        }
    }
}
