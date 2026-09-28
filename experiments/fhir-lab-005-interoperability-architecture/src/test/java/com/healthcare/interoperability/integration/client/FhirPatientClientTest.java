package com.healthcare.interoperability.integration.client;

import com.healthcare.interoperability.integration.exception.FhirClientException;
import com.healthcare.interoperability.integration.exception.FhirResourceNotFoundException;
import com.healthcare.interoperability.integration.exception.UnexpectedFhirResponseException;
import com.healthcare.interoperability.integration.fhir.FhirPatient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FhirPatientClientTest {

    private static final String BASE_URL = "http://localhost:18080/fhir";

    private MockRestServiceServer server;
    private FhirPatientClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FhirPatientClient(builder
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.ACCEPT, "application/fhir+json")
                .build());
    }

    @Test
    void getPatientByIdUsesBaseUrlAndAcceptHeader() {
        server.expect(requestTo(BASE_URL + "/Patient/1000"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, "application/fhir+json"))
                .andRespond(withSuccess(
                        """
                        {
                          "resourceType": "Patient",
                          "id": "1000",
                          "meta": { "versionId": "1" },
                          "identifier": [ { "value": "LAB-002-PATIENT-001" } ],
                          "name": [ { "family": "Example", "given": [ "Lucia" ] } ],
                          "gender": "female",
                          "birthDate": "1988-03-20"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        FhirPatient patient = client.getPatientById("1000");

        assertEquals("Patient", patient.resourceType());
        assertEquals("1000", patient.id());
        assertEquals("Example", patient.name().getFirst().family());
        assertEquals("Lucia", patient.name().getFirst().given().getFirst());
        assertEquals("female", patient.gender());
        assertEquals("1988-03-20", patient.birthDate());
        server.verify();
    }

    @Test
    void getPatientByIdThrowsWhenFhirServerReturnsNotFound() {
        server.expect(requestTo(BASE_URL + "/Patient/does-not-exist"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, "application/fhir+json"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"resourceType\":\"OperationOutcome\"}"));

        FhirResourceNotFoundException ex = assertThrows(
                FhirResourceNotFoundException.class,
                () -> client.getPatientById("does-not-exist")
        );
        assertEquals("does-not-exist", ex.getResourceId());
        server.verify();
    }

    @Test
    void getPatientByIdThrowsWhenFhirServerReturnsServerError() {
        server.expect(requestTo(BASE_URL + "/Patient/1000"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"resourceType\":\"OperationOutcome\"}"));

        FhirClientException ex = assertThrows(
                FhirClientException.class,
                () -> client.getPatientById("1000")
        );
        assertEquals(500, ex.getStatusCode());
        server.verify();
    }

    @Test
    void getPatientByIdThrowsWhenConnectionFails() {
        server.expect(requestTo(BASE_URL + "/Patient/1000"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new IOException("Connection refused")));

        FhirClientException ex = assertThrows(
                FhirClientException.class,
                () -> client.getPatientById("1000")
        );
        assertTrue(ex.isUnavailable());
        server.verify();
    }

    @Test
    void getPatientByIdThrowsWhenBodyIsNotJson() {
        server.expect(requestTo(BASE_URL + "/Patient/1000"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("not-json", MediaType.TEXT_PLAIN));

        assertThrows(UnexpectedFhirResponseException.class, () -> client.getPatientById("1000"));
        server.verify();
    }
}
