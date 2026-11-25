package cz.example.shopassistant.ai.application;

/**
 * Výsledek generování obrázku.
 */
public record AiImageResult(byte[] image, String contentType, String description) {
}
