package cz.example.shopassistant.ai.infrastructure.openai;

import java.time.Duration;
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
import cz.example.shopassistant.ai.application.AiTextClient;
import cz.example.shopassistant.ai.application.AiTextResult;

/**
 * Klient pro OpenAI Chat Completions API.
 */
@Component
@ConditionalOnProperty(name = "app.ai.text-provider", havingValue = "openai")
public class OpenAiTextClient implements AiTextClient {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAiTextClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final int maxRetries;
    private final double temperature;
    private final int maxTokens;

    public OpenAiTextClient(
            @Value("${app.ai.openai-api-key:}") String apiKey,
            @Value("${app.ai.openai-model:gpt-4.1-mini}") String model,
            @Value("${app.ai.timeout-seconds:60}") int timeoutSeconds,
            @Value("${app.ai.max-retries:3}") int maxRetries,
            @Value("${app.ai.temperature:0.7}") double temperature,
            @Value("${app.ai.max-tokens:2048}") int maxTokens,
            @Value("${app.ai.openai-base-url:https://api.openai.com/v1}") String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxRetries = Math.max(maxRetries, 0);
        this.temperature = temperature;
        this.maxTokens = maxTokens;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().requestFactory(factory).baseUrl(baseUrl).build();
    }

    /** Konstruktor pro testy s vlastní base URL. */
    public OpenAiTextClient(String baseUrl, String apiKey, String model, int timeoutSeconds,
            int maxRetries, double temperature, int maxTokens) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxRetries = Math.max(maxRetries, 0);
        this.temperature = temperature;
        this.maxTokens = maxTokens;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().requestFactory(factory).baseUrl(baseUrl).build();
    }

    @Override
    public AiTextResult generateDetailed(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiException("Není nastaven OPENAI_API_KEY. Nastavte app.ai.openai-api-key.");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "Vrať POUZE platný JSON podle pokynů uživatele. Žádné markdown bloky."),
                        Map.of("role", "user", "content", prompt)),
                "temperature", temperature,
                "max_tokens", maxTokens,
                "response_format", Map.of("type", "json_object"));

        Exception last = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String raw = restClient.post()
                        .uri("/chat/completions")
                        .header("Authorization", "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                if (raw == null || raw.isBlank()) {
                    throw new AiException("OpenAI vrátil prázdnou odpověď.");
                }

                ChatResponse response;
                try {
                    response = MAPPER.readValue(raw, ChatResponse.class);
                } catch (JsonProcessingException exception) {
                    LOG.debug("Neplatná odpověď OpenAI: {}", exception.getMessage());
                    throw new AiException("Odpověď OpenAI není platný JSON.", exception);
                }

                if (response.choices() == null || response.choices().isEmpty()
                        || response.choices().get(0).message() == null) {
                    throw new AiException("OpenAI nevrátil žádný text.");
                }
                String text = response.choices().get(0).message().content();
                ChatResponse.Usage usage = response.usage();
                return new AiTextResult(
                        text,
                        usage != null ? usage.promptTokens() : 0,
                        usage != null ? usage.completionTokens() : 0);
            } catch (HttpClientErrorException exception) {
                throw new AiException("OpenAI odmítl požadavek: " + exception.getStatusCode(), exception);
            } catch (RestClientException | IllegalStateException exception) {
                last = exception;
                if (attempt < maxRetries) {
                    sleep(attempt);
                }
            }
        }
        throw new AiException("OpenAI volání selhalo po " + (maxRetries + 1) + " pokusech.", last);
    }

    private void sleep(int attempt) {
        try {
            Thread.sleep((long) Math.pow(2, attempt) * 200L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    record ChatResponse(List<Choice> choices, Usage usage) {
        record Choice(Message message) {
        }

        record Message(String content) {
        }

        record Usage(int promptTokens, int completionTokens) {
        }
    }
}
