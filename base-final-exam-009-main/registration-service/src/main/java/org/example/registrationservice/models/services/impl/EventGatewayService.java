package org.example.registrationservice.models.services.impl;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.example.registrationservice.clients.EventClient;
import org.example.registrationservice.exceptions.EventNotFoundException;
import org.example.registrationservice.exceptions.EventServiceException;
import org.example.registrationservice.models.dto.responses.EventResponse;
import org.example.registrationservice.models.entities.Registration;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventGatewayService {

    private final EventClient eventClient;

    @CircuitBreaker(name = "roomService", fallbackMethod = "getEventFallback")
    public EventResponse getEventById(Long id) {
        EventResponse event = eventClient.getEventById(id);

        if (event == null || event.id() == null || event.ticketPrice() == null) {
            throw new EventServiceException(
                    "Event service returned invalid data for event " + id
            );
        }
        if (event.ticketPrice() < 0) {
            throw new EventServiceException(
                    "Event price per night must not be negative for event " + id
            );
        }

        return event;
    }

    private EventResponse getEventFallback(Long id, Throwable throwable) {
        if (throwable instanceof FeignException.NotFound) {
            throw new EventNotFoundException(id);
        }
        if (throwable instanceof EventNotFoundException eventNotFoundException) {
            throw eventNotFoundException;
        }
        if (throwable instanceof EventServiceException eventServiceException) {
            throw eventServiceException;
        }

        throw new EventServiceException(
                "Event service is unavailable for event " + id,
                throwable
        );
    }
}