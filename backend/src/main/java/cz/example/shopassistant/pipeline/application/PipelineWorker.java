package cz.example.shopassistant.pipeline.application;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.example.shopassistant.ai.AiApi;
import cz.example.shopassistant.ai.AiApi.GeneratedText;
import cz.example.shopassistant.ai.application.AiImageClient;
import cz.example.shopassistant.ai.application.AiImageResult;
import cz.example.shopassistant.ai.application.ModelPromptValidator;
import cz.example.shopassistant.export.ExportApi;
import cz.example.shopassistant.media.MediaApi;
import cz.example.shopassistant.pipeline.domain.AiCall;
import cz.example.shopassistant.pipeline.domain.AiCallRepository;
import cz.example.shopassistant.requests.RequestsApi;
import cz.example.shopassistant.requests.RequestsApi.RequestView;

/**
 * Worker zpracovává nové požadavky na pozadí: vygeneruje texty, marketing,
 * volitelně modelovou fotku a připraví export, pak přesune do REVIEW.
 */
@Component
@ConditionalOnProperty(name = "app.pipeline.enabled", havingValue = "true", matchIfMissing = true)
public class PipelineWorker {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineWorker.class);
    private static final String TONE = "přátelský, ale věcný";

    private final RequestsApi requestsApi;
    private final AiApi aiApi;
    private final AiImageClient imageClient;
    private final ExportApi exportApi;
    private final MediaApi mediaApi;
    private final ModelPromptValidator modelPromptValidator;
    private final ObjectMapper objectMapper;
    private final AiCallRepository aiCallRepository;
    private final String provider;

    public PipelineWorker(
            RequestsApi requestsApi,
            AiApi aiApi,
            AiImageClient imageClient,
            ExportApi exportApi,
            MediaApi mediaApi,
            ModelPromptValidator modelPromptValidator,
            ObjectMapper objectMapper,
            AiCallRepository aiCallRepository,
            @Value("${app.ai.text-provider:gemini}") String provider) {
        this.requestsApi = requestsApi;
        this.aiApi = aiApi;
        this.imageClient = imageClient;
        this.exportApi = exportApi;
        this.mediaApi = mediaApi;
        this.modelPromptValidator = modelPromptValidator;
        this.objectMapper = objectMapper;
        this.aiCallRepository = aiCallRepository;
        this.provider = provider;
    }

    @Scheduled(fixedDelayString = "${app.pipeline.poll-ms:2000}")
    public void processPending() {
        for (RequestView request : requestsApi.findPending(5)) {
            long started = System.currentTimeMillis();
            requestsApi.markProcessing(request.id());
            try {
                GeneratedText web = aiApi.generateProductTextDetailed(request.description(), TONE);
                GeneratedText social = aiApi.generateSocialTextDetailed(request.description(), TONE);
                GeneratedText marketing = aiApi.generateMarketingTextDetailed(request.description(), TONE);

                JsonNode webText = web.json();
                JsonNode socialText = social.json();
                JsonNode marketingText = marketing.json();
                String mediaId = request.mediaId() != null ? request.mediaId() : "";
                String modelImageUrl = "/api/v1/media/" + mediaId;

                int imagePromptTokens = 0;
                int imageCompletionTokens = 0;
                if (wantsModelPhoto(request)) {
                    String modelDescription = option(request, "modelDescription");
                    modelPromptValidator.validate(modelDescription);
                    String prompt = "Vytvoř marketingovou fotku produktu. "
                            + "Popis modelu: " + modelDescription;
                    AiImageResult image = imageClient.generate(prompt);
                    cz.example.shopassistant.media.MediaApi.MediaFile stored = mediaApi.storeBytes(
                            request.userId(), image.image(), image.description() + ".png",
                            image.contentType());
                    modelImageUrl = "/api/v1/media/" + stored.id();
                    // Image skill je teď fake – tokeny se nepočítají.
                }

                String wooJson = exportApi.buildWooJson(
                        request.id(), webText.toString(), socialText.toString(), mediaId,
                        request.sizePrices());

                exportApi.saveResult(
                        request.id(),
                        webText.toString(),
                        socialText.toString(),
                        wooJson,
                        marketingText.toString(),
                        modelImageUrl);
                requestsApi.markReview(request.id());
                recordCall(request.id(), "SUCCESS", started,
                        web.promptTokens() + social.promptTokens() + marketing.promptTokens()
                                + imagePromptTokens,
                        web.completionTokens() + social.completionTokens()
                                + marketing.completionTokens() + imageCompletionTokens);
                LOG.info("Požadavek {} zpracován → REVIEW", request.id());
            } catch (Exception exception) {
                requestsApi.markFailed(request.id(), "Zpracování selhalo: " + exception.getMessage());
                recordCall(request.id(), "FAILED", started, 0, 0);
                LOG.error("Zpracování požadavku {} selhalo", request.id(), exception);
            }
        }
    }

    private boolean wantsModelPhoto(RequestView request) {
        return Boolean.parseBoolean(option(request, "generateModelPhoto"));
    }

    private String option(RequestView request, String key) {
        try {
            JsonNode options = objectMapper.readTree(request.options() == null ? "{}" : request.options());
            JsonNode value = options.get(key);
            return value == null || value.isNull() ? "" : value.asText();
        } catch (Exception exception) {
            return "";
        }
    }

    private void recordCall(String requestId, String status, long started,
            int promptTokens, int completionTokens) {
        AiCall call = new AiCall();
        call.setRequestId(requestId);
        call.setProvider(provider);
        call.setOperation("product-text+social-text+marketing");
        call.setLatencyMs(System.currentTimeMillis() - started);
        call.setPromptTokens(promptTokens);
        call.setCompletionTokens(completionTokens);
        call.setStatus(status);
        call.setTimestamp(Instant.now());
        aiCallRepository.save(call);
    }
}
