package cz.example.shopassistant.users;

import java.time.Instant;
import java.util.Optional;

/**
 * Veřejné rozhraní modulu Users.
 */
public interface UsersApi {

    record UserView(
            String id,
            String email,
            String role,
            boolean enabled,
            int dailyLimit,
            Instant createdAt,
            Instant lastLoginAt) {
    }

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(String id);

    long count();

    long countEnabled();

    UserAccount create(String email, String passwordHash, String role, int dailyLimit);

    void recordLogin(String id);

    org.springframework.data.domain.Page<UserView> list(int page, int size);

    UserView createUser(String email, String passwordHash, String role, int dailyLimit);

    UserView updateUser(String id, String role, Boolean enabled, Integer dailyLimit);

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
