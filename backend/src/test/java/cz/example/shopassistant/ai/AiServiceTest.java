package cz.example.shopassistant.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.example.shopassistant.ai.application.AiRateLimiter;
import cz.example.shopassistant.ai.application.AiService;
import cz.example.shopassistant.ai.application.OutputValidator;
import cz.example.shopassistant.ai.application.PromptLoader;
import cz.example.shopassistant.ai.infrastructure.fake.FakeAiTextClient;

class AiServiceTest {

    @Test
    void fakeClientVratiValidniJsonBezSpotrebyTokenu() {
        AiService service = new AiService(
                new FakeAiTextClient(""),
                new PromptLoader(),
                new OutputValidator(new ObjectMapper()),
                org.mockito.Mockito.mock(AiRateLimiter.class));

        JsonNode result = service.generateProductText("šedá vlněná čepice", "neutrální");

        assertThat(result.get("title").asText()).isNotBlank();
        assertThat(result.get("keywords").size()).isGreaterThanOrEqualTo(3);
    }
}
