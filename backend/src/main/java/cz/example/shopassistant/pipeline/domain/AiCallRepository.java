package cz.example.shopassistant.pipeline.domain;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface AiCallRepository extends MongoRepository<AiCall, String> {
}
