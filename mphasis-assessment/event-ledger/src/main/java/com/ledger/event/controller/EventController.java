package com.ledger.event.controller;

import com.ledger.event.dto.EventRequest;
import com.ledger.event.dto.EventResponse;
import com.ledger.event.dto.EventResult;
import com.ledger.event.service.EventService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class EventController {

    private EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/events")
    public ResponseEntity<EventResponse> saveEvents(@Valid @RequestBody EventRequest eventRequest){
        log.info("Event Request received:: "+eventRequest);
        EventResult eventResult = eventService.saveEvents(eventRequest);
        HttpStatus status = eventResult.isDuplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(eventResult.getResponse());
    }
}
