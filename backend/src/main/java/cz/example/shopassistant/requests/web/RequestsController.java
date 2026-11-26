package cz.example.shopassistant.requests.web;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cz.example.shopassistant.common.security.AuthenticatedUser;
import cz.example.shopassistant.requests.application.RequestsService;
import cz.example.shopassistant.requests.infrastructure.RequestEventPublisher;
import cz.example.shopassistant.requests.application.RequestsService.CreatedRequestView;

@RestController
@RequestMapping("/api/v1/requests")
public class RequestsController {

    private final RequestsService service;
    private final RequestEventPublisher eventPublisher;

    public RequestsController(RequestsService service, RequestEventPublisher eventPublisher) {
        this.service = service;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreatedRequestView> create(
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("description") String description,
            @RequestParam(value = "options", required = false) String options) {

        String userId = currentUserId();
        CreatedRequestView view = service.create(userId, photo, description, options);
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @GetMapping
    public PageResponse<CreatedRequestView> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String userId = currentUserId();
        Page<CreatedRequestView> result = service.list(userId, page, size);
        return new PageResponse<>(result.getContent(), result.getTotalElements(), page, size);
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<CreatedRequestView> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<CreatedRequestView> result = service.listAll(page, size);
        return new PageResponse<>(result.getContent(), result.getTotalElements(), page, size);
    }

    @GetMapping("/{id}")
    public CreatedRequestView get(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser principal && principal.isAdmin()) {
            return service.getAny(id);
        }
        return service.get(currentUserId(), id);
    }

    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CreatedRequestView current = (auth != null && auth.getPrincipal() instanceof AuthenticatedUser principal && principal.isAdmin())
                ? service.getAny(id)
                : service.get(currentUserId(), id);
        return eventPublisher.subscribe(new cz.example.shopassistant.requests.RequestStatusChangedEvent(
                current.id(),
                current.status(),
                current.errorMessage(),
                current.description(),
                current.createdAt(),
                current.finishedAt()));
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Je potřeba se přihlásit.");
        }
        return principal.id();
    }

    public record PageResponse<T>(java.util.List<T> items, long total, int page, int size) {
    }
}
