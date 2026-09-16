package com.healthcare.interoperability.fhirclient.service;

import com.healthcare.interoperability.fhirclient.client.FhirPatientClient;
import com.healthcare.interoperability.fhirclient.exception.InvalidPatientIdException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class PatientServiceTest {

    @Test
    void getPatientByIdRejectsBlankIdWithoutCallingFhir() {
        FhirPatientClient fhirPatientClient = mock(FhirPatientClient.class);
        PatientService service = new PatientService(fhirPatientClient);

        assertThrows(InvalidPatientIdException.class, () -> service.getPatientById(" "));
        assertThrows(InvalidPatientIdException.class, () -> service.getPatientById(""));
        assertThrows(InvalidPatientIdException.class, () -> service.getPatientById(null));
        verifyNoInteractions(fhirPatientClient);
    }
}
