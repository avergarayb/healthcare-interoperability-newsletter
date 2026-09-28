package com.healthcare.interoperability.integration.adapter;

import com.healthcare.interoperability.integration.dto.HealthcarePatient;
import com.healthcare.interoperability.integration.exception.UnexpectedFhirResponseException;
import com.healthcare.interoperability.integration.fhir.FhirHumanName;
import com.healthcare.interoperability.integration.fhir.FhirPatient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PatientMapper {

    public HealthcarePatient toHealthcarePatient(FhirPatient fhirPatient) {
        if (fhirPatient == null || !"Patient".equals(fhirPatient.resourceType())) {
            throw new UnexpectedFhirResponseException("FHIR response is not a Patient");
        }
        String firstName = null;
        String lastName = null;
        List<FhirHumanName> names = fhirPatient.name();
        if (names != null && !names.isEmpty() && names.getFirst() != null) {
            FhirHumanName primary = names.getFirst();
            lastName = primary.family();
            List<String> given = primary.given();
            if (given != null && !given.isEmpty()) {
                firstName = given.getFirst();
            }
        }
        return new HealthcarePatient(
                fhirPatient.id(),
                firstName,
                lastName,
                fhirPatient.gender(),
                fhirPatient.birthDate()
        );
    }
}
