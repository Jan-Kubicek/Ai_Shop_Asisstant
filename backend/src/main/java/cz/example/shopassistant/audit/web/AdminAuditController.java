package cz.example.shopassistant.audit.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cz.example.shopassistant.audit.AuditApi;
import cz.example.shopassistant.audit.AuditApi.AuditEventView;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

    private final AuditApi auditApi;

    public AdminAuditController(AuditApi auditApi) {
        this.auditApi = auditApi;
    }

    @GetMapping("/audit")
    public PageResponse<AuditEventView> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String account,
            @RequestParam(defaultValue = "true") boolean newestFirst) {
        var result = auditApi.list(page, size, action, account, newestFirst);
        return new PageResponse<>(result.getContent(), result.getTotalElements(), page, size);
    }

    public record PageResponse<T>(List<T> items, long total, int page, int size) {
    }
}
