package com.ledger.event.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="EVENT_LEDGER")
@Data
public class TransactionEvent {

    @Id
    @Column(name = "EVENT_ID", nullable = false)
    private String eventId;

    @Column(name = "ACCOUNT_ID", nullable = false)
    private String accountId;

    @Column(name = "TYPE", nullable = false)
    private String type;

    @Column(name = "AMOUNT", nullable = false, precision = 12, scale = 4)
    private BigDecimal amount;

    @Column(name = "CURRENCY", nullable = false)
    private String currency;

    @Column(name = "EVENT_TIMESTAMP", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "METADATA", columnDefinition = "TEXT")
    private String metadata;

}
