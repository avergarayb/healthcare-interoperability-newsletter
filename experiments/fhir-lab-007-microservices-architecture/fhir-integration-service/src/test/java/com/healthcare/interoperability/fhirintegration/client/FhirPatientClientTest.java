package com.healthcare.interoperability.fhirintegration.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.healthcare.interoperability.fhirintegration.exception.FhirClientException;
import com.healthcare.interoperability.fhirintegration.exception.PatientNotFoundException;
import com.healthcare.interoperability.fhirintegration.fhir.FhirPatient;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class FhirPatientClientTest {

    private MockRestServiceServer server;
    private FhirPatientClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:18080/fhir")
                .defaultHeader(HttpHeaders.ACCEPT, "application/fhir+json");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FhirPatientClient(builder.build());
    }

    @Test
    void readsAPatientFromTheConfiguredFhirUrl() {
        server.expect(requestTo("http://localhost:18080/fhir/Patient/1000"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/fhir+json"))
                .andRespond(withSuccess("""
                        {
                          "resourceType": "Patient",
                          "id": "1000",
                          "meta": {"versionId": "1"},
                          "name": [{"family": "Example", "given": ["Lucia", "Maria"]}],
                          "gender": "female",
                          "birthDate": "1988-03-20"
                        }
                        """, MediaType.parseMediaType("application/fhir+json")));

        FhirPatient patient = client.getPatientById("1000");

        assertThat(patient.resourceType()).isEqualTo("Patient");
        assertThat(patient.id()).isEqualTo("1000");
        assertThat(patient.name().getFirst().family()).isEqualTo("Example");
        assertThat(patient.name().getFirst().given()).containsExactly("Lucia", "Maria");
        assertThat(patient.gender()).isEqualTo("female");
        assertThat(patient.birthDate()).isEqualTo("1988-03-20");
        server.verify();
    }

    @Test
    void translatesAFhirNotFoundWithoutKeepingTheOperationOutcome() {
        server.expect(requestTo("http://localhost:18080/fhir/Patient/missing"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/fhir+json"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.parseMediaType("application/fhir+json"))
                        .body("{\"resourceType\":\"OperationOutcome\",\"issue\":[{\"diagnostics\":\"HAPI-2001 secret-internal-detail\"}]}"));

        assertThatThrownBy(() -> client.getPatientById("missing"))
                .isInstanceOf(PatientNotFoundException.class)
                .hasMessage("Patient missing was not found")
                .hasMessageNotContaining("OperationOutcome")
                .hasMessageNotContaining("HAPI-2001")
                .hasMessageNotContaining("secret-internal-detail");
    }

    @Test
    void translatesAFhirServerError() {
        server.expect(requestTo("http://localhost:18080/fhir/Patient/1000"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"resourceType\":\"OperationOutcome\",\"issue\":[{\"diagnostics\":\"HAPI-9999\"}]}"));

        assertThatThrownBy(() -> client.getPatientById("1000"))
                .isInstanceOf(FhirClientException.class)
                .matches(ex -> ((FhirClientException) ex).kind() == FhirClientException.Kind.UPSTREAM_HTTP);
    }

    @Test
    void translatesAConnectionFailure() {
        server.expect(requestTo("http://localhost:18080/fhir/Patient/1000"))
                .andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> client.getPatientById("1000"))
                .isInstanceOf(FhirClientException.class)
                .matches(ex -> ((FhirClientException) ex).kind() == FhirClientException.Kind.UNAVAILABLE);
    }

    @Test
    void translatesAnUnreadablePayload() {
        server.expect(requestTo("http://localhost:18080/fhir/Patient/1000"))
                .andRespond(withSuccess("not-json", MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> client.getPatientById("1000"))
                .isInstanceOf(FhirClientException.class)
                .matches(ex -> ((FhirClientException) ex).kind() == FhirClientException.Kind.UNREADABLE);
    }
}
