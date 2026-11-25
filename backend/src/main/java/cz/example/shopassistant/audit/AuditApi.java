package cz.example.shopassistant.audit;

import java.time.Instant;
import java.util.Map;

/**
 * Veřejné rozhraní modulu Audit.
 */
public interface AuditApi {

    record AuditEventView(
            String id,
            String userId,
            String action,
            String ip,
            Map<String, Object> details,
            Instant timestamp) {
    }

    void record(String userId, String action, String ip, Map<String, Object> details);

    org.springframework.data.domain.Page<AuditEventView> list(
            int page, int size, String action, String account, boolean newestFirst);
}
