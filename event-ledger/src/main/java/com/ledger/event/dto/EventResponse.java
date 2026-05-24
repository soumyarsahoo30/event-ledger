package com.ledger.event.dto;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Map;

@Data
@ToString
public class EventResponse {

    private String eventId;
    private String accountId;
    private String type;
    private BigDecimal amount;
    private String currency;
    private String eventTimestamp;
    private Map<String, Object> metadata;
}
