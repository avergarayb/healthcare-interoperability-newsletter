package com.healthcare.interoperability.integration.adapter;

import com.healthcare.interoperability.integration.client.FhirPatientClient;
import com.healthcare.interoperability.integration.dto.HealthcarePatient;
import com.healthcare.interoperability.integration.exception.FhirClientException;
import com.healthcare.interoperability.integration.exception.FhirResourceNotFoundException;
import com.healthcare.interoperability.integration.exception.IntegrationException;
import com.healthcare.interoperability.integration.exception.PatientNotFoundException;
import com.healthcare.interoperability.integration.exception.UnexpectedFhirResponseException;
import com.healthcare.interoperability.integration.fhir.FhirHumanName;
import com.healthcare.interoperability.integration.fhir.FhirPatient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HealthcareIntegrationServiceTest {

    @Test
    void readPatientMapsTheFhirResourceReturnedByTheClient() {
        FhirPatientClient client = mock(FhirPatientClient.class);
        when(client.getPatientById("1000")).thenReturn(new FhirPatient(
                "Patient",
                "1000",
                List.of(new FhirHumanName("Example", List.of("Lucia"))),
                "female",
                "1988-03-20"
        ));
        HealthcareIntegrationService service = new HealthcareIntegrationService(client, new PatientMapper());

        HealthcarePatient patient = service.readPatient("1000");

        assertEquals("1000", patient.id());
        assertEquals("Lucia", patient.firstName());
        assertEquals("Example", patient.lastName());
        verify(client).getPatientById("1000");
    }

    @Test
    void readPatientTranslatesAMissingFhirPatient() {
        FhirPatientClient client = mock(FhirPatientClient.class);
        when(client.getPatientById("missing")).thenThrow(new FhirResourceNotFoundException("missing"));
        HealthcareIntegrationService service = new HealthcareIntegrationService(client, new PatientMapper());

        PatientNotFoundException ex = assertThrows(
                PatientNotFoundException.class,
                () -> service.readPatient("missing")
        );
        assertEquals("missing", ex.getPatientId());
    }

    @Test
    void readPatientTranslatesAnUnavailableFhirServer() {
        FhirPatientClient client = mock(FhirPatientClient.class);
        when(client.getPatientById("1000")).thenThrow(FhirClientException.unavailable(new RuntimeException("refused")));
        HealthcareIntegrationService service = new HealthcareIntegrationService(client, new PatientMapper());

        IntegrationException ex = assertThrows(IntegrationException.class, () -> service.readPatient("1000"));
        assertTrue(ex.isUnavailable());
    }

    @Test
    void readPatientTranslatesAnUpstreamHttpError() {
        FhirPatientClient client = mock(FhirPatientClient.class);
        when(client.getPatientById("1000")).thenThrow(FhirClientException.httpError(500));
        HealthcareIntegrationService service = new HealthcareIntegrationService(client, new PatientMapper());

        IntegrationException ex = assertThrows(IntegrationException.class, () -> service.readPatient("1000"));
        assertFalse(ex.isUnavailable());
    }

    @Test
    void readPatientRejectsANonPatientResource() {
        FhirPatientClient client = mock(FhirPatientClient.class);
        when(client.getPatientById("1000")).thenReturn(new FhirPatient("Observation", "1000", List.of(), null, null));
        HealthcareIntegrationService service = new HealthcareIntegrationService(client, new PatientMapper());

        assertThrows(UnexpectedFhirResponseException.class, () -> service.readPatient("1000"));
    }
}
