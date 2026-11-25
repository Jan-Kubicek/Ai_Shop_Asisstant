package cz.example.shopassistant.ai;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Veřejné rozhraní modulu AI.
 */
public interface AiApi {

    record GeneratedText(JsonNode json, int promptTokens, int completionTokens) {
    }

    GeneratedText generateProductTextDetailed(String description, String tone);

    GeneratedText generateSocialTextDetailed(String description, String tone);

    default JsonNode generateProductText(String description, String tone) {
        return generateProductTextDetailed(description, tone).json();
    }

    default JsonNode generateSocialText(String description, String tone) {
        return generateSocialTextDetailed(description, tone).json();
    }
}
