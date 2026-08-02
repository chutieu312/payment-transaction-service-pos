package com.fpt.payments.repository;

import com.fpt.payments.mongo.TransactionEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionEventRepository extends MongoRepository<TransactionEvent, String> {
    List<TransactionEvent> findByTransactionIdOrderByTimestampAsc(UUID transactionId);
}
