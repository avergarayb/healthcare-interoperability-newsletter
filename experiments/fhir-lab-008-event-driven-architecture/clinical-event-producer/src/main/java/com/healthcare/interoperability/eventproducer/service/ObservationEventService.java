package com.healthcare.interoperability.eventproducer.service;

import com.healthcare.interoperability.eventproducer.exception.InvalidResourceIdException;
import com.healthcare.interoperability.eventproducer.messaging.ObservationAvailableEvent;
import com.healthcare.interoperability.eventproducer.messaging.ObservationEventPublisher;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ObservationEventService {

    static final String EVENT_TYPE = "ObservationAvailable";
    static final String RESOURCE_TYPE = "Observation";

    private final ObservationEventPublisher publisher;
    private final Clock clock;

    public ObservationEventService(ObservationEventPublisher publisher, Clock clock) {
        this.publisher = publisher;
        this.clock = clock;
    }

    public ObservationAvailableEvent publish(String resourceId, String correlationId) {
        if (resourceId == null || resourceId.isBlank()) {
            throw new InvalidResourceIdException();
        }
        ObservationAvailableEvent event = new ObservationAvailableEvent(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                RESOURCE_TYPE,
                resourceId,
                clock.instant(),
                resolveCorrelationId(correlationId)
        );
        publisher.publish(event);
        return event;
    }

    private String resolveCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
