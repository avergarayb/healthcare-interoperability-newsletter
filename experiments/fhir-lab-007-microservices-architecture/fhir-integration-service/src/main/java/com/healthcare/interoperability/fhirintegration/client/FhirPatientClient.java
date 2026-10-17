package com.healthcare.interoperability.fhirintegration.client;

import com.healthcare.interoperability.fhirintegration.exception.FhirClientException;
import com.healthcare.interoperability.fhirintegration.exception.PatientNotFoundException;
import com.healthcare.interoperability.fhirintegration.fhir.FhirPatient;
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
                        throw new PatientNotFoundException(patientId);
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw FhirClientException.upstreamHttp(response.getStatusCode().value());
                    })
                    .body(FhirPatient.class);
            if (patient == null || patient.resourceType() == null || !"Patient".equals(patient.resourceType())) {
                throw FhirClientException.unreadable();
            }
            return patient;
        } catch (PatientNotFoundException | FhirClientException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw FhirClientException.unavailable(ex);
        } catch (RestClientException ex) {
            throw FhirClientException.unreadable(ex);
        }
    }
}
