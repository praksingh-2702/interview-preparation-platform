package com.prepmind.prepapi.service;

import com.prepmind.prepapi.dto.AuthResponse;
import com.prepmind.prepapi.dto.UserResponse;
import com.prepmind.prepapi.dto.UsernameAvailability;
import com.prepmind.prepapi.entity.User;
import com.prepmind.prepapi.exception.ApiException;
import com.prepmind.prepapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/** The signed-in user's own profile: username (unique, case-insensitive) and leaderboard visibility. */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final UserRepository userRepository;
    private final AuthService authService;

    public AccountService(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return UserResponse.from(load(userId));
    }

    /** For the live check while typing. viewerId (nullable) lets you "keep" your own current name. */
    @Transactional(readOnly = true)
    public UsernameAvailability availability(String raw, Long viewerId) {
        Optional<String> problem = Usernames.formatProblem(raw);
        if (problem.isPresent()) {
            return new UsernameAvailability(false, problem.get());
        }
        String name = raw.trim();
        boolean taken = viewerId == null
                ? userRepository.existsByUsernameIgnoreCase(name)
                : userRepository.existsByUsernameIgnoreCaseAndIdNot(name, viewerId);
        return taken
                ? new UsernameAvailability(false, "That username is taken.")
                : new UsernameAvailability(true, "Available");
    }

    /**
     * Sets the username (the Google first-sign-in picker, or a later change from Settings) and returns a
     * fresh token for the same session, so nothing else on the user's devices changes.
     */
    @Transactional
    public AuthResponse changeUsername(Long userId, String raw) {
        String name = Usernames.requireValid(raw);
        User user = load(userId);
        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(name, userId)) {
            throw taken();
        }
        String previous = user.getUsername();
        user.setUsername(name);
        user.setNeedsUsername(false);
        try {
            userRepository.saveAndFlush(user); // the unique index settles any race between two people
        } catch (DataIntegrityViolationException e) {
            throw taken();
        }
        if (!name.equals(previous)) {
            log.info("Username changed: '{}' -> '{}'", previous, name);
        }
        return authService.tokenFor(user);
    }

    @Transactional
    public UserResponse setLeaderboardVisibility(Long userId, boolean show) {
        User user = load(userId);
        user.setShowOnLeaderboard(show);
        return UserResponse.from(user);
    }

    private User load(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found."));
    }

    private static ApiException taken() {
        return ApiException.conflict("USERNAME_TAKEN", "That username is taken.")
                .with("errors", Map.of("username", "That username is taken."));
    }
}
