package cz.example.shopassistant.pipeline.application;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import cz.example.shopassistant.ai.AiApi;
import cz.example.shopassistant.ai.AiApi.GeneratedText;
import cz.example.shopassistant.export.ExportApi;
import cz.example.shopassistant.pipeline.domain.AiCall;
import cz.example.shopassistant.pipeline.domain.AiCallRepository;
import cz.example.shopassistant.requests.RequestsApi;
import cz.example.shopassistant.requests.RequestsApi.RequestView;

/**
 * Worker zpracovává nové požadavky na pozadí: vygeneruje texty a připraví
 * export, pak požadavek přesune do stavu REVIEW.
 */
@Component
@ConditionalOnProperty(name = "app.pipeline.enabled", havingValue = "true", matchIfMissing = true)
public class PipelineWorker {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineWorker.class);
    private static final String TONE = "přátelský, ale věcný";

    private final RequestsApi requestsApi;
    private final AiApi aiApi;
    private final ExportApi exportApi;
    private final AiCallRepository aiCallRepository;
    private final String provider;

    public PipelineWorker(
            RequestsApi requestsApi,
            AiApi aiApi,
            ExportApi exportApi,
            AiCallRepository aiCallRepository,
            @Value("${app.ai.text-provider:gemini}") String provider) {
        this.requestsApi = requestsApi;
        this.aiApi = aiApi;
        this.exportApi = exportApi;
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
                JsonNode webText = web.json();
                JsonNode socialText = social.json();
                String mediaId = request.mediaId() != null ? request.mediaId() : "";
                String wooJson = exportApi.buildWooJson(
                        request.id(), webText.toString(), socialText.toString(), mediaId);

                exportApi.saveResult(
                        request.id(),
                        webText.toString(),
                        socialText.toString(),
                        wooJson,
                        "/api/v1/media/" + mediaId);
                requestsApi.markReview(request.id());
                recordCall(request.id(), "SUCCESS", started,
                        web.promptTokens() + social.promptTokens(),
                        web.completionTokens() + social.completionTokens());
                LOG.info("Požadavek {} zpracován → REVIEW", request.id());
            } catch (Exception exception) {
                requestsApi.markFailed(request.id(), "Zpracování selhalo: " + exception.getMessage());
                recordCall(request.id(), "FAILED", started, 0, 0);
                LOG.error("Zpracování požadavku {} selhalo", request.id(), exception);
            }
        }
    }

    private void recordCall(String requestId, String status, long started,
            int promptTokens, int completionTokens) {
        AiCall call = new AiCall();
        call.setRequestId(requestId);
        call.setProvider(provider);
        call.setOperation("product-text+social-text");
        call.setLatencyMs(System.currentTimeMillis() - started);
        call.setPromptTokens(promptTokens);
        call.setCompletionTokens(completionTokens);
        call.setStatus(status);
        call.setTimestamp(Instant.now());
        aiCallRepository.save(call);
    }
}
