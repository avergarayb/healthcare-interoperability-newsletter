package com.healthcare.interoperability.fhirintegration.mapping;

import com.healthcare.interoperability.fhirintegration.dto.IntegrationPatientResponse;
import com.healthcare.interoperability.fhirintegration.fhir.FhirHumanName;
import com.healthcare.interoperability.fhirintegration.fhir.FhirPatient;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {

    public IntegrationPatientResponse toIntegrationPatient(FhirPatient patient) {
        FhirHumanName name = firstName(patient);
        return new IntegrationPatientResponse(
                patient.id(),
                firstGiven(name),
                name == null ? null : name.family(),
                patient.gender(),
                patient.birthDate()
        );
    }

    private FhirHumanName firstName(FhirPatient patient) {
        List<FhirHumanName> names = patient.name();
        if (names == null || names.isEmpty()) {
            return null;
        }
        return names.getFirst();
    }

    private String firstGiven(FhirHumanName name) {
        if (name == null || name.given() == null || name.given().isEmpty()) {
            return null;
        }
        return name.given().getFirst();
    }
}
