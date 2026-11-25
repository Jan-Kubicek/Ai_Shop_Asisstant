package cz.example.shopassistant.ai.application;

/**
 * Výsledek volání AI včetně spotřeby tokenů.
 */
public record AiTextResult(String text, int promptTokens, int completionTokens) {
}
