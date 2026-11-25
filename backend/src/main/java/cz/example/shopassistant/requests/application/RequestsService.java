package cz.example.shopassistant.requests.application;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.example.shopassistant.media.MediaApi;
import cz.example.shopassistant.requests.RequestsApi;
import cz.example.shopassistant.requests.RequestsApi.RequestView;
import cz.example.shopassistant.requests.domain.Request;
import cz.example.shopassistant.requests.domain.RequestRepository;
import cz.example.shopassistant.requests.domain.RequestStatus;

@Service
public class RequestsService implements RequestsApi {

    private final RequestRepository repository;
    private final MediaApi mediaApi;
    private final ObjectMapper objectMapper;

    public RequestsService(RequestRepository repository, MediaApi mediaApi, ObjectMapper objectMapper) {
        this.repository = repository;
        this.mediaApi = mediaApi;
        this.objectMapper = objectMapper;
    }

    public CreatedRequestView create(String userId, MultipartFile photo, String description, String optionsJson) {
        if (description == null || description.trim().length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Popis je příliš krátký.");
        }

        MediaApi.MediaFile media = mediaApi.store(userId, photo);

        Request request = new Request();
        request.setUserId(userId);
        request.setDescription(description.trim());
        request.setOptions(parseOptions(optionsJson));
        request.setStatus(RequestStatus.CREATED);
        request.setMediaId(media.id());
        request.setCreatedAt(Instant.now());

        return toView(repository.save(request));
    }

    public Page<CreatedRequestView> list(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toView);
    }

    public CreatedRequestView get(String userId, String id) {
        Request request = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Požadavek neexistuje."));
        return toView(request);
    }

    @Override
    public Optional<RequestView> findById(String id) {
        return repository.findById(id).map(this::toApiView);
    }

    @Override
    public List<RequestView> findPending(int limit) {
        return repository.findTop20ByStatusOrderByCreatedAtAsc(RequestStatus.CREATED)
                .stream()
                .limit(Math.max(limit, 0))
                .map(this::toApiView)
                .toList();
    }

    @Override
    public void markProcessing(String id) {
        updateStatus(id, RequestStatus.PROCESSING, null);
    }

    @Override
    public void markReview(String id) {
        updateStatus(id, RequestStatus.REVIEW, null);
    }

    @Override
    public void markFailed(String id, String message) {
        updateStatus(id, RequestStatus.FAILED, message);
    }

    @Override
    public void markQueuedForShop(String id) {
        updateStatus(id, RequestStatus.QUEUED_FOR_SHOP, null);
    }

    public void approve(String id) {
        updateStatus(id, RequestStatus.QUEUED_FOR_SHOP, null);
    }

    private void updateStatus(String id, RequestStatus status, String errorMessage) {
        Request request = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Požadavek neexistuje."));
        request.setStatus(status);
        request.setErrorMessage(errorMessage);
        if (status == RequestStatus.QUEUED_FOR_SHOP || status == RequestStatus.FAILED) {
            request.setFinishedAt(Instant.now());
        }
        repository.save(request);
    }

    private RequestView toApiView(Request request) {
        return new RequestView(
                request.getId(),
                request.getUserId(),
                request.getDescription(),
                request.getOptions() != null ? optionsToJson(request.getOptions()) : "{}",
                request.getStatus().name(),
                request.getErrorMessage(),
                request.getMediaId(),
                request.getCreatedAt(),
                request.getFinishedAt());
    }

    private String optionsToJson(Request.RequestOptions options) {
        return "{\"generateWebText\":" + options.isGenerateWebText()
                + ",\"generateSocialPost\":" + options.isGenerateSocialPost()
                + ",\"generateModelPhoto\":" + options.isGenerateModelPhoto() + "}";
    }

    private Request.RequestOptions parseOptions(String optionsJson) {
        Request.RequestOptions options = new Request.RequestOptions();
        options.setGenerateWebText(true);
        options.setGenerateSocialPost(true);
        options.setGenerateModelPhoto(false);

        if (optionsJson == null || optionsJson.isBlank()) {
            return options;
        }
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = objectMapper.readValue(optionsJson, java.util.Map.class);
            if (map.containsKey("generateWebText")) {
                options.setGenerateWebText(Boolean.TRUE.equals(map.get("generateWebText")));
            }
            if (map.containsKey("generateSocialPost")) {
                options.setGenerateSocialPost(Boolean.TRUE.equals(map.get("generateSocialPost")));
            }
            if (map.containsKey("generateModelPhoto")) {
                options.setGenerateModelPhoto(Boolean.TRUE.equals(map.get("generateModelPhoto")));
            }
        } catch (Exception ignored) {
            // Neplatný JSON – použijeme výchozí volby.
        }
        return options;
    }

    private CreatedRequestView toView(Request request) {
        return new CreatedRequestView(
                request.getId(),
                request.getDescription(),
                request.getOptions() != null ? request.getOptions() : new Request.RequestOptions(),
                request.getStatus().name(),
                request.getErrorMessage(),
                request.getCreatedAt() != null ? request.getCreatedAt().toString() : null,
                request.getFinishedAt() != null ? request.getFinishedAt().toString() : null);
    }

    public record CreatedRequestView(
            String id,
            String description,
            Request.RequestOptions options,
            String status,
            String errorMessage,
            String createdAt,
            String finishedAt) {
    }
}
