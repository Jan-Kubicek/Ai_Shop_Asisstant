package cz.example.shopassistant.ai.infrastructure.openai;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.example.shopassistant.ai.application.AiException;
import cz.example.shopassistant.ai.application.AiImageClient;
import cz.example.shopassistant.ai.application.AiImageResult;

/**
 * Klient pro OpenAI Images API (gpt-image).
 */
@Component
@ConditionalOnProperty(name = "app.ai.image-provider", havingValue = "openai")
public class OpenAiImageClient implements AiImageClient {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAiImageClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String SAFETY_PREFIX =
            "Marketingová fotka produktu pro český e-shop s dětským oblečením. "
                    + "Žádná nahota, sexualizace ani odhalování těla. Oblečení vždy kompletně oblečené, "
                    + "neutrální pozadí, žádné logo ani text. ";

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final int maxRetries;

    @org.springframework.beans.factory.annotation.Autowired
    public OpenAiImageClient(
            @Value("${app.ai.openai-api-key:}") String apiKey,
            @Value("${app.ai.openai-image-model:gpt-image-1.5}") String model,
            @Value("${app.ai.timeout-seconds:60}") int timeoutSeconds,
            @Value("${app.ai.max-retries:3}") int maxRetries,
            @Value("${app.ai.openai-base-url:https://api.openai.com/v1}") String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxRetries = Math.max(maxRetries, 0);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().requestFactory(factory).baseUrl(baseUrl).build();
    }

    /** Konstruktor pro testy s vlastní base URL. */
    public OpenAiImageClient(String baseUrl, String apiKey, String model, int timeoutSeconds,
            int maxRetries) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxRetries = Math.max(maxRetries, 0);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().requestFactory(factory).baseUrl(baseUrl).build();
    }

    @Override
    public AiImageResult generate(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiException("Není nastaven OPENAI_API_KEY.");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "prompt", SAFETY_PREFIX + (prompt == null ? "" : prompt),
                "size", "1024x1024",
                "n", 1,
                "output_format", "png");

        Exception last = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String raw = restClient.post()
                        .uri("/images/generations")
                        .header("Authorization", "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                ImageResponse response;
                try {
                    response = MAPPER.readValue(raw, ImageResponse.class);
                } catch (JsonProcessingException exception) {
                    LOG.debug("Neplatná odpověď OpenAI Images: {}", exception.getMessage());
                    throw new AiException("Odpověď OpenAI Images není platný JSON.", exception);
                }

                if (response.data() == null || response.data().isEmpty()
                        || response.data().get(0).b64Json() == null) {
                    throw new AiException("OpenAI nevrátil obrázek.");
                }
                byte[] image = Base64.getDecoder().decode(response.data().get(0).b64Json());
                return new AiImageResult(image, "image/png", "model-photo-openai");
            } catch (HttpClientErrorException exception) {
                throw new AiException("OpenAI Images odmítl požadavek: "
                        + exception.getStatusCode(), exception);
            } catch (RestClientException | IllegalStateException | IllegalArgumentException exception) {
                last = exception;
                if (attempt < maxRetries) {
                    sleep(attempt);
                }
            }
        }
        throw new AiException("OpenAI Images volání selhalo po " + (maxRetries + 1)
                + " pokusech.", last);
    }

    private void sleep(int attempt) {
        try {
            Thread.sleep((long) Math.pow(2, attempt) * 300L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private record ImageResponse(java.util.List<ImageData> data) {
        private record ImageData(@JsonProperty("b64_json") String b64Json) {
        }
    }
}
