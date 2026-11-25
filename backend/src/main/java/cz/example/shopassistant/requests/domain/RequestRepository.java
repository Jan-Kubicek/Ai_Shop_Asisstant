package cz.example.shopassistant.requests.domain;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RequestRepository extends MongoRepository<Request, String> {

    Page<Request> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    java.util.Optional<Request> findByIdAndUserId(String id, String userId);

    List<Request> findTop20ByStatusOrderByCreatedAtAsc(RequestStatus status);

    long countByStatus(RequestStatus status);

    long countByCreatedAtAfter(java.time.Instant since);

    List<Request> findByCreatedAtAfter(java.time.Instant since);
}
