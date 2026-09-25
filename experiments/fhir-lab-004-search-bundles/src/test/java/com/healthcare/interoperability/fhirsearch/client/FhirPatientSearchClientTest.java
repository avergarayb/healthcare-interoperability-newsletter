package com.healthcare.interoperability.fhirsearch.client;

import com.healthcare.interoperability.fhirsearch.exception.FhirClientException;
import com.healthcare.interoperability.fhirsearch.exception.UnexpectedFhirResponseException;
import com.healthcare.interoperability.fhirsearch.fhir.FhirBundle;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FhirPatientSearchClientTest {

    private static final String BASE_URL = "http://localhost:18080/fhir";

    private MockRestServiceServer server;
    private FhirPatientSearchClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FhirPatientSearchClient(builder
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.ACCEPT, "application/fhir+json")
                .build());
    }

    @Test
    void searchByIdentifierSendsQueryParamAndAcceptHeader() {
        server.expect(requestTo(BASE_URL + "/Patient?identifier=LAB-002-PATIENT-001"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, "application/fhir+json"))
                .andExpect(queryParam("identifier", "LAB-002-PATIENT-001"))
                .andRespond(withSuccess(
                        """
                        {
                          "resourceType": "Bundle",
                          "type": "searchset",
                          "total": 1,
                          "link": [ { "relation": "self", "url": "http://localhost:18080/fhir/Patient?identifier=LAB-002-PATIENT-001" } ],
                          "entry": [ {
                            "fullUrl": "http://localhost:18080/fhir/Patient/1000",
                            "resource": {
                              "resourceType": "Patient",
                              "id": "1000",
                              "meta": { "versionId": "1" },
                              "identifier": [ { "value": "LAB-002-PATIENT-001" } ],
                              "name": [ { "family": "Example", "given": [ "Lucia" ] } ],
                              "gender": "female",
                              "birthDate": "1988-03-20"
                            },
                            "search": { "mode": "match" }
                          } ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        FhirBundle bundle = client.searchByIdentifier("LAB-002-PATIENT-001");

        assertEquals("Bundle", bundle.resourceType());
        assertEquals("searchset", bundle.type());
        assertEquals(1, bundle.total());
        assertEquals("http://localhost:18080/fhir/Patient/1000", bundle.entry().getFirst().fullUrl());
        assertEquals("Patient", bundle.entry().getFirst().resource().resourceType());
        assertEquals("1000", bundle.entry().getFirst().resource().id());
        assertEquals("Example", bundle.entry().getFirst().resource().name().getFirst().family());
        assertEquals("self", bundle.link().getFirst().relation());
        server.verify();
    }

    @Test
    void searchByIdentifierReadsEmptySearchset() {
        server.expect(requestTo(BASE_URL + "/Patient?identifier=DOES-NOT-EXIST"))
                .andExpect(queryParam("identifier", "DOES-NOT-EXIST"))
                .andRespond(withSuccess(
                        """
                        {
                          "resourceType": "Bundle",
                          "type": "searchset",
                          "total": 0,
                          "link": [ { "relation": "self", "url": "http://localhost:18080/fhir/Patient?identifier=DOES-NOT-EXIST" } ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        FhirBundle bundle = client.searchByIdentifier("DOES-NOT-EXIST");

        assertEquals("searchset", bundle.type());
        assertEquals(0, bundle.total());
        assertNull(bundle.entry());
        server.verify();
    }

    @Test
    void searchByIdentifierThrowsWhenFhirServerReturnsHttpError() {
        server.expect(requestTo(BASE_URL + "/Patient?identifier=LAB-002-PATIENT-001"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"resourceType\":\"OperationOutcome\"}"));

        FhirClientException ex = assertThrows(
                FhirClientException.class,
                () -> client.searchByIdentifier("LAB-002-PATIENT-001")
        );
        assertEquals(500, ex.getStatusCode());
        server.verify();
    }

    @Test
    void searchByIdentifierThrowsWhenConnectionFails() {
        server.expect(requestTo(BASE_URL + "/Patient?identifier=LAB-002-PATIENT-001"))
                .andRespond(withException(new IOException("Connection refused")));

        FhirClientException ex = assertThrows(
                FhirClientException.class,
                () -> client.searchByIdentifier("LAB-002-PATIENT-001")
        );
        assertTrue(ex.isUnavailable());
        server.verify();
    }

    @Test
    void searchByIdentifierThrowsWhenBodyIsNotJson() {
        server.expect(requestTo(BASE_URL + "/Patient?identifier=LAB-002-PATIENT-001"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThrows(
                UnexpectedFhirResponseException.class,
                () -> client.searchByIdentifier("LAB-002-PATIENT-001")
        );
        server.verify();
    }
}
