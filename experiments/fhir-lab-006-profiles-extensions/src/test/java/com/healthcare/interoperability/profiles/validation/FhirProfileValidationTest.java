package com.healthcare.interoperability.profiles.validation;

import com.healthcare.interoperability.profiles.dto.ValidatedHealthcarePatient;
import com.healthcare.interoperability.profiles.exception.ProfileConstraintException;
import com.healthcare.interoperability.profiles.exception.UnreadableFhirPayloadException;
import com.healthcare.interoperability.profiles.fhir.LabProfileUrls;
import com.healthcare.interoperability.profiles.service.PatientValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class FhirProfileValidationTest {

    @Autowired
    private PatientValidationService patientValidationService;

    @Test
    void acceptsAPatientThatDeclaresTheLabProfile() {
        ValidatedHealthcarePatient patient = patientValidationService.validate(validPatient(true, "email", true));

        assertEquals("1000", patient.id());
        assertEquals("Lucia", patient.firstName());
        assertEquals("Example", patient.lastName());
        assertEquals("1988-03-20", patient.birthDate());
        assertEquals("email", patient.preferredContactChannel());
    }

    @Test
    void acceptsTheSamePatientWithoutMetaProfile() {
        ValidatedHealthcarePatient patient = patientValidationService.validate(validPatient(false, "sms", false));

        assertNull(patient.id());
        assertEquals("sms", patient.preferredContactChannel());
    }

    @Test
    void rejectsAMissingIdentifier() {
        assertMessage(without("identifier"), "Patient.identifier is required");
    }

    @Test
    void rejectsAMissingName() {
        assertMessage(without("name"), "Patient.name is required");
    }

    @Test
    void rejectsAMissingBirthDate() {
        assertMessage(without("birthDate"), "Patient.birthDate is required");
    }

    @Test
    void rejectsAnUnsupportedContactChannel() {
        assertMessage(validPatient(true, "fax", true), "preferredContactChannel must be phone, email, or sms");
    }

    @Test
    void rejectsAMalformedExtension() {
        String json = """
                {
                  "resourceType": "Patient",
                  "identifier": [{ "system": "https://example.org/patient-id", "value": "LAB-006-001" }],
                  "name": [{ "family": "Example", "given": ["Lucia"] }],
                  "birthDate": "1988-03-20",
                  "extension": [{
                    "url": "%s",
                    "valueString": "email"
                  }]
                }
                """.formatted(LabProfileUrls.PREFERRED_CONTACT_CHANNEL);
        assertMessage(json, "preferredContactChannel extension is not valid");
    }

    @Test
    void rejectsANonPatientResource() {
        String json = """
                {
                  "resourceType": "Observation",
                  "status": "final",
                  "code": { "text": "demo" }
                }
                """;
        assertMessage(json, "Payload is not a Patient");
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(UnreadableFhirPayloadException.class, () -> patientValidationService.validate("not-json"));
    }

    @Test
    void declaringTheProfileDoesNotBypassAMissingBirthDate() {
        String json = """
                {
                  "resourceType": "Patient",
                  "meta": { "profile": ["%s"] },
                  "identifier": [{ "system": "https://example.org/patient-id", "value": "LAB-006-001" }],
                  "name": [{ "family": "Example", "given": ["Lucia"] }]
                }
                """.formatted(LabProfileUrls.PATIENT_PROFILE);
        assertMessage(json, "Patient.birthDate is required");
    }

    private void assertMessage(String json, String expected) {
        ProfileConstraintException ex = assertThrows(ProfileConstraintException.class, () -> patientValidationService.validate(json));
        assertTrue(
                ex.getErrors().stream().anyMatch(error -> expected.equals(error.message())),
                ex.getErrors().toString()
        );
        assertTrue(ex.getErrors().stream().noneMatch(error -> error.message().contains("HAPI-")));
        assertTrue(ex.getErrors().stream().noneMatch(error -> error.message().contains("ca.uhn")));
    }

    private String validPatient(boolean withMeta, String channel, boolean withId) {
        String meta = withMeta
                ? "\"meta\": { \"profile\": [\"" + LabProfileUrls.PATIENT_PROFILE + "\"] },"
                : "";
        String id = withId ? "\"id\": \"1000\"," : "";
        return """
                {
                  "resourceType": "Patient",
                  %s
                  %s
                  "identifier": [{ "system": "https://example.org/patient-id", "value": "LAB-006-001" }],
                  "name": [{ "family": "Example", "given": ["Lucia"] }],
                  "birthDate": "1988-03-20",
                  "extension": [{
                    "url": "%s",
                    "valueCode": "%s"
                  }]
                }
                """.formatted(meta, id, LabProfileUrls.PREFERRED_CONTACT_CHANNEL, channel);
    }

    private String without(String field) {
        String identifier = "identifier".equals(field)
                ? ""
                : "\"identifier\": [{ \"system\": \"https://example.org/patient-id\", \"value\": \"LAB-006-001\" }],";
        String name = "name".equals(field)
                ? ""
                : "\"name\": [{ \"family\": \"Example\", \"given\": [\"Lucia\"] }],";
        String birthDate = "birthDate".equals(field) ? "" : "\"birthDate\": \"1988-03-20\",";
        return """
                {
                  "resourceType": "Patient",
                  %s
                  %s
                  %s
                  "extension": [{
                    "url": "%s",
                    "valueCode": "email"
                  }]
                }
                """.formatted(identifier, name, birthDate, LabProfileUrls.PREFERRED_CONTACT_CHANNEL);
    }
}
