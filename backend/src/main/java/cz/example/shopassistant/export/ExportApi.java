package cz.example.shopassistant.export;

import java.time.Instant;

/**
 * Veřejné rozhraní modulu Export.
 */
public interface ExportApi {

    record ResultData(
            String id,
            String requestId,
            String webTextJson,
            String socialTextJson,
            String wooJson,
            String modelImageUrl,
            Instant createdAt,
            Instant updatedAt) {
    }

    void saveResult(String requestId, String webTextJson, String socialTextJson,
            String wooJson, String modelImageUrl);

    java.util.Optional<ResultData> findByRequestId(String requestId);

    void updateResult(String requestId, String webTextJson, String socialTextJson);

    String buildWooJson(String requestId, String webTextJson, String socialTextJson, String mediaId,
            java.util.List<cz.example.shopassistant.requests.RequestsApi.SizePrice> sizePrices);

    byte[] createZip(ResultData result, byte[] image);
}
