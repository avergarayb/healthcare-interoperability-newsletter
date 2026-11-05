package com.healthcare.interoperability.eventconsumer.fhir;

import com.healthcare.interoperability.eventconsumer.exception.FhirClientException;
import com.healthcare.interoperability.eventconsumer.exception.ObservationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class FhirObservationClient {

    private final RestClient fhirRestClient;

    public FhirObservationClient(RestClient fhirRestClient) {
        this.fhirRestClient = fhirRestClient;
    }

    public FhirObservation getObservation(String observationId) {
        try {
            FhirObservation observation = fhirRestClient.get()
                    .uri("/Observation/{id}", observationId)
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
                        throw new ObservationNotFoundException(observationId);
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw FhirClientException.upstreamHttp(response.getStatusCode().value());
                    })
                    .body(FhirObservation.class);
            if (observation == null || observation.resourceType() == null) {
                throw FhirClientException.unreadable();
            }
            if (!"Observation".equals(observation.resourceType())) {
                throw FhirClientException.unexpectedResource();
            }
            return observation;
        } catch (ObservationNotFoundException | FhirClientException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw FhirClientException.unavailable(ex);
        } catch (RestClientException ex) {
            throw FhirClientException.unreadable(ex);
        }
    }
}
