package cz.example.shopassistant.media.web;

import org.springframework.http.HttpHeaders;
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

import cz.example.shopassistant.auth.domain.UserPrincipal;
import cz.example.shopassistant.media.MediaApi;
import cz.example.shopassistant.media.application.MediaService;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaService service;

    public MediaController(MediaService service) {
        this.service = service;
    }

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaApi.MediaFile> upload(@RequestParam("file") MultipartFile file) {
        String userId = currentUserId();
        MediaApi.MediaFile stored = service.store(userId, file);
        return ResponseEntity.status(201).body(stored);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable String id) {
        String userId = currentUserId();
        byte[] content = service.readContent(id, userId);
        String type = service.contentType(id, userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(content);
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Je potřeba se přihlásit.");
        }
        return principal.id();
    }
}
