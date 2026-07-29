package com.geckofly.messenger.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Простой in-memory ограничитель частоты (A3 из AUDIT.md): не более maxHits обращений
 * по ключу за окно window. Фиксированное окно, без внешних зависимостей — достаточно
 * для одного инстанса и аудитории ~50 человек. При рестарте счётчики обнуляются.
 */
@Service
public class RateLimitService {

    private static final int MAX_TRACKED_KEYS = 50_000;

    private record Window(int count, Instant startedAt) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /** Учесть обращение по ключу и бросить 429, если лимит за окно превышен. */
    public void check(String key, int maxHits, Duration window) {
        Instant now = Instant.now();
        Window updated = windows.compute(key, (k, cur) -> {
            if (cur == null || Duration.between(cur.startedAt(), now).compareTo(window) > 0) {
                return new Window(1, now);
            }
            return new Window(cur.count() + 1, cur.startedAt());
        });

        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.entrySet().removeIf(e ->
                    Duration.between(e.getValue().startedAt(), now).compareTo(window) > 0);
        }

        if (updated.count() > maxHits) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests, slow down");
        }
    }
}
