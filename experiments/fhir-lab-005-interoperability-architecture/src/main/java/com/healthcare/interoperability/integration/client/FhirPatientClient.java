package com.healthcare.interoperability.integration.client;

import com.healthcare.interoperability.integration.exception.FhirClientException;
import com.healthcare.interoperability.integration.exception.FhirResourceNotFoundException;
import com.healthcare.interoperability.integration.exception.UnexpectedFhirResponseException;
import com.healthcare.interoperability.integration.fhir.FhirPatient;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class FhirPatientClient {

    private final RestClient fhirRestClient;

    public FhirPatientClient(RestClient fhirRestClient) {
        this.fhirRestClient = fhirRestClient;
    }

    public FhirPatient getPatientById(String patientId) {
        try {
            FhirPatient patient = fhirRestClient.get()
                    .uri("/Patient/{id}", patientId)
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
                        throw new FhirResourceNotFoundException(patientId);
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw FhirClientException.httpError(response.getStatusCode().value());
                    })
                    .body(FhirPatient.class);
            if (patient == null) {
                throw new UnexpectedFhirResponseException("FHIR read returned an empty body");
            }
            return patient;
        } catch (FhirResourceNotFoundException | FhirClientException | UnexpectedFhirResponseException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw FhirClientException.unavailable(ex);
        } catch (RestClientException ex) {
            throw new UnexpectedFhirResponseException("FHIR Patient response could not be read", ex);
        }
    }
}
