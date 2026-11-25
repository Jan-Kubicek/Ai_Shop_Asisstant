package cz.example.shopassistant.requests.application;

import java.time.Instant;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.example.shopassistant.media.MediaApi;
import cz.example.shopassistant.requests.domain.Request;
import cz.example.shopassistant.requests.domain.RequestRepository;
import cz.example.shopassistant.requests.domain.RequestStatus;

@Service
public class RequestsService {

    private final RequestRepository repository;
    private final MediaApi mediaApi;
    private final ObjectMapper objectMapper;

    public RequestsService(RequestRepository repository, MediaApi mediaApi, ObjectMapper objectMapper) {
        this.repository = repository;
        this.mediaApi = mediaApi;
        this.objectMapper = objectMapper;
    }

    public RequestView create(String userId, MultipartFile photo, String description, String optionsJson) {
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

    public Page<RequestView> list(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toView);
    }

    public RequestView get(String userId, String id) {
        Request request = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Požadavek neexistuje."));
        return toView(request);
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

    private RequestView toView(Request request) {
        return new RequestView(
                request.getId(),
                request.getDescription(),
                request.getOptions() != null ? request.getOptions() : new Request.RequestOptions(),
                request.getStatus().name(),
                request.getErrorMessage(),
                request.getCreatedAt() != null ? request.getCreatedAt().toString() : null,
                request.getFinishedAt() != null ? request.getFinishedAt().toString() : null);
    }

    public record RequestView(
            String id,
            String description,
            Request.RequestOptions options,
            String status,
            String errorMessage,
            String createdAt,
            String finishedAt) {
    }
}
