package cz.example.shopassistant.ai;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Veřejné rozhraní modulu AI.
 */
public interface AiApi {

    JsonNode generateProductText(String description, String tone);

    JsonNode generateSocialText(String description, String tone);
}
