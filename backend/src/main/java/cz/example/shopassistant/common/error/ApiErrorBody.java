package cz.example.shopassistant.common.error;

import java.time.Instant;

/**
 * Jednotný formát chybové odpovědi API.
 */
public record ApiErrorBody(
        Instant timestamp,
        int status,
        String code,
        String message,
        String traceId) {
}
