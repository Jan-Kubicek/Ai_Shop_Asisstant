package cz.example.shopassistant.requests.application;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.example.shopassistant.ai.AiImageApi;
import cz.example.shopassistant.media.MediaApi;
import cz.example.shopassistant.users.UsersApi;
import cz.example.shopassistant.requests.RequestsApi;
import cz.example.shopassistant.requests.RequestStatusChangedEvent;
import cz.example.shopassistant.requests.RequestsApi.RequestView;
import cz.example.shopassistant.requests.domain.Request;
import cz.example.shopassistant.requests.domain.RequestRepository;
import cz.example.shopassistant.requests.domain.RequestStatus;

@Service
public class RequestsService implements RequestsApi {

    private final RequestRepository repository;
    private final MediaApi mediaApi;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher events;
    private final AiImageApi imageApi;
    private final UsersApi usersApi;
    private final int modelPhotoLimit;

    public RequestsService(
            RequestRepository repository,
            MediaApi mediaApi,
            ObjectMapper objectMapper,
            ApplicationEventPublisher events,
            AiImageApi imageApi,
            UsersApi usersApi,
            @Value("${app.limits.model-photos-per-day:3}") int modelPhotoLimit) {
        this.repository = repository;
        this.mediaApi = mediaApi;
        this.objectMapper = objectMapper;
        this.events = events;
        this.imageApi = imageApi;
        this.usersApi = usersApi;
        this.modelPhotoLimit = Math.max(modelPhotoLimit, 0);
    }

