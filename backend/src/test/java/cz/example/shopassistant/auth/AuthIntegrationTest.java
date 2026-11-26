package cz.example.shopassistant.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import cz.example.shopassistant.auth.application.LoginRateLimiter;
import cz.example.shopassistant.common.error.ApiErrorBody;
import cz.example.shopassistant.users.UsersApi;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "app.auth.login-rate-limit.attempts=3",
            "app.auth.login-rate-limit.window-minutes=5"
        })
@Testcontainers(disabledWithoutDocker = true)
class AuthIntegrationTest {

    private static final String EMAIL = "admin@example.com";
    private static final String PASSWORD = "Sup3rSecret!";

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:4.4");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO::getConnectionString);
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UsersApi users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection("users");
        rateLimiter.resetAll();
        users.create(EMAIL, passwordEncoder.encode(PASSWORD), "ADMIN", 100);
    }

    @Test
    void uspesnePrihlaseniVratiTokeny() {
        ResponseEntity<TokenResponse> response = rest.postForEntity(
                "/api/v1/auth/login",
                credentials(EMAIL, PASSWORD),
                TokenResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();
        assertThat(response.getBody().expiresIn()).isEqualTo(900);
        assertThat(response.getBody().user().email()).isEqualTo(EMAIL);
        assertThat(response.getBody().user().role()).isEqualTo("ADMIN");

        String cookie = response.getHeaders().getFirst("Set-Cookie");
        assertThat(cookie).isNotNull();
        assertThat(cookie).contains("refresh_token").contains("HttpOnly");
    }

    @Test
    void neuspesnePrihlaseniVrati401() {
        ResponseEntity<ApiErrorBody> response = rest.postForEntity(
                "/api/v1/auth/login",
                credentials(EMAIL, "spatne-heslo"),
                ApiErrorBody.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void nosqlInjectionPayloadJeOdmítnut() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(
                "{\"email\":{\"$ne\":null},\"password\":\"x\"}",
                headers);

        ResponseEntity<ApiErrorBody> response = rest.postForEntity(
                "/api/v1/auth/login",
                entity,
                ApiErrorBody.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void rateLimitOdmítnaOpakovanePokusy() {
        for (int i = 0; i < 3; i++) {
            ResponseEntity<ApiErrorBody> attempt = rest.postForEntity(
                    "/api/v1/auth/login",
                    credentials(EMAIL, "spatne-heslo"),
                    ApiErrorBody.class);
            assertThat(attempt.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<ApiErrorBody> blocked = rest.postForEntity(
                "/api/v1/auth/login",
                credentials(EMAIL, "spatne-heslo"),
                ApiErrorBody.class);

        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(blocked.getBody()).isNotNull();
        assertThat(blocked.getBody().code()).isEqualTo("RATE_LIMITED");
    }

    private HttpEntity<Map<String, String>> credentials(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(Map.of("email", email, "password", password), headers);
    }

    private record TokenResponse(String accessToken, long expiresIn, UserResponse user) {
    }

    private record UserResponse(String id, String email, String role, boolean enabled, int dailyLimit) {
    }
}
