package cz.example.shopassistant.shopqueue.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cz.example.shopassistant.shopqueue.ShopQueueApi;

@RestController
@RequestMapping("/api/v1/admin/shop-queue")
@PreAuthorize("hasRole('ADMIN')")
public class ShopQueueController {

    private final ShopQueueApi shopQueueApi;

    public ShopQueueController(ShopQueueApi shopQueueApi) {
        this.shopQueueApi = shopQueueApi;
    }

    @GetMapping
    public PageResponse<ShopQueueApi.QueueItem> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = shopQueueApi.list(page, size);
        return new PageResponse<>(result.getContent(), result.getTotalElements(), page, size);
    }

    @PostMapping("/{id}/mark-imported")
    public ResponseEntity<Void> markImported(@PathVariable String id) {
        shopQueueApi.markImported(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable String id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"woo-" + id + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(shopQueueApi.exportJson(id));
    }

    public record PageResponse<T>(java.util.List<T> items, long total, int page, int size) {
    }
}
