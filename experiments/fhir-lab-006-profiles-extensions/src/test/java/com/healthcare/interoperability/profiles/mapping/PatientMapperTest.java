package com.healthcare.interoperability.profiles.mapping;

import com.healthcare.interoperability.profiles.dto.ValidatedHealthcarePatient;
import com.healthcare.interoperability.profiles.fhir.LabProfileUrls;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PatientMapperTest {

    private final PatientMapper mapper = new PatientMapper();

    @Test
    void mapsTheFirstNameAndTheFormalExtension() {
        Patient patient = new Patient();
        patient.setId("1000");
        patient.addName().setFamily("Example").addGiven("Lucia").addGiven("M.");
        patient.getBirthDateElement().setValueAsString("1988-03-20");
        patient.addExtension(new Extension(LabProfileUrls.PREFERRED_CONTACT_CHANNEL, new CodeType("email")));
        patient.addExtension(new Extension("https://example.org/fhir/StructureDefinition/other", new CodeType("phone")));

        ValidatedHealthcarePatient mapped = mapper.toApplicationPatient(patient);

        assertEquals("1000", mapped.id());
        assertEquals("Lucia", mapped.firstName());
        assertEquals("Example", mapped.lastName());
        assertEquals("1988-03-20", mapped.birthDate());
        assertEquals("email", mapped.preferredContactChannel());
    }

    @Test
    void leavesIdAndChannelEmptyWhenTheResourceDoesNotCarryThem() {
        Patient patient = new Patient();
        patient.addName().setFamily("Example").addGiven("Lucia");
        patient.getBirthDateElement().setValueAsString("1988-03-20");

        ValidatedHealthcarePatient mapped = mapper.toApplicationPatient(patient);

        assertNull(mapped.id());
        assertNull(mapped.preferredContactChannel());
    }
}
