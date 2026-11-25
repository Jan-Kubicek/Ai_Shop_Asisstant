package cz.example.shopassistant.ai.application;

/**
 * Klient pro generování textu umělou inteligencí.
 *
 * <p>Rozhraní je záměrně jednoduché, aby se dalo snadno nahradit
 * falešnou implementací v testech a vývoji.</p>
 */
public interface AiTextClient {

    /**
     * Vygeneruje text na základě zadaného promptu.
     *
     * @param prompt kompletní prompt (system + uživatelský vstup)
     * @return vygenerovaný text (typicky JSON)
     * @throws AiException při chybě komunikace nebo neplatné odpovědi
     */
    String generate(String prompt);
}
