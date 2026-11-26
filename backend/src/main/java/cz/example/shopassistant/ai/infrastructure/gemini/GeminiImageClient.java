package cz.example.shopassistant.ai.infrastructure.gemini;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.example.shopassistant.ai.application.AiException;
import cz.example.shopassistant.ai.application.AiImageClient;
import cz.example.shopassistant.ai.application.AiImageResult;

/**
 * Klient pro Gemini image modely (Nano Banana) přes generateContent.
 */
@Component
@ConditionalOnProperty(name = "app.ai.image-provider", havingValue = "gemini")
public class GeminiImageClient implements AiImageClient {

    private static final Logger LOG = LoggerFactory.getLogger(GeminiImageClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String SAFETY_PREFIX =
            "Marketingová fotka produktu pro český e-shop s dětským oblečením. "
                    + "Žádná nahota, sexualizace ani odhalování těla. Dítě vždy plně oblečené, "
                    + "neutrální pozadí, žádné logo ani text. ";

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final int maxRetries;

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiImageClient(
            @Value("${app.ai.gemini-api-key:}") String apiKey,
            @Value("${app.ai.gemini-image-model:gemini-2.5-flash-image}") String model,
            @Value("${app.ai.timeout-seconds:60}") int timeoutSeconds,
            @Value("${app.ai.max-retries:3}") int maxRetries,
            @Value("${app.ai.gemini-base-url:https://generativelanguage.googleapis.com}") String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxRetries = Math.max(maxRetries, 0);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().requestFactory(factory).baseUrl(baseUrl).build();
    }

    /** Konstruktor pro testy s vlastní base URL. */
    public GeminiImageClient(String baseUrl, String apiKey, String model,
            int timeoutSeconds, int maxRetries) {
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
            throw new AiException("Není nastaven GEMINI_API_KEY.");
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text",
                        SAFETY_PREFIX + (prompt == null ? "" : prompt))))),
                "generationConfig", Map.of("responseModalities", List.of("TEXT", "IMAGE")));

        Exception last = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String raw = restClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1beta/models/{model}:generateContent")
                                .queryParam("key", apiKey)
                                .build(model))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                GenerateResponse response;
                try {
                    response = MAPPER.readValue(raw, GenerateResponse.class);
                } catch (JsonProcessingException exception) {
                    LOG.debug("Neplatná odpověď Gemini Images: {}", exception.getMessage());
                    throw new AiException("Odpověď Gemini Images není platný JSON.", exception);
                }

                if (response.candidates() == null || response.candidates().isEmpty()
                        || response.candidates().get(0).content() == null
                        || response.candidates().get(0).content().parts() == null) {
                    throw new AiException("Gemini nevrátil obrázek.");
                }
                for (Part part : response.candidates().get(0).content().parts()) {
                    if (part.inlineData() != null && part.inlineData().data() != null) {
                        byte[] image = Base64.getDecoder().decode(part.inlineData().data());
                        String mime = part.inlineData().mimeType() != null
                                ? part.inlineData().mimeType() : "image/png";
                        return new AiImageResult(image, mime, "model-photo-gemini");
                    }
                }
                throw new AiException("Gemini nevrátil obrázek.");
            } catch (HttpClientErrorException exception) {
                throw new AiException("Gemini Images odmítl požadavek: "
                        + exception.getStatusCode(), exception);
            } catch (RestClientException | IllegalStateException | IllegalArgumentException exception) {
                last = exception;
                if (attempt < maxRetries) {
                    sleep(attempt);
                }
            }
        }
        throw new AiException("Gemini Images volání selhalo po " + (maxRetries + 1)
                + " pokusech.", last);
    }

    private void sleep(int attempt) {
        try {
            Thread.sleep((long) Math.pow(2, attempt) * 300L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private record GenerateResponse(List<Candidate> candidates) {
        private record Candidate(Content content) {
        }

        private record Content(List<Part> parts) {
        }
    }

    private record Part(InlineData inlineData) {
    }

    private record InlineData(String mimeType, String data) {
    }
}
