package cz.example.shopassistant.common.security;

/**
 * Přihlášený uživatel dostupný v {@code SecurityContext}. Společné rozhraní
 * pro všechny moduly, aby se nemusely záviset na interních třídách modulu
 * {@code auth}.
 */
public interface AuthenticatedUser {
    String id();
    String email();
    String role();
}
