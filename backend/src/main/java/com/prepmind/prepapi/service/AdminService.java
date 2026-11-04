package com.prepmind.prepapi.service;

import com.prepmind.prepapi.dto.AdminOverview;
import com.prepmind.prepapi.dto.AdminUserView;
import com.prepmind.prepapi.entity.Role;
import com.prepmind.prepapi.entity.Sections;
import com.prepmind.prepapi.entity.User;
import com.prepmind.prepapi.exception.ApiException;
import com.prepmind.prepapi.repository.ProgressEventRepository;
import com.prepmind.prepapi.repository.ProgressRepository;
import com.prepmind.prepapi.repository.QuestionRepository;
import com.prepmind.prepapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Admin panel: who uses the app, and who may edit questions or manage users. */
@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final ProgressRepository progressRepository;
    private final ProgressEventRepository eventRepository;
    private final QuestionRepository questionRepository;
    private final Clock clock;

    public AdminService(UserRepository userRepository, ProgressRepository progressRepository,
                        ProgressEventRepository eventRepository, QuestionRepository questionRepository, Clock clock) {
        this.userRepository = userRepository;
        this.progressRepository = progressRepository;
        this.eventRepository = eventRepository;
        this.questionRepository = questionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminOverview overview() {
        Instant weekAgo = clock.instant().minus(Duration.ofDays(7));

        Map<String, Long> byRole = new LinkedHashMap<>();
        for (Role r : Role.values()) {
            byRole.put(r.name(), 0L);
        }
        for (Object[] row : userRepository.countByRole()) {
            String role = row[0] == null ? Role.USER.name() : ((Role) row[0]).name(); // null = pre-v3 = USER
            byRole.merge(role, ((Number) row[1]).longValue(), Long::sum);
        }

        Map<String, Long> bySection = new LinkedHashMap<>();
        Sections.ALL.forEach(s -> bySection.put(s, 0L));
        for (Object[] row : questionRepository.countBySection()) {
            String section = row[0] == null ? Sections.DSA : (String) row[0];
            bySection.merge(section, ((Number) row[1]).longValue(), Long::sum);
        }

        return new AdminOverview(
                userRepository.count(),
                userRepository.countByEmailVerifiedTrue(),
                userRepository.countByGoogleSubjectIsNotNull(),
                userRepository.countByCreatedAtAfter(weekAgo),
                eventRepository.countActiveUsersSince(weekAgo),
                byRole,
                bySection);
    }

    /** Newest first, at most 200. query matches username or email. */
    @Transactional(readOnly = true)
    public List<AdminUserView> users(String query) {
        List<User> users = query == null || query.isBlank()
                ? userRepository.findTop200ByOrderByCreatedAtDesc()
                : userRepository.findTop200ByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByCreatedAtDesc(
                        query.trim(), query.trim());
        Map<Long, Points.Tally> tallies = Points.tally(progressRepository.countSolvedByUserAndDifficulty());
        return users.stream().map(u -> view(u, tallies.getOrDefault(u.getId(), Points.Tally.ZERO))).toList();
    }

    /** Admins can't change their own role, so there is always at least one admin left. */
    @Transactional
    public AdminUserView changeRole(Long actorId, Long userId, Role role) {
        if (actorId.equals(userId)) {
            throw ApiException.badRequest("CANNOT_CHANGE_OWN_ROLE", "You can't change your own role.");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found."));
        Role before = user.effectiveRole();
        user.setRole(role);
        log.info("Role of '{}' changed {} -> {} by user #{}", user.getUsername(), before, role, actorId);
        Map<Long, Points.Tally> tallies = Points.tally(progressRepository.countSolvedByUserAndDifficulty());
        return view(user, tallies.getOrDefault(userId, Points.Tally.ZERO));
    }

    private static AdminUserView view(User u, Points.Tally tally) {
        return new AdminUserView(u.getId(), u.getUsername(), u.getEmail(), u.effectiveRole().name(),
                u.hasVerifiedEmail(), u.getGoogleSubject() != null, u.isListedOnLeaderboard(), u.getCreatedAt(),
                tally.completed(), tally.points());
    }
}
