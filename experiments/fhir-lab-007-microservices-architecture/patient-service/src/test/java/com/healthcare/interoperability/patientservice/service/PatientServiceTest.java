package com.healthcare.interoperability.patientservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.healthcare.interoperability.patientservice.client.FhirIntegrationClient;
import com.healthcare.interoperability.patientservice.dto.PatientResponse;
import com.healthcare.interoperability.patientservice.exception.InvalidPatientIdException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private FhirIntegrationClient fhirIntegrationClient;

    @InjectMocks
    private PatientService patientService;

    @Test
    void rejectsABlankIdBeforeCallingTheIntegrationService() {
        assertThatThrownBy(() -> patientService.getPatient(" "))
                .isInstanceOf(InvalidPatientIdException.class);
        verify(fhirIntegrationClient, never()).getPatient(" ");
    }

    @Test
    void returnsTheIntegrationPatient() {
        PatientResponse expected = new PatientResponse("1000", "Lucia", "Example", "female", "1988-03-20");
        when(fhirIntegrationClient.getPatient("1000")).thenReturn(expected);

        assertThat(patientService.getPatient("1000")).isEqualTo(expected);
    }
}
