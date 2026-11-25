package cz.example.shopassistant.export.web;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.example.shopassistant.common.security.AuthenticatedUser;
import cz.example.shopassistant.export.ExportApi;
import cz.example.shopassistant.export.ExportApi.ResultData;
import cz.example.shopassistant.media.MediaApi;
import cz.example.shopassistant.requests.RequestsApi;
import cz.example.shopassistant.requests.RequestsApi.RequestView;

@RestController
@RequestMapping("/api/v1/requests")
public class ExportController {

    private final ExportApi exportApi;
    private final RequestsApi requestsApi;
    private final MediaApi mediaApi;
    private final ObjectMapper objectMapper;

    public ExportController(ExportApi exportApi, RequestsApi requestsApi, MediaApi mediaApi,
            ObjectMapper objectMapper) {
        this.exportApi = exportApi;
        this.requestsApi = requestsApi;
        this.mediaApi = mediaApi;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{id}/result")
    public ResultView result(@PathVariable String id) {
        RequestView request = ownedRequest(id);
        ResultData data = exportApi.findByRequestId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Výsledek neexistuje."));
        return toView(data);
    }

    @PatchMapping("/{id}/result")
    public ResultView update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        RequestView request = ownedRequest(id);
        ResultData current = exportApi.findByRequestId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Výsledek neexistuje."));

        String webText = current.webTextJson();
        String socialText = current.socialTextJson();
        if (body.containsKey("webText")) {
            webText = toJson(body.get("webText"));
        }
        if (body.containsKey("socialText")) {
            socialText = toJson(body.get("socialText"));
        }

        String wooJson = exportApi.buildWooJson(id, webText, socialText,
                request.mediaId() != null ? request.mediaId() : "");
        exportApi.saveResult(id, webText, socialText, wooJson, current.modelImageUrl());

        return toView(exportApi.findByRequestId(id).orElseThrow());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String id) {
        RequestView request = ownedRequest(id);
        ResultData data = exportApi.findByRequestId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Výsledek neexistuje."));

        byte[] image = request.mediaId() != null
                ? mediaApi.readContent(request.mediaId(), currentUserId())
                : new byte[0];

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"request-" + id + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(exportApi.createZip(data, image));
    }

    private RequestView ownedRequest(String id) {
        RequestView request = requestsApi.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Požadavek neexistuje."));
        if (!request.userId().equals(currentUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Požadavek neexistuje.");
        }
        return request;
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Je potřeba se přihlásit.");
        }
        return principal.id();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Neplatná data výsledku.");
        }
    }

    private ResultView toView(ResultData data) {
        try {
            return new ResultView(
                    data.requestId(),
                    objectMapper.readTree(data.webTextJson()),
                    objectMapper.readTree(data.socialTextJson()),
                    data.modelImageUrl(),
                    objectMapper.readTree(data.wooJson()),
                    data.createdAt() != null ? data.createdAt().toString() : null);
        } catch (Exception exception) {
            throw new IllegalStateException("Výsledek se nepodařilo načíst.", exception);
        }
    }

    public record ResultView(
            String requestId,
            JsonNode webText,
            JsonNode socialText,
            String modelImageUrl,
            JsonNode wooJson,
            String createdAt) {
    }
}
