package cz.example.shopassistant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    private static final ApplicationModules MODULES =
            ApplicationModules.of(ShopAssistantApplication.class);

    @Test
    void modulyDodrzujiHranice() {
        MODULES.verify();
    }

    @Test
    void aplikaceMaOcekavaneModuly() {
        assertThat(MODULES.stream().map(module -> module.getName()))
                .contains("auth", "users", "requests", "media", "ai",
                        "pipeline", "export", "shopqueue", "audit", "common");
    }
}
