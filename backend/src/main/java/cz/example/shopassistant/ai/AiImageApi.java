package cz.example.shopassistant.ai;

/**
 * Veřejné rozhraní image skillu (modelová fotka) a its safety kontroly.
 */
public interface AiImageApi {

    record GeneratedImage(byte[] image, String contentType, String description) {
    }

    void validateModelDescription(String text);

    GeneratedImage generate(String prompt);
}
