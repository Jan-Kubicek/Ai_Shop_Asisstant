package cz.example.shopassistant.export.domain;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface RequestResultRepository extends MongoRepository<RequestResult, String> {

    Optional<RequestResult> findByRequestId(String requestId);
}
