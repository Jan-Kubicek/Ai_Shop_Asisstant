package cz.example.shopassistant.requests;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Veřejné rozhraní modulu Requests.
 */
public interface RequestsApi {

    record RequestView(
            String id,
            String userId,
            String description,
            String options,
            String status,
            String errorMessage,
            String mediaId,
            Instant createdAt,
            Instant finishedAt) {
    }

    Optional<RequestView> findById(String id);

    List<RequestView> findPending(int limit);

    void markProcessing(String id);

    void markReview(String id);

    void markFailed(String id, String message);

    void markQueuedForShop(String id);

    long count();

    long countCreatedSince(java.time.Instant since);

    long countFailed();
}
