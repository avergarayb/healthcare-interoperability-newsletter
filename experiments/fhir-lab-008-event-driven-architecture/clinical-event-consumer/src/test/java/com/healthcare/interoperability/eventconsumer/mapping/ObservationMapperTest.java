package com.healthcare.interoperability.eventconsumer.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import com.healthcare.interoperability.eventconsumer.fhir.FhirCodeableConcept;
import com.healthcare.interoperability.eventconsumer.fhir.FhirCoding;
import com.healthcare.interoperability.eventconsumer.fhir.FhirObservation;
import com.healthcare.interoperability.eventconsumer.fhir.FhirQuantity;
import com.healthcare.interoperability.eventconsumer.fhir.FhirReference;
import com.healthcare.interoperability.eventconsumer.messaging.ObservationAvailableEvent;
import com.healthcare.interoperability.eventconsumer.model.ProcessedObservation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ObservationMapperTest {

    private final ObservationMapper mapper = new ObservationMapper();

    @Test
    void mapsTheFirstCodingAndQuantityIntoTheApplicationModel() {
        ProcessedObservation processed = mapper.toProcessed(event(), observation());

        assertThat(processed.eventId()).isEqualTo("event-1");
        assertThat(processed.observationId()).isEqualTo("1000");
        assertThat(processed.status()).isEqualTo("final");
        assertThat(processed.code()).isEqualTo("LAB-GLUCOSE");
        assertThat(processed.display()).isEqualTo("Glucose");
        assertThat(processed.value()).isEqualTo("92");
        assertThat(processed.unit()).isEqualTo("mg/dL");
        assertThat(processed.patientReference()).isEqualTo("Patient/2000");
        assertThat(processed.correlationId()).isEqualTo("manual-lab-008");
    }

    @Test
    void leavesClinicalFieldsEmptyWhenTheObservationOmitsThem() {
        FhirObservation observation = new FhirObservation("Observation", "1000", "final", null, null, null, null);

        ProcessedObservation processed = mapper.toProcessed(event(), observation);

        assertThat(processed.code()).isNull();
        assertThat(processed.display()).isNull();
        assertThat(processed.value()).isNull();
        assertThat(processed.unit()).isNull();
        assertThat(processed.patientReference()).isNull();
    }

    private ObservationAvailableEvent event() {
        return new ObservationAvailableEvent(
                "event-1",
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
                new FhirCodeableConcept(List.of(
                        new FhirCoding("https://example.org/fhir/CodeSystem/lab-008", "LAB-GLUCOSE", "Glucose"),
                        new FhirCoding("https://example.org/fhir/CodeSystem/lab-008", "OTHER", "Other")
                ), null),
                new FhirReference("Patient/2000"),
                "2026-11-04T21:00:00Z",
                new FhirQuantity(new BigDecimal("92"), "mg/dL")
        );
    }
}
