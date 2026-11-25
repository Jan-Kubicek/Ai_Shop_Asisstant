package cz.example.shopassistant.ai.application;

/**
 * Klient pro generování marketingové fotky produktu na modelovi.
 */
public interface AiImageClient {

    AiImageResult generate(String prompt);
}
