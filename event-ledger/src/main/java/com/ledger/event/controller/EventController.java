package com.ledger.event.controller;

import com.ledger.event.dto.BalanceResponse;
import com.ledger.event.dto.EventRequest;
import com.ledger.event.dto.EventResponse;
import com.ledger.event.dto.EventResult;
import com.ledger.event.service.EventService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/events/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable String id) {
        EventResponse response = eventService.getEventById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/events")
    public ResponseEntity<List<EventResponse>> getEventsByAccount(@RequestParam String account) {
        List<EventResponse> events = eventService.getEventsByAccount(account);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountId) {
        BalanceResponse balance = eventService.getBalance(accountId);
        return ResponseEntity.ok(balance);
    }
}
