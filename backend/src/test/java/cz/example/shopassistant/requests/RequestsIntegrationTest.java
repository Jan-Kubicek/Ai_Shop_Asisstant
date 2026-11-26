package cz.example.shopassistant.requests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import cz.example.shopassistant.common.error.ApiErrorBody;
import cz.example.shopassistant.users.UsersApi;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class RequestsIntegrationTest {

    private static final String USER_A = "a@example.com";
    private static final String USER_B = "b@example.com";
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

    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection("requests");
        mongoTemplate.dropCollection("media");
        mongoTemplate.dropCollection("users");

        users.create(USER_A, passwordEncoder.encode(PASSWORD), "USER", 20);
        users.create(USER_B, passwordEncoder.encode(PASSWORD), "USER", 20);

        tokenA = login(USER_A);
        tokenB = login(USER_B);
    }

    @Test
    void vytvoreniPozadavkuVrati201() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/requests",
                multipartRequest("test.png", "image/png", pngBytes(), "Testovací produkt.", tokenA),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("id");
        assertThat(response.getBody().get("status")).isEqualTo("CREATED");
    }

    @Test
    void phpSouborPřejmenovanýNaJpgSeOdmítne() {
        byte[] php = "<?php echo 1; ?>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ResponseEntity<ApiErrorBody> response = rest.postForEntity(
                "/api/v1/requests",
                multipartRequest("malware.jpg", "image/jpeg", php, "Test", tokenA),
                ApiErrorBody.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void uzivatelANevidiPozadavekUzivateleB() {
        // Vytvoříme požadavek jako uživatel A.
        ResponseEntity<Map> created = rest.postForEntity(
                "/api/v1/requests",
                multipartRequest("test.png", "image/png", pngBytes(), "Tajný požadavek A.", tokenA),
                Map.class);
        String requestId = (String) created.getBody().get("id");

        // Uživatel B se pokusí o detail.
        HttpHeaders headers = authHeaders(tokenB);
        ResponseEntity<ApiErrorBody> response = rest.exchange(
                "/api/v1/requests/" + requestId,
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                ApiErrorBody.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String login(String email) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(
                Map.of("email", email, "password", PASSWORD), headers);
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/login", entity, Map.class);
        return (String) response.getBody().get("accessToken");
    }

    private HttpEntity<MultiValueMap<String, Object>> multipartRequest(
            String filename, String mimeType, byte[] content, String description, String token) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("photo", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        body.add("description", description);
        body.add("options", "{\"generateWebText\":true,\"generateSocialPost\":true,\"generateModelPhoto\":false}");

        return new HttpEntity<>(body, authHeaders(token));
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private static byte[] pngBytes() {
        return java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
    }
}
