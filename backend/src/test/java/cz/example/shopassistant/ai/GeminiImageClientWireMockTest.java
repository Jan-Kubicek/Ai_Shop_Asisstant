package cz.example.shopassistant.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import cz.example.shopassistant.ai.application.AiImageResult;
import cz.example.shopassistant.ai.infrastructure.gemini.GeminiImageClient;

class GeminiImageClientWireMockTest {

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
    void imagePartVratiDekodovanyObrazek() {
        String base64 = Base64.getEncoder().encodeToString(new byte[] {9, 8, 7});
        wireMock.stubFor(WireMock.post(WireMock.urlPathMatching("/v1beta/models/.*:generateContent"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"candidates":[{"content":{"parts":[
                                  {"text":"ok"},
                                  {"inlineData":{"mimeType":"image/png","data":"%s"}}
                                ]}}]}
                                """.formatted(base64))));

        GeminiImageClient client = new GeminiImageClient(
                wireMock.baseUrl(), "test-key", "gemini-2.5-flash-image", 5, 1);

        AiImageResult result = client.generate("dítě v čepici");

        assertThat(result.image()).containsExactly(9, 8, 7);
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.description()).isEqualTo("model-photo-gemini");
    }
}
