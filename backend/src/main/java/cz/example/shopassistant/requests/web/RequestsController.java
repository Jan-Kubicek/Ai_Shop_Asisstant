package cz.example.shopassistant.requests.web;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import cz.example.shopassistant.common.security.AuthenticatedUser;
import cz.example.shopassistant.requests.application.RequestsService;
import cz.example.shopassistant.requests.application.RequestsService.CreatedRequestView;

@RestController
@RequestMapping("/api/v1/requests")
public class RequestsController {

    private final RequestsService service;

    public RequestsController(RequestsService service) {
        this.service = service;
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

    @GetMapping("/{id}")
    public CreatedRequestView get(@PathVariable String id) {
        String userId = currentUserId();
        return service.get(userId, id);
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
