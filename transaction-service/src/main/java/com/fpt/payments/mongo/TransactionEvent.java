package com.fpt.payments.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "transaction_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {

    @Id
    private String id;

    private UUID transactionId;
    private String fromStatus;
    private String toStatus;
    private String changedBy;
    private Instant timestamp;
    private String metadata;
}
