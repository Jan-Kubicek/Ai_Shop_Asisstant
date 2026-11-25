package cz.example.shopassistant.audit.application;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import cz.example.shopassistant.audit.AuditApi;
import cz.example.shopassistant.audit.domain.AuditEvent;
import cz.example.shopassistant.audit.domain.AuditEventRepository;

@Service
public class AuditService implements AuditApi {

    private final AuditEventRepository repository;
    private final MongoTemplate mongoTemplate;

    public AuditService(AuditEventRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
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
    public Page<AuditEventView> list(int page, int size, String action, String account,
            boolean newestFirst) {
        Query query = new Query();
        if (action != null && !action.isBlank()) {
            query.addCriteria(Criteria.where("action").regex(action, "i"));
        }
        if (account != null && !account.isBlank()) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("userId").regex(account, "i"),
                    Criteria.where("details.email").regex(account, "i")));
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(newestFirst ? Sort.Direction.DESC : Sort.Direction.ASC, "timestamp"));

        long total = mongoTemplate.count(Query.of(query).limit(0).skip(0), AuditEvent.class);
        java.util.List<AuditEventView> items = mongoTemplate.find(query.with(pageable), AuditEvent.class)
                .stream()
                .map(this::toView)
                .toList();

        return new PageImpl<>(items, pageable, total);
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
