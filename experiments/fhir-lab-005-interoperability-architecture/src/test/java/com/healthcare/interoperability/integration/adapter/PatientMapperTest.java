package com.healthcare.interoperability.integration.adapter;

import com.healthcare.interoperability.integration.dto.HealthcarePatient;
import com.healthcare.interoperability.integration.exception.UnexpectedFhirResponseException;
import com.healthcare.interoperability.integration.fhir.FhirHumanName;
import com.healthcare.interoperability.integration.fhir.FhirPatient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PatientMapperTest {

    private final PatientMapper mapper = new PatientMapper();

    @Test
    void mapsFirstHumanNameToApplicationFields() {
        FhirPatient fhirPatient = new FhirPatient(
                "Patient",
                "1000",
                List.of(
                        new FhirHumanName("Example", List.of("Lucia", "M.")),
                        new FhirHumanName("Other", List.of("Ignored"))
                ),
                "female",
                "1988-03-20"
        );

        HealthcarePatient patient = mapper.toHealthcarePatient(fhirPatient);

        assertEquals("1000", patient.id());
        assertEquals("Lucia", patient.firstName());
        assertEquals("Example", patient.lastName());
        assertEquals("female", patient.gender());
        assertEquals("1988-03-20", patient.birthDate());
    }

    @Test
    void leavesNamesNullWhenFhirNameIsMissing() {
        FhirPatient fhirPatient = new FhirPatient("Patient", "1000", null, "female", "1988-03-20");

        HealthcarePatient patient = mapper.toHealthcarePatient(fhirPatient);

        assertNull(patient.firstName());
        assertNull(patient.lastName());
        assertEquals("1000", patient.id());
    }

    @Test
    void rejectsAResponseThatIsNotAPatient() {
        FhirPatient fhirPatient = new FhirPatient("Observation", "1000", List.of(), null, null);

        assertThrows(UnexpectedFhirResponseException.class, () -> mapper.toHealthcarePatient(fhirPatient));
        assertThrows(UnexpectedFhirResponseException.class, () -> mapper.toHealthcarePatient(null));
    }
}
