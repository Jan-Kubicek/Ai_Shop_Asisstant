package cz.example.shopassistant.auth.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Jednoduchý rate limit pro přihlášení. Počítá pokusy v pevném časovém okně
 * zvlášť pro kombinaci IP adresy a e-mailu, aby jeden útočník nezablokoval
 * ostatní uživatele.
 */
@Component
public class LoginRateLimiter {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final int maxAttempts;
    private final Duration window;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public LoginRateLimiter(
            @Value("${app.auth.login-rate-limit.attempts:10}") int maxAttempts,
            @Value("${app.auth.login-rate-limit.window-minutes:5}") long windowMinutes) {
        this.maxAttempts = Math.max(maxAttempts, 1);
        this.window = Duration.ofMinutes(Math.max(windowMinutes, 1));
    }

    public void check(String key) {
        Instant now = Instant.now();
        Window current = windows.compute(key, (ignored, existing) -> {
            if (existing == null || !existing.expiresAt().isAfter(now)) {
                return new Window(1, now.plus(window));
            }
            return new Window(existing.count() + 1, existing.expiresAt());
        });

        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        }

        if (current.count() > maxAttempts) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Příliš mnoho pokusů o přihlášení. Zkuste to prosím později.");
        }
    }

    public void reset(String key) {
        windows.remove(key);
    }

    /** Vymaže všechna okna. Používá se v testech. */
    public void resetAll() {
        windows.clear();
    }

    private record Window(int count, Instant expiresAt) {
    }
}
