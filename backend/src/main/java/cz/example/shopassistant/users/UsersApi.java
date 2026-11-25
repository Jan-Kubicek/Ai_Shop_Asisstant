package cz.example.shopassistant.users;

import java.time.Instant;
import java.util.Optional;

/**
 * Veřejné rozhraní modulu Users.
 *
 * <p>Ostatní moduly smí používat pouze toto rozhraní. Implementace,
 * repozitáře a entity zůstávají skryté.</p>
 */
public interface UsersApi {

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(String id);

    long count();

    long countEnabled();

    UserAccount create(String email, String passwordHash, String role, int dailyLimit);

    void recordLogin(String id);

    /**
     * Interní pohled na účet včetně hashe hesla. Slouží výhradně modulu
     * {@code auth} k ověření přihlašovacích údajů, nikdy se neposílá klientovi.
     */
    record UserAccount(
            String id,
            String email,
            String passwordHash,
            String role,
            boolean enabled,
            int dailyLimit,
            Instant createdAt,
            Instant lastLoginAt) {
    }
}
