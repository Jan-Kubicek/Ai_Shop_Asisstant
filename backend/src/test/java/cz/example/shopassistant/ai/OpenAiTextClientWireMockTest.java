package cz.example.shopassistant.ai;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import cz.example.shopassistant.ai.application.AiException;
import cz.example.shopassistant.ai.infrastructure.openai.OpenAiTextClient;

class OpenAiTextClientWireMockTest {

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
    void uspesnaOdpovedVratiTextATokeny() {
        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"choices":[{"message":{"content":"{\\"title\\":\\"Čepice\\"}"}}],
                                 "usage":{"prompt_tokens":12,"completion_tokens":9}}
                                """)));

        OpenAiTextClient client = new OpenAiTextClient(
                wireMock.baseUrl(), "test-key", "gpt-4.1-mini", 5, 1, 0.7, 100);

        var result = client.generateDetailed("prompt");

        assertThat(result.text()).contains("Čepice");
        assertThat(result.promptTokens()).isEqualTo(12);
        assertThat(result.completionTokens()).isEqualTo(9);
    }

    @Test
    void rozbitýJsonVyhodíAiException() {
        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{not json}")));

        OpenAiTextClient client = new OpenAiTextClient(
                wireMock.baseUrl(), "test-key", "gpt-4.1-mini", 5, 1, 0.7, 100);

        assertThatThrownBy(() -> client.generate("prompt")).isInstanceOf(AiException.class);
    }

    @Test
    void retryPri500NakonecUspěje() {
        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .inScenario("retry")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second"));

        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .inScenario("retry")
                .whenScenarioStateIs("second")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"choices":[{"message":{"content":"ok"}}],
                                 "usage":{"prompt_tokens":1,"completion_tokens":1}}
                                """)));

        OpenAiTextClient client = new OpenAiTextClient(
                wireMock.baseUrl(), "test-key", "gpt-4.1-mini", 5, 1, 0.7, 100);

        assertThat(client.generate("prompt")).isEqualTo("ok");
    }
}
