package com.healthcare.interoperability.eventconsumer.mapping;

import com.healthcare.interoperability.eventconsumer.fhir.FhirCodeableConcept;
import com.healthcare.interoperability.eventconsumer.fhir.FhirCoding;
import com.healthcare.interoperability.eventconsumer.fhir.FhirObservation;
import com.healthcare.interoperability.eventconsumer.fhir.FhirQuantity;
import com.healthcare.interoperability.eventconsumer.messaging.ObservationAvailableEvent;
import com.healthcare.interoperability.eventconsumer.model.ProcessedObservation;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ObservationMapper {

    public ProcessedObservation toProcessed(ObservationAvailableEvent event, FhirObservation observation) {
        FhirCoding coding = firstCoding(observation.code());
        FhirQuantity quantity = observation.valueQuantity();
        return new ProcessedObservation(
                event.eventId(),
                observation.id(),
                observation.status(),
                coding == null ? null : coding.code(),
                display(coding, observation.code()),
                quantity == null || quantity.value() == null ? null : quantity.value().toPlainString(),
                quantity == null ? null : quantity.unit(),
                observation.subject() == null ? null : observation.subject().reference(),
                event.correlationId()
        );
    }

    private FhirCoding firstCoding(FhirCodeableConcept code) {
        if (code == null) {
            return null;
        }
        List<FhirCoding> coding = code.coding();
        if (coding == null || coding.isEmpty()) {
            return null;
        }
        return coding.getFirst();
    }

    private String display(FhirCoding coding, FhirCodeableConcept code) {
        if (coding != null && coding.display() != null) {
            return coding.display();
        }
        return code == null ? null : code.text();
    }
}
