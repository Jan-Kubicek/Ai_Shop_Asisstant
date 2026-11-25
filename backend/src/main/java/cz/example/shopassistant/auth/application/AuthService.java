package cz.example.shopassistant.auth.application;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cz.example.shopassistant.users.UsersApi;
import cz.example.shopassistant.users.UsersApi.UserAccount;

/**
 * Přihlášení, obnovení tokenu a načtení přihlášeného uživatele.
 */
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Neplatný e-mail nebo heslo.";

    private final UsersApi users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsersApi users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResult login(String email, String rawPassword) {
        UserAccount user = users.findByEmail(email)
                .filter(UserAccount::enabled)
                .orElseThrow(AuthService::unauthorized);

        if (!passwordEncoder.matches(rawPassword, user.passwordHash())) {
            throw unauthorized();
        }

        users.recordLogin(user.id());
        return issueTokens(user);
    }

    public AuthResult refresh(String refreshToken) {
        JwtService.JwtClaims claims = jwtService.parseRefreshToken(refreshToken)
                .orElseThrow(AuthService::unauthorized);

        UserAccount user = users.findById(claims.userId())
                .filter(UserAccount::enabled)
                .orElseThrow(AuthService::unauthorized);

        return issueTokens(user);
    }

    public UserView currentUser(String userId) {
        UserAccount user = users.findById(userId)
                .filter(UserAccount::enabled)
                .orElseThrow(AuthService::unauthorized);
        return UserView.from(user);
    }

    private AuthResult issueTokens(UserAccount user) {
        return new AuthResult(
                jwtService.createAccessToken(user),
                jwtService.createRefreshToken(user),
                jwtService.accessTtl().toSeconds(),
                UserView.from(user));
    }

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
    }

    public record AuthResult(String accessToken, String refreshToken, long expiresIn, UserView user) {
    }

    public record UserView(
            String id,
            String email,
            String role,
            boolean enabled,
            int dailyLimit,
            Instant createdAt,
            Instant lastLoginAt) {

        static UserView from(UserAccount user) {
            return new UserView(
                    user.id(),
                    user.email(),
                    user.role(),
                    user.enabled(),
                    user.dailyLimit(),
                    user.createdAt(),
                    user.lastLoginAt());
        }
    }
}
