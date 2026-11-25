package cz.example.shopassistant.shopqueue.web;

import org.springframework.http.ResponseEntity;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.example.shopassistant.audit.AuditApi;
import cz.example.shopassistant.common.security.AuthenticatedUser;
import cz.example.shopassistant.requests.RequestsApi;
import cz.example.shopassistant.shopqueue.ShopQueueApi;

/**
 * Schválení výsledku review a zařazení do fronty e-shopu.
 */
@RestController
@RequestMapping("/api/v1/requests")
public class ApprovalController {

    private final RequestsApi requestsApi;
    private final ShopQueueApi shopQueueApi;
    private final AuditApi auditApi;

    public ApprovalController(RequestsApi requestsApi, ShopQueueApi shopQueueApi, AuditApi auditApi) {
        this.requestsApi = requestsApi;
        this.shopQueueApi = shopQueueApi;
        this.auditApi = auditApi;
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approve(@PathVariable String id) {
        requestsApi.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Požadavek neexistuje."));
        requestsApi.markQueuedForShop(id);
        shopQueueApi.enqueue(id);
        auditApi.record(currentUserId(), "REQUEST_APPROVED", null, Map.of("requestId", id));
        return ResponseEntity.noContent().build();
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser principal) {
            return principal.id();
        }
        return null;
    }
}
