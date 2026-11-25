package cz.example.shopassistant.ai.application;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import cz.example.shopassistant.ai.AiApi;
import cz.example.shopassistant.ai.AiApi.GeneratedText;

/**
 * Orchestrace AI: připraví prompt, zavolá klienta a zvaliduje výstup
 * proti JSON schématu.
 */
@Service
public class AiService implements AiApi {

    private static final String PRODUCT_PROMPT = "product-description.v1.txt";
    private static final String PRODUCT_SCHEMA = "schemas/product-text.schema.json";
    private static final String SOCIAL_PROMPT = "social-post.v1.txt";
    private static final String SOCIAL_SCHEMA = "schemas/social-text.schema.json";
    private static final String MARKETING_PROMPT = "marketing-text.v1.txt";
    private static final String MARKETING_SCHEMA = "schemas/marketing-text.schema.json";

    private final AiTextClient client;
    private final PromptLoader promptLoader;
    private final OutputValidator validator;

    public AiService(AiTextClient client, PromptLoader promptLoader, OutputValidator validator) {
        this.client = client;
        this.promptLoader = promptLoader;
        this.validator = validator;
    }

    @Override
    public GeneratedText generateProductTextDetailed(String description, String tone) {
        String prompt = promptLoader.render(PRODUCT_PROMPT, Map.of(
                "userDescription", description,
                "tone", tone));
        return generateAndValidate(prompt, PRODUCT_SCHEMA);
    }

    @Override
    public GeneratedText generateSocialTextDetailed(String description, String tone) {
        String prompt = promptLoader.render(SOCIAL_PROMPT, Map.of(
                "userDescription", description,
                "tone", tone));
        return generateAndValidate(prompt, SOCIAL_SCHEMA);
    }

    @Override
    public GeneratedText generateMarketingTextDetailed(String description, String tone) {
        String prompt = promptLoader.render(MARKETING_PROMPT, Map.of(
                "userDescription", description,
                "tone", tone));
        return generateAndValidate(prompt, MARKETING_SCHEMA);
    }

    private GeneratedText generateAndValidate(String prompt, String schema) {
        AiTextResult result = client.generateDetailed(prompt);
        JsonNode json = validator.validate(result.text(), schema);
        return new GeneratedText(json, result.promptTokens(), result.completionTokens());
    }
}
