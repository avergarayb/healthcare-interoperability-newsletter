package com.healthcare.interoperability.integration.service;

import com.healthcare.interoperability.integration.adapter.HealthcareIntegrationService;
import com.healthcare.interoperability.integration.dto.HealthcarePatient;
import com.healthcare.interoperability.integration.exception.InvalidPatientIdException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PatientServiceTest {

    @Test
    void getPatientRejectsBlankIdWithoutCallingIntegration() {
        HealthcareIntegrationService integrationService = mock(HealthcareIntegrationService.class);
        PatientService service = new PatientService(integrationService);

        assertThrows(InvalidPatientIdException.class, () -> service.getPatient(" "));
        assertThrows(InvalidPatientIdException.class, () -> service.getPatient(""));
        assertThrows(InvalidPatientIdException.class, () -> service.getPatient(null));
        verifyNoInteractions(integrationService);
    }

    @Test
    void getPatientDelegatesANonBlankId() {
        HealthcareIntegrationService integrationService = mock(HealthcareIntegrationService.class);
        HealthcarePatient mapped = new HealthcarePatient("1000", "Lucia", "Example", "female", "1988-03-20");
        when(integrationService.readPatient("1000")).thenReturn(mapped);
        PatientService service = new PatientService(integrationService);

        HealthcarePatient patient = service.getPatient("1000");

        assertEquals(mapped, patient);
        verify(integrationService).readPatient("1000");
    }
}
