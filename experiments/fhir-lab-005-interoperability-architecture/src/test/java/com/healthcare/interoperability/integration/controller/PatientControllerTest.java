package com.healthcare.interoperability.integration.controller;

import com.healthcare.interoperability.integration.dto.HealthcarePatient;
import com.healthcare.interoperability.integration.exception.IntegrationException;
import com.healthcare.interoperability.integration.exception.InvalidPatientIdException;
import com.healthcare.interoperability.integration.exception.PatientNotFoundException;
import com.healthcare.interoperability.integration.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientController.class)
@Import(ApiExceptionHandler.class)
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientService patientService;

    @Test
    void getPatientReturnsTheApplicationContract() throws Exception {
        when(patientService.getPatient("1000"))
                .thenReturn(new HealthcarePatient("1000", "Lucia", "Example", "female", "1988-03-20"));

        mockMvc.perform(get("/api/healthcare/patients/1000").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1000"))
                .andExpect(jsonPath("$.firstName").value("Lucia"))
                .andExpect(jsonPath("$.lastName").value("Example"))
                .andExpect(jsonPath("$.gender").value("female"))
                .andExpect(jsonPath("$.birthDate").value("1988-03-20"))
                .andExpect(jsonPath("$.resourceType").doesNotExist())
                .andExpect(jsonPath("$.name").doesNotExist());
    }

    @Test
    void getPatientReturnsNotFound() throws Exception {
        when(patientService.getPatient("does-not-exist"))
                .thenThrow(new PatientNotFoundException("does-not-exist"));

        mockMvc.perform(get("/api/healthcare/patients/does-not-exist").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"))
                .andExpect(jsonPath("$.message").value("Patient does-not-exist was not found"));
    }

    @Test
    void getPatientReturnsBadGatewayWhenFhirServerIsUnavailable() throws Exception {
        when(patientService.getPatient("1000"))
                .thenThrow(IntegrationException.unavailable(new RuntimeException("Connection refused")));

        mockMvc.perform(get("/api/healthcare/patients/1000").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("upstream_unavailable"));
    }

    @Test
    void getPatientReturnsBadGatewayWhenFhirServerReturnsHttpError() throws Exception {
        when(patientService.getPatient("1000"))
                .thenThrow(IntegrationException.upstreamError(new RuntimeException("HTTP 500")));

        mockMvc.perform(get("/api/healthcare/patients/1000").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("upstream_error"));
    }

    @Test
    void getPatientReturnsBadRequestWhenIdIsBlank() throws Exception {
        when(patientService.getPatient(" "))
                .thenThrow(new InvalidPatientIdException());

        mockMvc.perform(get("/api/healthcare/patients/{id}", " ").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_id"));
    }

    @Test
    void getPatientReturnsInternalErrorWithoutInternalDetails() throws Exception {
        when(patientService.getPatient("1000"))
                .thenThrow(new IllegalStateException("secret-internal-detail"));

        mockMvc.perform(get("/api/healthcare/patients/1000").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("internal_error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.message").value(not("secret-internal-detail")));
    }
}
