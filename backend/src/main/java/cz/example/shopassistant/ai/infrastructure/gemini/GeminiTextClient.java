package cz.example.shopassistant.ai.infrastructure.gemini;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import cz.example.shopassistant.ai.application.AiException;
import cz.example.shopassistant.ai.application.AiTextClient;
import cz.example.shopassistant.ai.application.AiTextResult;

/**
 * Klient pro Google Gemini (Generative Language API).
 */
@Component
@org.springframework.context.annotation.Profile("!fake-ai")
public class GeminiTextClient implements AiTextClient {

    private static final Logger LOG = LoggerFactory.getLogger(GeminiTextClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final RestClient restClient;
    private final String model;
    private final String apiKey;
    private final int maxRetries;
    private final double temperature;
    private final int maxTokens;

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiTextClient(
            @Value("${app.ai.gemini-api-key:}") String apiKey,
            @Value("${app.ai.gemini-model:gemini-3.5-flash}") String model,
            @Value("${app.ai.timeout-seconds:60}") int timeoutSeconds,
            @Value("${app.ai.max-retries:3}") int maxRetries,
            @Value("${app.ai.temperature:0.7}") double temperature,
            @Value("${app.ai.max-tokens:2048}") int maxTokens,
            @Value("${app.ai.gemini-base-url:https://generativelanguage.googleapis.com}") String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxRetries = Math.max(maxRetries, 0);
        this.temperature = temperature;
        this.maxTokens = maxTokens;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .build();
    }

    /** Konstruktor pro testy s vlastním základem URL. */
    public GeminiTextClient(String baseUrl, String apiKey, String model, int timeoutSeconds,
            int maxRetries, double temperature, int maxTokens) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxRetries = Math.max(maxRetries, 0);
        this.temperature = temperature;
        this.maxTokens = maxTokens;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public AiTextResult generateDetailed(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiException("Není nastaven GEMINI_API_KEY. Nastavte app.ai.gemini-api-key.");
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "maxOutputTokens", maxTokens));

        Exception last = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                // Gemini občas vrací JSON s Content-Type application/octet-stream,
                // proto načteme tělo jako String a parsing provedeme sami.
                String rawBody = restClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1beta/models/{model}:generateContent")
                                .queryParam("key", apiKey)
                                .build(model))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                if (rawBody == null || rawBody.isBlank()) {
                    throw new AiException("Gemini vrátil prázdnou odpověď.");
                }

                GeminiResponse response;
                try {
                    response = MAPPER.readValue(rawBody, GeminiResponse.class);
                } catch (JsonProcessingException exception) {
                    LOG.debug("Neplatná odpověď Gemini: {}", exception.getMessage());
                    throw new AiException("Odpověď Gemini není platný JSON.", exception);
                }

                String text = extractText(response);
                if (text == null || text.isBlank()) {
                    throw new AiException("Gemini vrátil prázdnou odpověď.");
                }
                GeminiResponse.UsageMetadata usage = response.usageMetadata();
                int promptTokens = usage != null ? usage.promptTokenCount() : 0;
                int completionTokens = usage != null ? usage.candidatesTokenCount() : 0;
                return new AiTextResult(text, promptTokens, completionTokens);
            } catch (HttpClientErrorException exception) {
                // Chyba na straně klienta (např. špatný model nebo klíč) – retry nepomůže.
                throw new AiException("Gemini odmítl požadavek: " + exception.getStatusCode(), exception);
            } catch (RestClientException | IllegalStateException exception) {
                last = exception;
                if (attempt < maxRetries) {
                    sleep(attempt);
                }
            }
        }
        throw new AiException("Gemini volání selhalo po " + (maxRetries + 1) + " pokusech.", last);
    }

    private void sleep(int attempt) {
        try {
            Thread.sleep((long) Math.pow(2, attempt) * 200L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return null;
        }
        GeminiResponse.Candidate candidate = response.candidates().get(0);
        if (candidate.content() == null || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            return null;
        }
        return candidate.content().parts().get(0).text();
    }

    record GeminiResponse(List<Candidate> candidates, UsageMetadata usageMetadata) {
        record Candidate(Content content) {
        }

        record Content(List<Part> parts) {
        }

        record Part(String text) {
        }

        record UsageMetadata(int promptTokenCount, int candidatesTokenCount) {
        }
    }
}
