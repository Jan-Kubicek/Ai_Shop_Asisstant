package cz.example.shopassistant.ai.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Fixed-window rate limit pro AI volání podle uživatele.
 */
@Component
public class AiRateLimiter {

    private final int maxAttempts;
    private final Duration window;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public AiRateLimiter(
            @Value("${app.ai.rate-limit.attempts:20}") int maxAttempts,
            @Value("${app.ai.rate-limit.window-minutes:5}") long windowMinutes) {
        this.maxAttempts = Math.max(maxAttempts, 1);
        this.window = Duration.ofMinutes(Math.max(windowMinutes, 1));
    }

    public void check(String userId) {
        Instant now = Instant.now();
        Window current = windows.compute(userId, (ignored, existing) -> {
            if (existing == null || !existing.expiresAt().isAfter(now)) {
                return new Window(1, now.plus(window));
            }
            return new Window(existing.count() + 1, existing.expiresAt());
        });
        if (current.count() > maxAttempts) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Příliš mnoho AI požadavků. Zkuste to prosím později.");
        }
    }

    private record Window(int count, Instant expiresAt) {
    }
}
