package cz.example.shopassistant.shopqueue.application;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cz.example.shopassistant.export.ExportApi;
import cz.example.shopassistant.shopqueue.ShopQueueApi;
import cz.example.shopassistant.shopqueue.domain.ShopQueueItem;
import cz.example.shopassistant.shopqueue.domain.ShopQueueRepository;

@Service
public class ShopQueueService implements ShopQueueApi {

    private final ShopQueueRepository repository;
    private final ExportApi exportApi;

    public ShopQueueService(ShopQueueRepository repository, ExportApi exportApi) {
        this.repository = repository;
        this.exportApi = exportApi;
    }

    @Override
    public void enqueue(String requestId) {
        if (repository.findByRequestId(requestId).isPresent()) {
            return;
        }
        ShopQueueItem item = new ShopQueueItem();
        item.setRequestId(requestId);
        item.setStatus("QUEUED");
        item.setCreatedAt(Instant.now());
        repository.save(item);
    }

    @Override
    public java.util.Optional<QueueItem> findByRequestId(String requestId) {
        return repository.findByRequestId(requestId).map(this::toApi);
    }

    @Override
    public Page<QueueItem> list(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return repository.findAll(pageable).map(this::toApi);
    }

    @Override
    public void markImported(String id) {
        ShopQueueItem item = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Položka neexistuje."));
        item.setStatus("IMPORTED");
        item.setProcessedAt(Instant.now());
        repository.save(item);
    }

    @Override
    public byte[] exportJson(String id) {
        ShopQueueItem item = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Položka neexistuje."));
        String wooJson = exportApi.findByRequestId(item.getRequestId())
                .map(ExportApi.ResultData::wooJson)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Výsledek neexistuje."));
        return wooJson.getBytes(StandardCharsets.UTF_8);
    }

    private QueueItem toApi(ShopQueueItem item) {
        return new QueueItem(
                item.getId(),
                item.getRequestId(),
                item.getStatus(),
                item.getNote(),
                item.getCreatedAt(),
                item.getProcessedAt());
    }
}
