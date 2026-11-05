package com.healthcare.interoperability.eventconsumer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.healthcare.interoperability.eventconsumer.exception.FhirClientException;
import com.healthcare.interoperability.eventconsumer.exception.InvalidEventException;
import com.healthcare.interoperability.eventconsumer.exception.ObservationNotFoundException;
import com.healthcare.interoperability.eventconsumer.fhir.FhirCodeableConcept;
import com.healthcare.interoperability.eventconsumer.fhir.FhirCoding;
import com.healthcare.interoperability.eventconsumer.fhir.FhirObservation;
import com.healthcare.interoperability.eventconsumer.fhir.FhirObservationClient;
import com.healthcare.interoperability.eventconsumer.fhir.FhirQuantity;
import com.healthcare.interoperability.eventconsumer.fhir.FhirReference;
import com.healthcare.interoperability.eventconsumer.mapping.ObservationMapper;
import com.healthcare.interoperability.eventconsumer.messaging.ObservationAvailableEvent;
import com.healthcare.interoperability.eventconsumer.model.ProcessedObservation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObservationEventHandlerTest {

    @Mock
    private FhirObservationClient fhirObservationClient;

    private ProcessedObservationStore store;
    private ObservationEventHandler handler;

    @BeforeEach
    void setUp() {
        store = new ProcessedObservationStore();
        handler = new ObservationEventHandler(new IdempotencyGuard(store), fhirObservationClient, new ObservationMapper(), store);
    }

    @Test
    void resolvesAndStoresTheObservation() {
        when(fhirObservationClient.getObservation("1000")).thenReturn(observation());

        handler.handle(event("event-1"));

        ProcessedObservation processed = store.find("event-1").orElseThrow();
        assertThat(processed.observationId()).isEqualTo("1000");
        assertThat(processed.code()).isEqualTo("LAB-GLUCOSE");
        assertThat(processed.display()).isEqualTo("Glucose");
        assertThat(processed.value()).isEqualTo("92");
        assertThat(processed.unit()).isEqualTo("mg/dL");
        assertThat(processed.patientReference()).isEqualTo("Patient/2000");
        assertThat(processed.correlationId()).isEqualTo("manual-lab-008");
    }

    @Test
    void ignoresADuplicateEventWithoutCallingFhirAgain() {
        when(fhirObservationClient.getObservation("1000")).thenReturn(observation());

        handler.handle(event("event-1"));
        handler.handle(event("event-1"));

        verify(fhirObservationClient, times(1)).getObservation("1000");
        assertThat(store.findAll()).hasSize(1);
    }

    @Test
    void failsWhenTheObservationIsNotFound() {
        when(fhirObservationClient.getObservation("1000")).thenThrow(new ObservationNotFoundException("1000"));

        assertThatThrownBy(() -> handler.handle(event("event-404")))
                .isInstanceOf(ObservationNotFoundException.class);
        assertThat(store.contains("event-404")).isFalse();
    }

    @Test
    void failsWhenFhirIsUnavailable() {
        when(fhirObservationClient.getObservation("1000")).thenThrow(FhirClientException.unavailable(new java.io.IOException("down")));

        assertThatThrownBy(() -> handler.handle(event("event-down")))
                .isInstanceOf(FhirClientException.class);
        assertThat(store.contains("event-down")).isFalse();
    }

    @Test
    void failsWhenTheEventDoesNotReferenceAnObservation() {
        ObservationAvailableEvent event = new ObservationAvailableEvent(
                "event-bad", "ObservationAvailable", "Patient", "1000",
                Instant.parse("2026-11-04T21:00:00Z"), "manual-lab-008");

        assertThatThrownBy(() -> handler.handle(event)).isInstanceOf(InvalidEventException.class);
        verify(fhirObservationClient, never()).getObservation("1000");
    }

    @Test
    void failsWhenTheFhirPayloadIsNotAnObservation() {
        when(fhirObservationClient.getObservation("1000")).thenThrow(FhirClientException.unexpectedResource());

        assertThatThrownBy(() -> handler.handle(event("event-type")))
                .isInstanceOf(FhirClientException.class)
                .matches(ex -> ((FhirClientException) ex).kind() == FhirClientException.Kind.UNEXPECTED_RESOURCE);
        assertThat(store.contains("event-type")).isFalse();
    }

    private ObservationAvailableEvent event(String eventId) {
        return new ObservationAvailableEvent(
                eventId,
                "ObservationAvailable",
                "Observation",
                "1000",
                Instant.parse("2026-11-04T21:00:00Z"),
                "manual-lab-008"
        );
    }

    private FhirObservation observation() {
        return new FhirObservation(
                "Observation",
                "1000",
                "final",
                new FhirCodeableConcept(List.of(new FhirCoding(
                        "https://example.org/fhir/CodeSystem/lab-008", "LAB-GLUCOSE", "Glucose")), null),
                new FhirReference("Patient/2000"),
                "2026-11-04T21:00:00Z",
                new FhirQuantity(new BigDecimal("92"), "mg/dL")
        );
    }
}
