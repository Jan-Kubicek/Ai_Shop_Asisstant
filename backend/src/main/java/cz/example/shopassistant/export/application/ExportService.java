package cz.example.shopassistant.export.application;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cz.example.shopassistant.export.ExportApi;
import cz.example.shopassistant.export.domain.RequestResult;
import cz.example.shopassistant.export.domain.RequestResultRepository;

@Service
public class ExportService implements ExportApi {

    private final RequestResultRepository repository;
    private final ObjectMapper objectMapper;

    public ExportService(RequestResultRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveResult(String requestId, String webTextJson, String socialTextJson,
            String wooJson, String marketingTextJson, String productImageUrl,
            String modelImageUrl, String modelMediaId) {
        RequestResult result = repository.findByRequestId(requestId).orElseGet(RequestResult::new);
        result.setRequestId(requestId);
        result.setWebTextJson(webTextJson);
        result.setSocialTextJson(socialTextJson);
        result.setWooJson(wooJson);
        result.setMarketingTextJson(marketingTextJson);
        result.setProductImageUrl(productImageUrl);
        result.setModelImageUrl(modelImageUrl);
        result.setModelMediaId(modelMediaId);
        Instant now = Instant.now();
        if (result.getCreatedAt() == null) {
            result.setCreatedAt(now);
        }
        result.setUpdatedAt(now);
        repository.save(result);
    }

    @Override
    public Optional<ResultData> findByRequestId(String requestId) {
        return repository.findByRequestId(requestId).map(this::toApi);
    }

    @Override
    public void updateResult(String requestId, String webTextJson, String socialTextJson) {
        RequestResult result = repository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("Výsledek neexistuje."));
        if (webTextJson != null && !webTextJson.isBlank()) {
            result.setWebTextJson(webTextJson);
        }
        if (socialTextJson != null && !socialTextJson.isBlank()) {
            result.setSocialTextJson(socialTextJson);
        }
        result.setUpdatedAt(Instant.now());
        repository.save(result);
    }

    @Override
    public String buildWooJson(String requestId, String webTextJson, String socialTextJson, String mediaId,
            java.util.List<cz.example.shopassistant.requests.RequestsApi.SizePrice> sizePrices) {
        try {
            JsonNode web = objectMapper.readTree(webTextJson);
            JsonNode social = objectMapper.readTree(socialTextJson);

            String title = web.path("title").asText();
            if (title.isBlank() || web.path("shortDescription").asText().isBlank()
                    || web.path("longDescription").asText().isBlank()) {
                throw new IllegalArgumentException("Text produktu je neúplný.");
            }
            if (!social.has("instagram") || !social.has("facebook")) {
                throw new IllegalArgumentException("Sociální text je neúplný.");
            }

            ObjectNode product = objectMapper.createObjectNode();
            product.put("name", title);
            product.put("type", "simple");
            product.put("status", "draft");
            product.put("short_description", web.path("shortDescription").asText());
            product.put("description", web.path("longDescription").asText());

            ArrayNode images = product.putArray("images");
            ObjectNode image = images.addObject();
            image.put("src", "/api/v1/media/" + mediaId);
            image.put("alt", title);

            ArrayNode tags = product.putArray("tags");
            for (JsonNode keyword : web.path("keywords")) {
                ObjectNode tag = tags.addObject();
                tag.put("name", keyword.asText());
            }

            ArrayNode variations = product.putArray("variations");
            if (sizePrices != null) {
                for (cz.example.shopassistant.requests.RequestsApi.SizePrice item : sizePrices) {
                    ObjectNode variation = variations.addObject();
                    variation.put("size", item.size());
                    variation.put("price", item.price());
                }
            }

            ObjectNode root = objectMapper.createObjectNode();
            root.put("schemaVersion", "1.0");
            root.put("generatedAt", Instant.now().toString());
            root.put("requestId", requestId);
            root.set("product", product);
            root.set("social", social);

            return objectMapper.writeValueAsString(root);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("WooCommerce export se nepodařilo sestavit.", exception);
        }
    }

    @Override
    public byte[] createZip(ResultData result, byte[] image) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(bytes)) {

            addEntry(zip, "product-text.json", result.webTextJson());
            addEntry(zip, "social-text.json", result.socialTextJson());
            addEntry(zip, "woo-product.json", result.wooJson());
            if (result.marketingTextJson() != null && !result.marketingTextJson().isBlank()) {
                addEntry(zip, "marketing-text.json", result.marketingTextJson());
            }
            if (image != null && image.length > 0) {
                addEntry(zip, "product-image.bin", image);
            }
            zip.finish();
            return bytes.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("ZIP se nepodařilo vytvořit.", exception);
        }
    }

    @Override
    public void deleteByRequestId(String requestId) {
        repository.findByRequestId(requestId).ifPresent(repository::delete);
    }

    private static void addEntry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void addEntry(ZipOutputStream zip, String name, byte[] content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    private ResultData toApi(RequestResult entity) {
        return new ResultData(
                entity.getId(),
                entity.getRequestId(),
                entity.getWebTextJson(),
                entity.getSocialTextJson(),
                entity.getWooJson(),
                entity.getMarketingTextJson(),
                entity.getProductImageUrl(),
                entity.getModelImageUrl(),
                entity.getModelMediaId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
