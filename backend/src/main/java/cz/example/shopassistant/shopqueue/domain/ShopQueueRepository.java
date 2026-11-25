package cz.example.shopassistant.shopqueue.domain;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShopQueueRepository extends MongoRepository<ShopQueueItem, String> {

    Optional<ShopQueueItem> findByRequestId(String requestId);
}
