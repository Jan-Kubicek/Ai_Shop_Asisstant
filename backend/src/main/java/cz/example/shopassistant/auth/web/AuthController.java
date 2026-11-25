package cz.example.shopassistant.auth.web;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cz.example.shopassistant.auth.application.AuthService;
import cz.example.shopassistant.auth.application.AuthService.AuthResult;
import cz.example.shopassistant.auth.application.AuthService.UserView;
import cz.example.shopassistant.auth.application.JwtService;
import cz.example.shopassistant.auth.domain.UserPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final JwtService jwtService;
    private final boolean cookieSecure;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            @Value("${app.jwt.cookie-secure:false}") boolean cookieSecure) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResult result = authService.login(request.email(), request.password());
        addRefreshCookie(response, result.refreshToken(), jwtService.refreshTtl());
        return ResponseEntity.ok(TokenResponse.from(result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chybí refresh token.");
        }

        AuthResult result = authService.refresh(refreshToken);
        addRefreshCookie(response, result.refreshToken(), jwtService.refreshTtl());
        return ResponseEntity.ok(TokenResponse.from(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        addRefreshCookie(response, "", Duration.ZERO);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserView me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nejste přihlášen.");
        }
        return authService.currentUser(principal.id());
    }

    private void addRefreshCookie(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public record LoginRequest(
            @NotBlank(message = "Zadejte e-mail.") @Email(message = "Neplatný e-mail.") String email,
            @NotBlank(message = "Zadejte heslo.") String password) {
    }

    public record TokenResponse(String accessToken, long expiresIn, UserView user) {

        static TokenResponse from(AuthResult result) {
            return new TokenResponse(result.accessToken(), result.expiresIn(), result.user());
        }
    }
}
