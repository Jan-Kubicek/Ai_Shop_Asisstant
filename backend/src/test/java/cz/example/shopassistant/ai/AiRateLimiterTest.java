package cz.example.shopassistant.ai;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import cz.example.shopassistant.ai.application.AiRateLimiter;

class AiRateLimiterTest {

    @Test
    void tretiVolaniVrati429() {
        AiRateLimiter limiter = new AiRateLimiter(2, 5);

        limiter.check("user-1");
        limiter.check("user-1");

        assertThatThrownBy(() -> limiter.check("user-1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> org.assertj.core.api.Assertions.assertThat(
                        ((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void ruzniUzivateleMajiVlastniOkno() {
        AiRateLimiter limiter = new AiRateLimiter(1, 5);

        limiter.check("user-1");
        limiter.check("user-2");
    }
}
