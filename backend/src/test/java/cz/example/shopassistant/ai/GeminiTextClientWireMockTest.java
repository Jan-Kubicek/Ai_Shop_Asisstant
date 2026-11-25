package cz.example.shopassistant.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import cz.example.shopassistant.ai.application.AiException;
import cz.example.shopassistant.ai.infrastructure.gemini.GeminiTextClient;

class GeminiTextClientWireMockTest {

    private WireMockServer wireMock;

    @BeforeEach
    void start() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterEach
    void stop() {
        wireMock.stop();
    }

    @Test
    void uspesnaOdpovedVratiText() {
        wireMock.stubFor(post(urlPathMatching("/v1beta/models/.*:generateContent"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"candidates":[{"content":{"parts":[{"text":"Hello world"}]}}],
                                 "usageMetadata":{"promptTokenCount":11,"candidatesTokenCount":7}}
                                """)));

        GeminiTextClient client = new GeminiTextClient(
                wireMock.baseUrl(), "test-key", "gemini-test", 5, 1, 0.7, 100);

        assertThat(client.generateDetailed("prompt").text()).isEqualTo("Hello world");
        assertThat(client.generateDetailed("prompt").promptTokens()).isEqualTo(11);
        assertThat(client.generateDetailed("prompt").completionTokens()).isEqualTo(7);
    }

    @Test
    void rozbitýJsonVyhodíAiException() {
        wireMock.stubFor(post(urlPathMatching("/v1beta/models/.*:generateContent"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{not json}")));

        GeminiTextClient client = new GeminiTextClient(
                wireMock.baseUrl(), "test-key", "gemini-test", 5, 1, 0.7, 100);

        assertThatThrownBy(() -> client.generate("prompt"))
                .isInstanceOf(AiException.class);
    }

    @Test
    void retryPri500NakonecUspěje() {
        wireMock.stubFor(post(urlPathMatching("/v1beta/models/.*:generateContent"))
                .inScenario("retry")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second"));

        wireMock.stubFor(post(urlPathMatching("/v1beta/models/.*:generateContent"))
                .inScenario("retry")
                .whenScenarioStateIs("second")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"candidates":[{"content":{"parts":[{"text":"ok"}]}}]}
                                """)));

        GeminiTextClient client = new GeminiTextClient(
                wireMock.baseUrl(), "test-key", "gemini-test", 5, 1, 0.7, 100);

        assertThat(client.generate("prompt")).isEqualTo("ok");
    }

    @Test
    void timeoutVyhodíAiException() {
        wireMock.stubFor(post(urlPathMatching("/v1beta/models/.*:generateContent"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(2000)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"candidates":[{"content":{"parts":[{"text":"late"}]}}]}
                                """)));

        GeminiTextClient client = new GeminiTextClient(
                wireMock.baseUrl(), "test-key", "gemini-test", 1, 0, 0.7, 100);

        assertThatThrownBy(() -> client.generate("prompt"))
                .isInstanceOf(AiException.class);
    }
}
