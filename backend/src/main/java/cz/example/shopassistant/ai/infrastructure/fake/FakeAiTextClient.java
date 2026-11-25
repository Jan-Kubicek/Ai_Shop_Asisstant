package cz.example.shopassistant.ai.infrastructure.fake;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import cz.example.shopassistant.ai.application.AiTextClient;
import cz.example.shopassistant.ai.application.AiTextResult;

/**
 * Falešná implementace pro vývoj a testy. Vrací pevný JSON bez volání API.
 */
@Component
@Profile("fake-ai")
public class FakeAiTextClient implements AiTextClient {

    private static final String FIXED_RESPONSE = """
            {
              "title": "Ručně pletená čepice, šedá",
              "shortDescription": "Teplá čepice z čisté vlny, ručně pletená v Česku.",
              "longDescription": "Tato čepice je upletena ručně z kvalitní vlny. Je teplá, příjemná a hodí se do chladného počasí. Velikost je univerzální.",
              "keywords": ["čepice", "vlna", "ruční práce", "zima", "doplněk"]
            }
            """;

    private static final String SOCIAL_RESPONSE = """
            {
              "instagram": {
                "caption": "Ručně pletená čepice z čisté vlny v szedé barvě. Teplá, pohodlná a vyrobená v Česku.",
                "hashtags": ["#čepice", "#vlna", "#handmade", "#zima", "#českývýrobek"]
              },
              "facebook": {
                "text": "Představujeme ručně pletenou čepici z čisté vlny. Univerzální velikost, česká výroba a teplo pro chladné dny."
              }
            }
            """;

    private final String configuredResponse;

    public FakeAiTextClient(@Value("${app.ai.fake-response:}") String configuredResponse) {
        this.configuredResponse = configuredResponse;
    }

    @Override
    public AiTextResult generateDetailed(String prompt) {
        String text;
        if (configuredResponse != null && !configuredResponse.isBlank()) {
            text = configuredResponse;
        } else if (prompt != null && prompt.contains("Instagram")) {
            text = SOCIAL_RESPONSE;
        } else {
            text = FIXED_RESPONSE;
        }
        return new AiTextResult(text, 0, 0);
    }
}
