package cz.example.shopassistant.pipeline.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.example.shopassistant.ai.AiApi;
import cz.example.shopassistant.ai.AiApi.GeneratedText;
import cz.example.shopassistant.ai.AiImageApi;
import cz.example.shopassistant.ai.AiImageApi.GeneratedImage;
import cz.example.shopassistant.common.security.AuthenticatedUser;
import cz.example.shopassistant.export.ExportApi;
import cz.example.shopassistant.export.ExportApi.ResultData;
import cz.example.shopassistant.media.MediaApi;
import cz.example.shopassistant.pipeline.domain.AiCall;
import cz.example.shopassistant.pipeline.domain.AiCallRepository;
import cz.example.shopassistant.requests.RequestsApi;
import cz.example.shopassistant.requests.RequestsApi.RequestView;

/**
 * Opakované vygenerování vybrané části výsledku.
 */
@RestController
@RequestMapping("/api/v1/requests")
public class PipelineController {

    private static final String TONE = "přátelský, ale věcný";

    private final RequestsApi requestsApi;
    private final AiApi aiApi;
    private final AiImageApi imageApi;
    private final ExportApi exportApi;
    private final MediaApi mediaApi;
    private final AiCallRepository aiCallRepository;
    private final ObjectMapper objectMapper;

    public PipelineController(
            RequestsApi requestsApi,
            AiApi aiApi,
            AiImageApi imageApi,
            ExportApi exportApi,
            MediaApi mediaApi,
            AiCallRepository aiCallRepository,
            ObjectMapper objectMapper) {
        this.requestsApi = requestsApi;
        this.aiApi = aiApi;
        this.imageApi = imageApi;
        this.exportApi = exportApi;
        this.mediaApi = mediaApi;
        this.aiCallRepository = aiCallRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/{id}/regenerate")
    public org.springframework.http.ResponseEntity<Void> regenerate(
            @PathVariable String id,
            @RequestBody RegenerateRequest body) {

        RequestView request = requestsApi.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Požadavek neexistuje."));
        if (!request.userId().equals(currentUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Požadavek neexistuje.");
        }

        aiApi.checkRateLimit(currentUserId());

        ResultData current = exportApi.findByRequestId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Výsledek neexistuje."));

        String part = body.part() == null ? "" : body.part().trim();
        String webText = current.webTextJson();
        String socialText = current.socialTextJson();
        String marketingText = current.marketingTextJson();
        String productImageUrl = current.productImageUrl();
        String modelImageUrl = current.modelImageUrl();
        String modelMediaId = current.modelMediaId();
        int promptTokens = 0;
        int completionTokens = 0;

        switch (part) {
            case "webText" -> {
                GeneratedText generated = aiApi.generateProductTextDetailed(request.description(), TONE);
                webText = generated.json().toString();
                promptTokens += generated.promptTokens();
                completionTokens += generated.completionTokens();
            }
            case "socialText" -> {
                GeneratedText generated = aiApi.generateSocialTextDetailed(request.description(), TONE);
                socialText = generated.json().toString();
                promptTokens += generated.promptTokens();
                completionTokens += generated.completionTokens();
            }
            case "marketingText" -> {
                GeneratedText generated = aiApi.generateMarketingTextDetailed(request.description(), TONE);
                marketingText = generated.json().toString();
                promptTokens += generated.promptTokens();
                completionTokens += generated.completionTokens();
            }
            case "modelImage" -> {
                String description = option(request, "modelDescription");
                imageApi.validateModelDescription(description);
                GeneratedImage image = imageApi.generate(
                        "Marketingová fotka produktu. Popis modelu: " + description);
                MediaApi.MediaFile stored = mediaApi.storeBytes(
                        request.userId(), image.image(), image.description() + ".png",
                        image.contentType());
                modelImageUrl = "/api/v1/media/" + stored.id();
                modelMediaId = stored.id();
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Podporované části: webText, socialText, marketingText, modelImage.");
        }

        String wooJson = exportApi.buildWooJson(id, webText, socialText,
                request.mediaId() != null ? request.mediaId() : "", request.sizePrices());
        exportApi.saveResult(id, webText, socialText, wooJson, marketingText,
                productImageUrl, modelImageUrl, modelMediaId);

        AiCall call = new AiCall();
        call.setRequestId(id);
        call.setProvider(option(request, "provider"));
        call.setOperation("regenerate:" + part);
        call.setLatencyMs(0);
        call.setPromptTokens(promptTokens);
        call.setCompletionTokens(completionTokens);
        call.setStatus("SUCCESS");
        call.setTimestamp(java.time.Instant.now());
        aiCallRepository.save(call);

        return org.springframework.http.ResponseEntity.noContent().build();
    }

    private String option(RequestView request, String key) {
        try {
            JsonNode options = objectMapper.readTree(
                    request.options() == null ? "{}" : request.options());
            JsonNode value = options.get(key);
            return value == null || value.isNull() ? "" : value.asText();
        } catch (Exception exception) {
            return "";
        }
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser principal) {
            return principal.id();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Je potřeba se přihlásit.");
    }

    public record RegenerateRequest(String part) {
    }
}
