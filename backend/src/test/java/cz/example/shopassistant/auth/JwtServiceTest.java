package cz.example.shopassistant.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import cz.example.shopassistant.auth.application.JwtService;
import cz.example.shopassistant.users.UsersApi.UserAccount;

class JwtServiceTest {

    private static final String SECRET =
            "testovaci-tajemstvi-pro-jwt-ktere-ma-alespon-32-bajtu";

    private final JwtService jwtService = new JwtService(SECRET, 15, 14);

    private final UserAccount user = new UserAccount(
            "user-1",
            "admin@example.com",
            "hash",
            "ADMIN",
            true,
            100,
            Instant.parse("2026-01-01T10:00:00Z"),
            null);

    @Test
    void accessTokenObsahujeUzivatele() {
        String token = jwtService.createAccessToken(user);

        assertThat(jwtService.parseAccessToken(token))
                .hasValueSatisfying(claims -> {
                    assertThat(claims.userId()).isEqualTo("user-1");
                    assertThat(claims.email()).isEqualTo("admin@example.com");
                    assertThat(claims.role()).isEqualTo("ADMIN");
                });
    }

    @Test
    void refreshTokenNejdePouzitJakoAccessToken() {
        String refreshToken = jwtService.createRefreshToken(user);

        assertThat(jwtService.parseAccessToken(refreshToken)).isEmpty();
        assertThat(jwtService.parseRefreshToken(refreshToken)).isPresent();
    }

    @Test
    void kratkySecretJeOdmítnut() {
        assertThatThrownBy(() -> new JwtService("kratky", 15, 14))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
