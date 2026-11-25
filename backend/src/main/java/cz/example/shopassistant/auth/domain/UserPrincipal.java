package cz.example.shopassistant.auth.domain;

/**
 * Přihlášený uživatel uložený v {@code SecurityContext}.
 */
public record UserPrincipal(String id, String email, String role) {
}
