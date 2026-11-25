package cz.example.shopassistant.auth.domain;

import cz.example.shopassistant.common.security.AuthenticatedUser;

/**
 * Přihlášený uživatel uložený v {@code SecurityContext}.
 */
public record UserPrincipal(String id, String email, String role) implements AuthenticatedUser {
}
