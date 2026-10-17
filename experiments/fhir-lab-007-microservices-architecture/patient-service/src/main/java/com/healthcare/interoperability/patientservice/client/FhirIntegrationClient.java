package com.healthcare.interoperability.patientservice.client;

import com.healthcare.interoperability.patientservice.dto.PatientResponse;
import com.healthcare.interoperability.patientservice.exception.IntegrationUnavailableException;
import com.healthcare.interoperability.patientservice.exception.IntegrationUpstreamException;
import com.healthcare.interoperability.patientservice.exception.PatientNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class FhirIntegrationClient {

    private final RestClient integrationRestClient;

    public FhirIntegrationClient(RestClient integrationRestClient) {
        this.integrationRestClient = integrationRestClient;
    }

    public PatientResponse getPatient(String patientId) {
        try {
            PatientResponse patient = integrationRestClient.get()
                    .uri("/internal/fhir/patients/{id}", patientId)
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
                        throw new PatientNotFoundException(patientId);
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new IntegrationUpstreamException();
                    })
                    .body(PatientResponse.class);
            if (patient == null) {
                throw new IntegrationUpstreamException();
            }
            return patient;
        } catch (PatientNotFoundException | IntegrationUpstreamException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw new IntegrationUnavailableException(ex);
        } catch (RestClientException ex) {
            throw new IntegrationUpstreamException(ex);
        }
    }
}
