package cz.example.shopassistant.ai.web;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import cz.example.shopassistant.ai.AiApi;
import cz.example.shopassistant.ai.application.AiService;

/**
 * Ruční ověření kvality textů ve vývoji. V produkci není endpoint dostupný.
 */
@RestController
@RequestMapping("/api/v1/ai")
@Profile("dev")
public class AiController {

    private final AiService aiService;
    private final AiApi aiApi;

    public AiController(AiService aiService, AiApi aiApi) {
        this.aiService = aiService;
        this.aiApi = aiApi;
    }

    @PostMapping("/product-text")
    public JsonNode productText(@RequestBody ProductTextRequest request) {
        aiApi.checkRateLimit(currentUserId());
        return aiService.generateProductText(request.description(), request.tone());
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal()
                instanceof cz.example.shopassistant.common.security.AuthenticatedUser principal) {
            return principal.id();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Je potřeba se přihlásit.");
    }

    public record ProductTextRequest(String description, String tone) {
    }
}
