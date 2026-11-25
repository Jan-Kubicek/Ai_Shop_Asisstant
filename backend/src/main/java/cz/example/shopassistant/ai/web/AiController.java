package cz.example.shopassistant.ai.web;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import cz.example.shopassistant.ai.application.AiService;

/**
 * Ruční ověření kvality textů ve vývoji. V produkci není endpoint dostupný.
 */
@RestController
@RequestMapping("/api/v1/ai")
@Profile("dev")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/product-text")
    public JsonNode productText(@RequestBody ProductTextRequest request) {
        return aiService.generateProductText(request.description(), request.tone());
    }

    public record ProductTextRequest(String description, String tone) {
    }
}
