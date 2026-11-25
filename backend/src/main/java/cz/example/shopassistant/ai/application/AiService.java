package cz.example.shopassistant.ai.application;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Orchestrace AI: připraví prompt, zavolá klienta a zvaliduje výstup
 * proti JSON schématu.
 */
@Service
public class AiService {

    private static final String PRODUCT_PROMPT = "product-description.v1.txt";
    private static final String PRODUCT_SCHEMA = "schemas/product-text.schema.json";

    private final AiTextClient client;
    private final PromptLoader promptLoader;
    private final OutputValidator validator;

    public AiService(AiTextClient client, PromptLoader promptLoader, OutputValidator validator) {
        this.client = client;
        this.promptLoader = promptLoader;
        this.validator = validator;
    }

    public JsonNode generateProductText(String description, String tone) {
        String prompt = promptLoader.render(PRODUCT_PROMPT, Map.of(
                "userDescription", description,
                "tone", tone));
        String raw = client.generate(prompt);
        return validator.validate(raw, PRODUCT_SCHEMA);
    }
}
