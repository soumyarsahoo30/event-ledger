package com.ledger.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.event.dto.EventRequest;
import com.ledger.event.dto.EventResponse;
import com.ledger.event.dto.EventResult;
import com.ledger.event.model.TransactionEvent;
import com.ledger.event.repo.EventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public EventService(EventRepository eventRepository, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    public EventResult saveEvents(EventRequest eventRequest) {

        Optional<TransactionEvent> existing = eventRepository.findById(eventRequest.getEventId());

        if(existing.isPresent()){
            return new EventResult(mapToResponse(existing.get()), true);
        }else{
            TransactionEvent event = mapToEntity(eventRequest);
            eventRepository.save(event);
            return new EventResult(mapToResponse(event), false);
        }
    }

    private EventResponse mapToResponse(TransactionEvent event) {
        EventResponse response = new EventResponse();
        response.setEventId(event.getEventId());
        response.setAccountId(event.getAccountId());
        response.setType(event.getType());
        response.setAmount(event.getAmount());
        response.setCurrency(event.getCurrency());
        response.setEventTimestamp(event.getEventTimestamp().toString());

        if (event.getMetadata() != null) {
            try {
                response.setMetadata(objectMapper.readValue(event.getMetadata(), Map.class));
            } catch (JsonProcessingException e) {

            }
        }
        return response;
    }

    private TransactionEvent mapToEntity(EventRequest request) {
        TransactionEvent event = new TransactionEvent();
        event.setEventId(request.getEventId());
        event.setAccountId(request.getAccountId());
        event.setType(request.getType());
        event.setAmount(request.getAmount());
        event.setCurrency(request.getCurrency());
        event.setEventTimestamp(Instant.parse(request.getEventTimestamp()));

        if (request.getMetadata() != null) {
            try {
                event.setMetadata(objectMapper.writeValueAsString(request.getMetadata()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid metadata format");
            }
        }
        return event;
    }

}
