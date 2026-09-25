package com.healthcare.interoperability.fhirsearch.client;

import com.healthcare.interoperability.fhirsearch.exception.FhirClientException;
import com.healthcare.interoperability.fhirsearch.exception.UnexpectedFhirResponseException;
import com.healthcare.interoperability.fhirsearch.fhir.FhirBundle;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class FhirPatientSearchClient {

    private final RestClient fhirRestClient;

    public FhirPatientSearchClient(RestClient fhirRestClient) {
        this.fhirRestClient = fhirRestClient;
    }

    public FhirBundle searchByIdentifier(String identifier) {
        try {
            FhirBundle bundle = fhirRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/Patient")
                            .queryParam("identifier", identifier)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw FhirClientException.httpError(response.getStatusCode().value());
                    })
                    .body(FhirBundle.class);
            if (bundle == null) {
                throw new UnexpectedFhirResponseException("FHIR search returned an empty body");
            }
            return bundle;
        } catch (FhirClientException | UnexpectedFhirResponseException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw FhirClientException.unavailable(ex);
        } catch (RestClientException ex) {
            throw new UnexpectedFhirResponseException("FHIR search response could not be read", ex);
        }
    }
}
