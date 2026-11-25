package cz.example.shopassistant.audit.application;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import cz.example.shopassistant.audit.AuditApi;
import cz.example.shopassistant.audit.domain.AuditEvent;
import cz.example.shopassistant.audit.domain.AuditEventRepository;

@Service
public class AuditService implements AuditApi {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(String userId, String action, String ip, Map<String, Object> details) {
        AuditEvent event = new AuditEvent();
        event.setUserId(userId);
        event.setAction(action);
        event.setIp(ip);
        event.setDetails(details);
        event.setTimestamp(Instant.now());
        repository.save(event);
    }

    @Override
    public Page<AuditEventView> list(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return repository.findAll(pageable).map(this::toView);
    }

    private AuditEventView toView(AuditEvent event) {
        return new AuditEventView(
                event.getId(),
                event.getUserId(),
                event.getAction(),
                event.getIp(),
                event.getDetails(),
                event.getTimestamp());
    }
}
