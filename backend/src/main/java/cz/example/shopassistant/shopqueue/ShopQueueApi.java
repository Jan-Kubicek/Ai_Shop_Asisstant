package cz.example.shopassistant.shopqueue;

import java.time.Instant;

/**
 * Veřejné rozhraní modulu ShopQueue.
 */
public interface ShopQueueApi {

    record QueueItem(
            String id,
            String requestId,
            String status,
            String note,
            Instant createdAt,
            Instant processedAt) {
    }

    void enqueue(String requestId);

    java.util.Optional<QueueItem> findByRequestId(String requestId);

    org.springframework.data.domain.Page<QueueItem> list(int page, int size);

    void markImported(String id);

    byte[] exportJson(String id);
}