    public CreatedRequestView create(String userId, MultipartFile photo, String description, String optionsJson) {
        if (description == null || description.trim().length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Popis je příliš krátký.");
        }

        Request.RequestOptions options = parseOptions(optionsJson);
        try {
            imageApi.validateModelDescription(options.getModelDescription());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }

        enforceDailyLimits(userId, options);

        MediaApi.MediaFile media = mediaApi.store(userId, photo);

        Request request = new Request();
        request.setUserId(userId);
        request.setDescription(description.trim());
        request.setOptions(options);
        request.setSizePrices(buildSizePrices(options));
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

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public long countCreatedSince(Instant since) {
        return repository.countByCreatedAtAfter(since);
    }

    @Override
    public long countFailed() {
        return repository.countByStatus(RequestStatus.FAILED);
    }

    private void enforceDailyLimits(String userId, Request.RequestOptions options) {
        cz.example.shopassistant.users.UsersApi.UserAccount account = usersApi.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Uživatel neexistuje."));

        Instant since = java.time.LocalDate.now()
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        java.util.List<Request> today = repository.findByUserIdAndCreatedAtAfter(userId, since);

        int requestLimit = Math.max(account.dailyLimit(), 0);
        if (today.size() >= requestLimit) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Denní limit požadavků (" + requestLimit + ") byl dosažen.");
        }

        if (options.isGenerateModelPhoto()) {
            long photos = today.stream()
                    .filter(item -> item.getOptions() != null && item.getOptions().isGenerateModelPhoto())
                    .count();
            if (photos >= modelPhotoLimit) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Denní limit modelových fotek (" + modelPhotoLimit + ") byl dosažen.");
            }
        }
    }

    @Override
    public void delete(String id, String userId) {
        Request request = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Požadavek neexistuje."));
        repository.delete(request);
    }

    @Override
    public java.util.List<cz.example.shopassistant.requests.RequestsApi.DailyStat> dailyStats(int days) {
        java.time.Instant since = java.time.LocalDate.now()
                .minusDays(Math.max(days, 1) - 1L)
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant();

        java.util.Map<java.time.LocalDate, long[]> grouped = new java.util.LinkedHashMap<>();
        for (Request request : repository.findByCreatedAtAfter(since)) {
            if (request.getCreatedAt() == null) {
                continue;
            }
            java.time.LocalDate day = request.getCreatedAt()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            long[] counts = grouped.computeIfAbsent(day, ignored -> new long[2]);
            counts[0]++;
            if (request.getStatus() == RequestStatus.FAILED) {
                counts[1]++;
            }
        }

        java.util.List<cz.example.shopassistant.requests.RequestsApi.DailyStat> result =
                new java.util.ArrayList<>();
        for (java.time.LocalDate day = since.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                !day.isAfter(java.time.LocalDate.now());
                day = day.plusDays(1)) {
            long[] counts = grouped.getOrDefault(day, new long[2]);
            result.add(new cz.example.shopassistant.requests.RequestsApi.DailyStat(
                    day, counts[0], counts[1]));
        }
        return result;
    }

    private void updateStatus(String id, RequestStatus status, String errorMessage) {
        Request request = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Požadavek neexistuje."));
        request.setStatus(status);
        request.setErrorMessage(errorMessage);
        if (status == RequestStatus.QUEUED_FOR_SHOP || status == RequestStatus.FAILED) {
            request.setFinishedAt(Instant.now());
        }
        Request saved = repository.save(request);
        events.publishEvent(new RequestStatusChangedEvent(
                saved.getId(),
                saved.getStatus().name(),
                saved.getErrorMessage(),
                saved.getDescription(),
                saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : null,
                saved.getFinishedAt() != null ? saved.getFinishedAt().toString() : null));
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
                toApiSizePrices(request.getSizePrices()),
                request.getCreatedAt(),
                request.getFinishedAt());
    }

    private static java.util.List<cz.example.shopassistant.requests.RequestsApi.SizePrice> toApiSizePrices(
            java.util.List<Request.SizePrice> items) {
        if (items == null) {
            return java.util.List.of();
        }
        return items.stream()
                .map(item -> new cz.example.shopassistant.requests.RequestsApi.SizePrice(
                        item.getSize(), item.getPrice()))
                .toList();
    }

    private String optionsToJson(Request.RequestOptions options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private static java.util.List<Request.SizePrice> buildSizePrices(Request.RequestOptions options) {
        Integer from = parseSize(options.getSizeFrom());
        Integer to = parseSize(options.getSizeTo());
        java.math.BigDecimal base = options.getBasePrice();
        java.math.BigDecimal percent = options.getPriceIncreasePercent();

        if (from == null || to == null || base == null || percent == null || from > to) {
            return java.util.List.of();
        }

        java.util.List<Request.SizePrice> result = new java.util.ArrayList<>();
        java.math.BigDecimal multiplier = java.math.BigDecimal.ONE
                .add(percent.divide(java.math.BigDecimal.valueOf(100), 4,
                        java.math.RoundingMode.HALF_UP));
        java.math.BigDecimal price = base;

        for (int size = from; size <= to; size += 6) {
            java.math.BigDecimal rounded = price.setScale(0, java.math.RoundingMode.HALF_UP);
            Request.SizePrice item = new Request.SizePrice();
            item.setSize(size);
            item.setPrice(rounded);
            result.add(item);
            price = rounded.multiply(multiplier);
        }
        return result;
    }

    private static Integer parseSize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Optional<java.math.BigDecimal> parsePrice(Object value) {
        if (value == null || value.toString().isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new java.math.BigDecimal(value.toString()));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
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
            if (map.get("modelDescription") != null) {
                options.setModelDescription(map.get("modelDescription").toString());
            }
            if (map.get("sizeFrom") != null) {
                options.setSizeFrom(map.get("sizeFrom").toString());
            }
            if (map.get("sizeTo") != null) {
                options.setSizeTo(map.get("sizeTo").toString());
            }
            parsePrice(map.get("basePrice")).ifPresent(options::setBasePrice);
            parsePrice(map.get("priceIncreasePercent")).ifPresent(options::setPriceIncreasePercent);
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
                toApiSizePrices(request.getSizePrices()),
                request.getStatus().name(),
                request.getErrorMessage(),
                request.getCreatedAt() != null ? request.getCreatedAt().toString() : null,
                request.getFinishedAt() != null ? request.getFinishedAt().toString() : null);
    }

    public record CreatedRequestView(
            String id,
            String description,
            Request.RequestOptions options,
            java.util.List<cz.example.shopassistant.requests.RequestsApi.SizePrice> sizePrices,
            String status,
            String errorMessage,
            String createdAt,
            String finishedAt) {
    }
}
