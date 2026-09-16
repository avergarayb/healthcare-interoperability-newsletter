package com.healthcare.interoperability.fhirclient.client;

import com.healthcare.interoperability.fhirclient.dto.PatientResponse;
import com.healthcare.interoperability.fhirclient.exception.FhirClientException;
import com.healthcare.interoperability.fhirclient.exception.FhirResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class FhirPatientClient {

    private final RestClient fhirRestClient;

    public FhirPatientClient(RestClient fhirRestClient) {
        this.fhirRestClient = fhirRestClient;
    }

    public PatientResponse getPatientById(String patientId) {
        try {
            return fhirRestClient.get()
                    .uri("/Patient/{id}", patientId)
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
                        throw new FhirResourceNotFoundException(patientId);
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw FhirClientException.httpError(response.getStatusCode().value());
                    })
                    .body(PatientResponse.class);
        } catch (FhirResourceNotFoundException | FhirClientException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw FhirClientException.unavailable(ex);
        }
    }
}
