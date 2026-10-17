package com.healthcare.interoperability.fhirintegration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.healthcare.interoperability.fhirintegration.client.FhirPatientClient;
import com.healthcare.interoperability.fhirintegration.dto.IntegrationPatientResponse;
import com.healthcare.interoperability.fhirintegration.exception.InvalidPatientIdException;
import com.healthcare.interoperability.fhirintegration.fhir.FhirHumanName;
import com.healthcare.interoperability.fhirintegration.fhir.FhirPatient;
import com.healthcare.interoperability.fhirintegration.mapping.PatientMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FhirPatientIntegrationServiceTest {

    @Mock
    private FhirPatientClient fhirPatientClient;

    @Spy
    private PatientMapper patientMapper = new PatientMapper();

    @InjectMocks
    private FhirPatientIntegrationService integrationService;

    @Test
    void rejectsABlankIdBeforeCallingFhir() {
        assertThatThrownBy(() -> integrationService.getPatient(" "))
                .isInstanceOf(InvalidPatientIdException.class);
        verify(fhirPatientClient, never()).getPatientById(" ");
    }

    @Test
    void mapsTheFhirPatientToTheIntegrationContract() {
        when(fhirPatientClient.getPatientById("1000")).thenReturn(new FhirPatient(
                "Patient",
                "1000",
                List.of(new FhirHumanName("Example", List.of("Lucia"))),
                "female",
                "1988-03-20"));

        IntegrationPatientResponse response = integrationService.getPatient("1000");

        assertThat(response.firstName()).isEqualTo("Lucia");
        assertThat(response.lastName()).isEqualTo("Example");
    }
}
