package com.healthcare.interoperability.patientservice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.healthcare.interoperability.patientservice.dto.PatientResponse;
import com.healthcare.interoperability.patientservice.exception.IntegrationUnavailableException;
import com.healthcare.interoperability.patientservice.exception.IntegrationUpstreamException;
import com.healthcare.interoperability.patientservice.exception.PatientNotFoundException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class FhirIntegrationClientTest {

    private MockRestServiceServer server;
    private FhirIntegrationClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8090");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FhirIntegrationClient(builder.build());
    }

    @Test
    void readsTheIntegrationContract() {
        server.expect(requestTo("http://localhost:8090/internal/fhir/patients/1000"))
                .andRespond(withSuccess("""
                        {"id":"1000","firstName":"Lucia","lastName":"Example","gender":"female","birthDate":"1988-03-20"}
                        """, MediaType.APPLICATION_JSON));

        PatientResponse patient = client.getPatient("1000");

        assertThat(patient.id()).isEqualTo("1000");
        assertThat(patient.firstName()).isEqualTo("Lucia");
        assertThat(patient.lastName()).isEqualTo("Example");
        assertThat(patient.gender()).isEqualTo("female");
        assertThat(patient.birthDate()).isEqualTo("1988-03-20");
        server.verify();
    }

    @Test
    void translatesNotFoundWithoutTheDownstreamBody() {
        server.expect(requestTo("http://localhost:8090/internal/fhir/patients/1000"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"not_found\",\"message\":\"secret-internal-detail OperationOutcome HAPI-2001\"}"));

        assertThatThrownBy(() -> client.getPatient("1000"))
                .isInstanceOf(PatientNotFoundException.class)
                .hasMessage("Patient 1000 was not found")
                .hasMessageNotContaining("secret-internal-detail")
                .hasMessageNotContaining("OperationOutcome")
                .hasMessageNotContaining("HAPI-2001");
    }

    @Test
    void translatesDownstreamServerErrorWithoutTheBody() {
        server.expect(requestTo("http://localhost:8090/internal/fhir/patients/1000"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"upstream_unavailable\",\"message\":\"http://localhost:18080/fhir secret-internal-detail\"}"));

        assertThatThrownBy(() -> client.getPatient("1000"))
                .isInstanceOf(IntegrationUpstreamException.class)
                .hasMessageNotContaining("18080")
                .hasMessageNotContaining("secret-internal-detail");
    }

    @Test
    void translatesAConnectionFailureAsUnavailable() {
        server.expect(requestTo("http://localhost:8090/internal/fhir/patients/1000"))
                .andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> client.getPatient("1000"))
                .isInstanceOf(IntegrationUnavailableException.class);
    }

    @Test
    void translatesAnUnreadableBodyAsAnUpstreamError() {
        server.expect(requestTo("http://localhost:8090/internal/fhir/patients/1000"))
                .andRespond(withSuccess("not-json", MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> client.getPatient("1000"))
                .isInstanceOf(IntegrationUpstreamException.class);
    }
}
