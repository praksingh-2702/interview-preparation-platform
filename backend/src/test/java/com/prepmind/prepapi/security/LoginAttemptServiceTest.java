package com.prepmind.prepapi.security;

import com.prepmind.prepapi.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptServiceTest {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
    private final Clock clock = new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    };
    private final LoginAttemptService service = new LoginAttemptService(3, Duration.ofMinutes(15), clock);

    @Test
    void locksAfterMaxFailuresAndUnlocksAfterWindow() {
        String key = LoginAttemptService.key("Om", "1.2.3.4");
        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> service.checkAllowed(key));
            service.recordFailure(key);
        }
        assertThrows(TooManyRequestsException.class, () -> service.checkAllowed(key));

        now.set(now.get().plus(Duration.ofMinutes(15)));
        assertDoesNotThrow(() -> service.checkAllowed(key));
    }

    @Test
    void successResetsCounterAndKeysAreIsolated() {
        String key = LoginAttemptService.key("om", "1.2.3.4");
        service.recordFailure(key);
        service.recordFailure(key);
        service.recordSuccess(key);
        service.recordFailure(key);
        assertDoesNotThrow(() -> service.checkAllowed(key));

        String otherIp = LoginAttemptService.key("om", "5.6.7.8");
        for (int i = 0; i < 3; i++) {
            service.recordFailure(otherIp);
        }
        assertThrows(TooManyRequestsException.class, () -> service.checkAllowed(otherIp));
        assertDoesNotThrow(() -> service.checkAllowed(key));
    }
}
