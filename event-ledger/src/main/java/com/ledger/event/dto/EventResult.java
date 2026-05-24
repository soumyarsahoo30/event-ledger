package com.ledger.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class EventResult {

    private  EventResponse response;
    private  boolean duplicate;
}
