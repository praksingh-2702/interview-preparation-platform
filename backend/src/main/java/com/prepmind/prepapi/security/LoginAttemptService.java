package com.prepmind.prepapi.security;

import com.prepmind.prepapi.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory brute-force guard: after N failed logins for the same username from the same IP,
 * further attempts are rejected until the window expires. Keyed by username+IP so an attacker
 * cannot lock a real user out from a different network.
 * (In-memory is fine for a single instance; move to Redis if the API is ever scaled out.)
 */
@Component
public class LoginAttemptService {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final int maxFailures;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptService(@Value("${app.auth.max-failed-logins:5}") int maxFailures,
                               @Value("${app.auth.lockout-minutes:15}") long lockoutMinutes) {
        this(maxFailures, Duration.ofMinutes(lockoutMinutes), Clock.systemUTC());
    }

    LoginAttemptService(int maxFailures, Duration window, Clock clock) {
        this.maxFailures = maxFailures;
        this.window = window;
        this.clock = clock;
    }

    public static String key(String username, String clientIp) {
        return (username == null ? "" : username.trim().toLowerCase(Locale.ROOT)) + "|" + clientIp;
    }

    /** Throws 429 if this key is currently locked out. */
    public void checkAllowed(String key) {
        Attempts a = attempts.get(key);
        if (a == null) {
            return;
        }
        Instant now = clock.instant();
        if (isExpired(a, now)) {
            attempts.remove(key, a);
            return;
        }
        if (a.count() >= maxFailures) {
            long secondsLeft = Math.max(1, Duration.between(now, a.windowStart().plus(window)).toSeconds());
            throw new TooManyRequestsException(secondsLeft);
        }
    }

    public void recordFailure(String key) {
        Instant now = clock.instant();
        attempts.compute(key, (k, a) -> (a == null || isExpired(a, now))
                ? new Attempts(1, now)
                : new Attempts(a.count() + 1, a.windowStart()));
        if (attempts.size() > MAX_TRACKED_KEYS) {
            attempts.entrySet().removeIf(e -> isExpired(e.getValue(), now));
        }
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    private boolean isExpired(Attempts a, Instant now) {
        return !now.isBefore(a.windowStart().plus(window));
    }

    private record Attempts(int count, Instant windowStart) {
    }
}
