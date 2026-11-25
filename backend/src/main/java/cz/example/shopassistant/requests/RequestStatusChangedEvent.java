package cz.example.shopassistant.requests;

/**
 * Událost změny stavu požadavku. Posílá se klientům připojeným přes SSE.
 */
public record RequestStatusChangedEvent(
        String requestId,
        String status,
        String errorMessage,
        String description,
        String createdAt,
        String finishedAt) {
}
