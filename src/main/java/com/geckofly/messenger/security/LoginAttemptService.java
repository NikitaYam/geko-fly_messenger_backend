package com.geckofly.messenger.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final Map<String, FailureWindow> failures = new ConcurrentHashMap<>();

    private record FailureWindow(int count, Instant startedAt) {}

    /** Вызывать ДО проверки пароля. Бросает 429, если лимит исчерпан. */
    public void checkAllowed(String key) {
        FailureWindow window = failures.get(key);
        if (window == null) {
            return;
        }
        if (isExpired(window)) {
            failures.remove(key);
            return;
        }
        if (window.count() >= MAX_FAILURES) {
            long secondsLeft = WINDOW.minus(Duration.between(window.startedAt(), Instant.now())).toSeconds();
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many login attempts, try again in " + Math.max(secondsLeft, 1) + " seconds");
        }
    }

    public void onFailure(String key) {
        if (failures.size() >= MAX_TRACKED_KEYS) {
            failures.entrySet().removeIf(e -> isExpired(e.getValue()));
        }
        failures.merge(key,
                new FailureWindow(1, Instant.now()),
                (old, fresh) -> isExpired(old) ? fresh
                        : new FailureWindow(old.count() + 1, old.startedAt()));
    }

    public void onSuccess(String key) {
        failures.remove(key);
    }

    private static boolean isExpired(FailureWindow window) {
        return Duration.between(window.startedAt(), Instant.now()).compareTo(WINDOW) > 0;
    }
}