package cz.example.shopassistant.ai.infrastructure.fake;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cz.example.shopassistant.ai.application.AiTextClient;
import cz.example.shopassistant.ai.application.AiTextResult;

/**
 * Falešná implementace pro vývoj a testy. Vrací pevný JSON bez volání API.
 */
@Component
@ConditionalOnProperty(name = "app.ai.text-provider", havingValue = "fake")
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

    private static final String MARKETING_RESPONSE = """
            {
              "title": "Ručně pletená čepice, šedá",
              "shortDescription": "Teplá čepice z čisté vlny, ručně pletená v Česku.",
              "longDescription": "Tato čepice je upletena ručně z kvalitní vlny. Je teplá, příjemná a hodí se do chladného počasí. Velikost je univerzální.",
              "keywords": ["čepice", "vlna", "ruční práce", "zima", "doplněk"],
              "usp": "Ruční pletení z čisté vlny a česká výroba.",
              "audience": "Děti a dospělí, kteří hledají teplou čepici na zimu.",
              "tone": "Přátelský, ale věcný.",
              "marketingAngle": "Zdůraznit ruční výrobu, český původ a hřejivost vlny."
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
        } else if (prompt != null && prompt.contains("MARKETING")) {
            text = MARKETING_RESPONSE;
        } else if (prompt != null && prompt.contains("Instagram")) {
            text = SOCIAL_RESPONSE;
        } else {
            text = FIXED_RESPONSE;
        }
        return new AiTextResult(text, 0, 0);
    }
}
