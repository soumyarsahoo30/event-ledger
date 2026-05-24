package com.ledger.event.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Map;

@Data
@ToString
public class EventRequest {

    @NotBlank(message = "eventId is required")
    private String eventId;

    @NotBlank(message = "accountId is required")
    private String accountId;

    @NotBlank(message = "type is required")
    @Pattern(regexp = "^(CREDIT|DEBIT)$", message = "type must be either CREDIT or DEBIT")
    private String type;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    private String currency;

    @NotBlank(message = "eventTimestamp is required")
    private String eventTimestamp;

    private Map<String, Object> metadata;
}
