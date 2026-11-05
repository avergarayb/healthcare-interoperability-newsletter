package com.healthcare.interoperability.eventconsumer.fhir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.healthcare.interoperability.eventconsumer.exception.FhirClientException;
import com.healthcare.interoperability.eventconsumer.exception.ObservationNotFoundException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class FhirObservationClientTest {

    private MockRestServiceServer server;
    private FhirObservationClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:18080/fhir")
                .defaultHeader(HttpHeaders.ACCEPT, "application/fhir+json");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FhirObservationClient(builder.build());
    }

    @Test
    void readsAnObservationFromTheConfiguredFhirUrl() {
        server.expect(requestTo("http://localhost:18080/fhir/Observation/1000"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/fhir+json"))
                .andRespond(withSuccess("""
                        {
                          "resourceType": "Observation",
                          "id": "1000",
                          "status": "final",
                          "code": {"coding": [{"system": "https://example.org/fhir/CodeSystem/lab-008", "code": "LAB-GLUCOSE", "display": "Glucose"}]},
                          "subject": {"reference": "Patient/2000"},
                          "effectiveDateTime": "2026-11-04T21:00:00Z",
                          "valueQuantity": {"value": 92, "unit": "mg/dL"}
                        }
                        """, MediaType.parseMediaType("application/fhir+json")));

        FhirObservation observation = client.getObservation("1000");

        assertThat(observation.resourceType()).isEqualTo("Observation");
        assertThat(observation.id()).isEqualTo("1000");
        assertThat(observation.status()).isEqualTo("final");
        assertThat(observation.code().coding().getFirst().code()).isEqualTo("LAB-GLUCOSE");
        assertThat(observation.valueQuantity().value()).isEqualByComparingTo("92");
        assertThat(observation.valueQuantity().unit()).isEqualTo("mg/dL");
        server.verify();
    }

    @Test
    void translatesNotFoundWithoutKeepingTheOperationOutcome() {
        server.expect(requestTo("http://localhost:18080/fhir/Observation/missing"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.parseMediaType("application/fhir+json"))
                        .body("{\"resourceType\":\"OperationOutcome\",\"issue\":[{\"diagnostics\":\"HAPI-2001 secret-internal-detail\"}]}"));

        assertThatThrownBy(() -> client.getObservation("missing"))
                .isInstanceOf(ObservationNotFoundException.class)
                .hasMessage("Observation missing was not found")
                .hasMessageNotContaining("OperationOutcome")
                .hasMessageNotContaining("HAPI-2001")
                .hasMessageNotContaining("secret-internal-detail");
    }

    @Test
    void translatesAServerError() {
        server.expect(requestTo("http://localhost:18080/fhir/Observation/1000"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"resourceType\":\"OperationOutcome\"}"));

        assertThatThrownBy(() -> client.getObservation("1000"))
                .isInstanceOf(FhirClientException.class)
                .matches(ex -> ((FhirClientException) ex).kind() == FhirClientException.Kind.UPSTREAM_HTTP);
    }

    @Test
    void translatesAConnectionFailure() {
        server.expect(requestTo("http://localhost:18080/fhir/Observation/1000"))
                .andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> client.getObservation("1000"))
                .isInstanceOf(FhirClientException.class)
                .matches(ex -> ((FhirClientException) ex).kind() == FhirClientException.Kind.UNAVAILABLE);
    }

    @Test
    void translatesAnUnreadablePayload() {
        server.expect(requestTo("http://localhost:18080/fhir/Observation/1000"))
                .andRespond(withSuccess("not-json", MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> client.getObservation("1000"))
                .isInstanceOf(FhirClientException.class)
                .matches(ex -> ((FhirClientException) ex).kind() == FhirClientException.Kind.UNREADABLE);
    }

    @Test
    void rejectsAResourceThatIsNotAnObservation() {
        server.expect(requestTo("http://localhost:18080/fhir/Observation/1000"))
                .andRespond(withSuccess("{\"resourceType\":\"Patient\",\"id\":\"1000\"}", MediaType.parseMediaType("application/fhir+json")));

        assertThatThrownBy(() -> client.getObservation("1000"))
                .isInstanceOf(FhirClientException.class)
                .matches(ex -> ((FhirClientException) ex).kind() == FhirClientException.Kind.UNEXPECTED_RESOURCE);
    }
}
