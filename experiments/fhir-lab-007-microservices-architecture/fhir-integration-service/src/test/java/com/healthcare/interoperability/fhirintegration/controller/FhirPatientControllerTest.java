package com.healthcare.interoperability.fhirintegration.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.healthcare.interoperability.fhirintegration.dto.IntegrationPatientResponse;
import com.healthcare.interoperability.fhirintegration.exception.FhirClientException;
import com.healthcare.interoperability.fhirintegration.exception.InvalidPatientIdException;
import com.healthcare.interoperability.fhirintegration.exception.PatientNotFoundException;
import com.healthcare.interoperability.fhirintegration.service.FhirPatientIntegrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FhirPatientController.class)
@Import(ApiExceptionHandler.class)
class FhirPatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FhirPatientIntegrationService integrationService;

    @Test
    void returnsTheIntegrationContract() throws Exception {
        when(integrationService.getPatient("1000"))
                .thenReturn(new IntegrationPatientResponse("1000", "Lucia", "Example", "female", "1988-03-20"));

        mockMvc.perform(get("/internal/fhir/patients/1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1000"))
                .andExpect(jsonPath("$.firstName").value("Lucia"))
                .andExpect(jsonPath("$.lastName").value("Example"))
                .andExpect(jsonPath("$.gender").value("female"))
                .andExpect(jsonPath("$.birthDate").value("1988-03-20"))
                .andExpect(jsonPath("$.resourceType").doesNotExist())
                .andExpect(jsonPath("$.meta").doesNotExist());
    }

    @Test
    void rejectsABlankId() throws Exception {
        when(integrationService.getPatient(" ")).thenThrow(new InvalidPatientIdException());

        mockMvc.perform(get("/internal/fhir/patients/{id}", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_id"));
    }

    @Test
    void returnsNotFoundWithoutAnOperationOutcome() throws Exception {
        when(integrationService.getPatient("1000"))
                .thenThrow(new PatientNotFoundException("1000"));

        mockMvc.perform(get("/internal/fhir/patients/1000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"))
                .andExpect(jsonPath("$.message").value("Patient 1000 was not found"))
                .andExpect(content().string(not(containsString("OperationOutcome"))))
                .andExpect(content().string(not(containsString("HAPI-"))));
    }

    @Test
    void returnsBadGatewayForAFhirServerError() throws Exception {
        when(integrationService.getPatient("1000"))
                .thenThrow(FhirClientException.upstreamHttp(500));

        mockMvc.perform(get("/internal/fhir/patients/1000"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("upstream_error"))
                .andExpect(jsonPath("$.message").value("FHIR server returned an unexpected error"))
                .andExpect(content().string(not(containsString("HAPI-"))));
    }

    @Test
    void returnsBadGatewayWhenFhirIsUnreachable() throws Exception {
        when(integrationService.getPatient("1000"))
                .thenThrow(FhirClientException.unavailable(new java.io.IOException("http://localhost:18080 refused")));

        mockMvc.perform(get("/internal/fhir/patients/1000"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("upstream_unavailable"))
                .andExpect(jsonPath("$.message").value("FHIR server is not reachable"))
                .andExpect(content().string(not(containsString("18080"))))
                .andExpect(content().string(not(containsString("IOException"))));
    }

    @Test
    void returnsBadGatewayWhenTheFhirPayloadCannotBeRead() throws Exception {
        when(integrationService.getPatient("1000"))
                .thenThrow(FhirClientException.unreadable(new IllegalStateException("secret-internal-detail")));

        mockMvc.perform(get("/internal/fhir/patients/1000"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("unreadable_payload"))
                .andExpect(jsonPath("$.message").value("FHIR response could not be read"))
                .andExpect(content().string(not(containsString("secret-internal-detail"))));
    }

    @Test
    void hidesUnexpectedFailures() throws Exception {
        when(integrationService.getPatient("1000"))
                .thenThrow(new IllegalStateException("secret-internal-detail"));

        mockMvc.perform(get("/internal/fhir/patients/1000"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("internal_error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("secret-internal-detail"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }
}
