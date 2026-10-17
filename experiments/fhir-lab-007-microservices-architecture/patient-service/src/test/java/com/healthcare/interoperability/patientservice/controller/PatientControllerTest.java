package com.healthcare.interoperability.patientservice.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.healthcare.interoperability.patientservice.dto.PatientResponse;
import com.healthcare.interoperability.patientservice.exception.IntegrationUnavailableException;
import com.healthcare.interoperability.patientservice.exception.IntegrationUpstreamException;
import com.healthcare.interoperability.patientservice.exception.InvalidPatientIdException;
import com.healthcare.interoperability.patientservice.exception.PatientNotFoundException;
import com.healthcare.interoperability.patientservice.service.PatientService;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PatientController.class)
@Import(ApiExceptionHandler.class)
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientService patientService;

    @Test
    void returnsTheApplicationPatient() throws Exception {
        when(patientService.getPatient("1000"))
                .thenReturn(new PatientResponse("1000", "Lucia", "Example", "female", "1988-03-20"));

        mockMvc.perform(get("/api/patients/1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1000"))
                .andExpect(jsonPath("$.firstName").value("Lucia"))
                .andExpect(jsonPath("$.lastName").value("Example"))
                .andExpect(jsonPath("$.gender").value("female"))
                .andExpect(jsonPath("$.birthDate").value("1988-03-20"))
                .andExpect(jsonPath("$.resourceType").doesNotExist())
                .andExpect(content().string(not(containsString("OperationOutcome"))));
    }

    @Test
    void rejectsABlankId() throws Exception {
        when(patientService.getPatient(" ")).thenThrow(new InvalidPatientIdException());

        mockMvc.perform(get("/api/patients/{id}", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_id"))
                .andExpect(jsonPath("$.message").value("Patient id must not be blank"));
    }

    @Test
    void returnsNotFoundWhenTheIntegrationServiceDoes() throws Exception {
        when(patientService.getPatient("1000"))
                .thenThrow(new PatientNotFoundException("1000"));

        mockMvc.perform(get("/api/patients/1000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"))
                .andExpect(jsonPath("$.message").value("Patient 1000 was not found"))
                .andExpect(content().string(not(containsString("OperationOutcome"))))
                .andExpect(content().string(not(containsString("HAPI-"))));
    }

    @Test
    void returnsServiceUnavailableWhenTheIntegrationServiceCannotBeReached() throws Exception {
        when(patientService.getPatient("1000"))
                .thenThrow(new IntegrationUnavailableException(new IOException("http://localhost:8090 refused secret-internal-detail")));

        mockMvc.perform(get("/api/patients/1000"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("integration_unavailable"))
                .andExpect(jsonPath("$.message").value("Integration service is not reachable"))
                .andExpect(content().string(not(containsString("8090"))))
                .andExpect(content().string(not(containsString("secret-internal-detail"))))
                .andExpect(content().string(not(containsString("IOException"))));
    }

    @Test
    void returnsBadGatewayWhenTheIntegrationServiceReturnsAnError() throws Exception {
        when(patientService.getPatient("1000"))
                .thenThrow(new IntegrationUpstreamException(new IllegalStateException("http://localhost:18080/fhir HAPI-2001 secret-internal-detail")));

        mockMvc.perform(get("/api/patients/1000"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("upstream_error"))
                .andExpect(jsonPath("$.message").value("Patient data is temporarily unavailable"))
                .andExpect(content().string(not(containsString("18080"))))
                .andExpect(content().string(not(containsString("HAPI-2001"))))
                .andExpect(content().string(not(containsString("secret-internal-detail"))));
    }

    @Test
    void hidesUnexpectedFailures() throws Exception {
        when(patientService.getPatient("1000"))
                .thenThrow(new IllegalStateException("secret-internal-detail"));

        mockMvc.perform(get("/api/patients/1000"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("internal_error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("secret-internal-detail"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }
}
