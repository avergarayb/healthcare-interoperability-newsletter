package com.healthcare.interoperability.eventconsumer.service;

import com.healthcare.interoperability.eventconsumer.exception.InvalidEventException;
import com.healthcare.interoperability.eventconsumer.fhir.FhirObservation;
import com.healthcare.interoperability.eventconsumer.fhir.FhirObservationClient;
import com.healthcare.interoperability.eventconsumer.mapping.ObservationMapper;
import com.healthcare.interoperability.eventconsumer.messaging.ObservationAvailableEvent;
import com.healthcare.interoperability.eventconsumer.model.ProcessedObservation;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ObservationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(ObservationEventHandler.class);

    private final IdempotencyGuard idempotencyGuard;
    private final FhirObservationClient fhirObservationClient;
    private final ObservationMapper observationMapper;
    private final ProcessedObservationStore processedObservationStore;
    private final AtomicInteger duplicatesIgnored = new AtomicInteger();

    public ObservationEventHandler(
            IdempotencyGuard idempotencyGuard,
            FhirObservationClient fhirObservationClient,
            ObservationMapper observationMapper,
            ProcessedObservationStore processedObservationStore
    ) {
        this.idempotencyGuard = idempotencyGuard;
        this.fhirObservationClient = fhirObservationClient;
        this.observationMapper = observationMapper;
        this.processedObservationStore = processedObservationStore;
    }

    public void handle(ObservationAvailableEvent event) {
        validate(event);
        if (idempotencyGuard.isDuplicate(event.eventId())) {
            duplicatesIgnored.incrementAndGet();
            log.info("event=duplicate-ignored eventId={} correlationId={}", event.eventId(), event.correlationId());
            return;
        }
        log.info("event=received eventId={} resourceId={} correlationId={}",
                event.eventId(), event.resourceId(), event.correlationId());
        log.info("event=fhir-lookup eventId={} resourceId={} correlationId={}",
                event.eventId(), event.resourceId(), event.correlationId());
        FhirObservation observation = fhirObservationClient.getObservation(event.resourceId());
        ProcessedObservation processed = observationMapper.toProcessed(event, observation);
        processedObservationStore.save(processed);
        log.info("event=processed eventId={} observationId={} correlationId={}",
                processed.eventId(), processed.observationId(), processed.correlationId());
    }

    public int duplicatesIgnored() {
        return duplicatesIgnored.get();
    }

    private void validate(ObservationAvailableEvent event) {
        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new InvalidEventException("Event id must not be blank");
        }
        if (!"ObservationAvailable".equals(event.eventType())) {
            throw new InvalidEventException("Event type is not ObservationAvailable");
        }
        if (!"Observation".equals(event.resourceType())) {
            throw new InvalidEventException("Event resource type is not Observation");
        }
        if (event.resourceId() == null || event.resourceId().isBlank()) {
            throw new InvalidEventException("Event resource id must not be blank");
        }
    }
}
