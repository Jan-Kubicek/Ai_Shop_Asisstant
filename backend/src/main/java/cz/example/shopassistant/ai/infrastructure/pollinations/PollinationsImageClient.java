package cz.example.shopassistant.ai.infrastructure.pollinations;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import cz.example.shopassistant.ai.application.AiException;
import cz.example.shopassistant.ai.application.AiImageClient;
import cz.example.shopassistant.ai.application.AiImageResult;

/**
 * Bezplatný image provider přes veřejné Pollinations.ai API (bez API klíče).
 */
@Component
@ConditionalOnProperty(name = "app.ai.image-provider", havingValue = "pollinations")
public class PollinationsImageClient implements AiImageClient {

    private static final String SAFETY_PREFIX =
            "Product marketing photo for a Czech e-shop with children's clothing. "
                    + "No nudity, no sexualization, child fully clothed, neutral background, "
                    + "no logos, no text. ";

    private final RestClient restClient;
    private final int maxRetries;

    @org.springframework.beans.factory.annotation.Autowired
    public PollinationsImageClient(
            @Value("${app.ai.pollinations-base-url:https://image.pollinations.ai}") String baseUrl,
            @Value("${app.ai.timeout-seconds:60}") int timeoutSeconds,
            @Value("${app.ai.max-retries:3}") int maxRetries) {
        this.maxRetries = Math.max(maxRetries, 0);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().requestFactory(factory).baseUrl(baseUrl).build();
    }

    /** Konstruktor pro testy s vlastní base URL. */
    public PollinationsImageClient(String baseUrl, int timeoutSeconds) {
        this.maxRetries = 1;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().requestFactory(factory).baseUrl(baseUrl).build();
    }

    @Override
    public AiImageResult generate(String prompt) {
        String fullPrompt = SAFETY_PREFIX + (prompt == null ? "" : prompt);
        String encoded = URLEncoder.encode(fullPrompt, StandardCharsets.UTF_8);
        String uri = "/prompt/" + encoded + "?width=1024&height=1024&nologo=true";

        Exception last = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                ResponseEntity<byte[]> response = restClient.get()
                        .uri(uri)
                        .retrieve()
                        .toEntity(byte[].class);

                byte[] image = response.getBody();
                if (image == null || image.length == 0) {
                    throw new AiException("Pollinations nevrátil obrázek.");
                }
                String contentType = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
                return new AiImageResult(image,
                        contentType != null && contentType.startsWith("image/")
                                ? contentType : "image/jpeg",
                        "model-photo-pollinations");
            } catch (HttpClientErrorException exception) {
                throw new AiException("Pollinations odmítl požadavek: "
                        + exception.getStatusCode(), exception);
            } catch (RestClientException | IllegalStateException exception) {
                last = exception;
                if (attempt < maxRetries) {
                    sleep(attempt);
                }
            }
        }
        throw new AiException("Pollinations volání selhalo po " + (maxRetries + 1)
                + " pokusech.", last);
    }

    private void sleep(int attempt) {
        try {
            Thread.sleep((long) Math.pow(2, attempt) * 300L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
