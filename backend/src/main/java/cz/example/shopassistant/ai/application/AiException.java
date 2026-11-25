package cz.example.shopassistant.ai.application;

/**
 * Výjimka při komunikaci s AI nebo při zpracování její odpovědi.
 */
public class AiException extends RuntimeException {

    public AiException(String message) {
        super(message);
    }

    public AiException(String message, Throwable cause) {
        super(message, cause);
    }
}
