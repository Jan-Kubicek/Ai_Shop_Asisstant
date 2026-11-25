package cz.example.shopassistant.ai.application;

import org.springframework.stereotype.Service;

import cz.example.shopassistant.ai.AiImageApi;

/**
 * Adaptér interních komponent image skillu na veřejné rozhraní modulu AI.
 */
@Service
public class AiImageService implements AiImageApi {

    private final AiImageClient client;
    private final ModelPromptValidator validator;

    public AiImageService(AiImageClient client, ModelPromptValidator validator) {
        this.client = client;
        this.validator = validator;
    }

    @Override
    public void validateModelDescription(String text) {
        try {
            validator.validate(text);
        } catch (AiException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
    }

    @Override
    public GeneratedImage generate(String prompt) {
        AiImageResult result = client.generate(prompt);
        return new GeneratedImage(result.image(), result.contentType(), result.description());
    }
}
