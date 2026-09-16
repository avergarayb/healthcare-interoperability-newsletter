package com.healthcare.interoperability.fhirclient.controller;

import com.healthcare.interoperability.fhirclient.dto.HumanNameResponse;
import com.healthcare.interoperability.fhirclient.dto.PatientResponse;
import com.healthcare.interoperability.fhirclient.exception.FhirClientException;
import com.healthcare.interoperability.fhirclient.exception.FhirResourceNotFoundException;
import com.healthcare.interoperability.fhirclient.exception.InvalidPatientIdException;
import com.healthcare.interoperability.fhirclient.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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
    void getPatientReturnsMappedPatient() throws Exception {
        PatientResponse body = new PatientResponse(
                "Patient",
                "1000",
                List.of(new HumanNameResponse("Example", List.of("Lucia"))),
                "female",
                "1988-03-20"
        );
        when(patientService.getPatientById("1000")).thenReturn(body);

        mockMvc.perform(get("/api/fhir/patients/1000").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Patient"))
                .andExpect(jsonPath("$.id").value("1000"))
                .andExpect(jsonPath("$.name[0].family").value("Example"))
                .andExpect(jsonPath("$.name[0].given[0]").value("Lucia"))
                .andExpect(jsonPath("$.gender").value("female"))
                .andExpect(jsonPath("$.birthDate").value("1988-03-20"));
    }

    @Test
    void getPatientReturnsNotFoundWhenServiceReportsMissingPatient() throws Exception {
        when(patientService.getPatientById("does-not-exist"))
                .thenThrow(new FhirResourceNotFoundException("does-not-exist"));

        mockMvc.perform(get("/api/fhir/patients/does-not-exist").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"))
                .andExpect(jsonPath("$.message").value("Patient does-not-exist was not found"));
    }

    @Test
    void getPatientReturnsBadGatewayWhenFhirServerIsUnavailable() throws Exception {
        when(patientService.getPatientById("1000"))
                .thenThrow(FhirClientException.unavailable(new RuntimeException("Connection refused")));

        mockMvc.perform(get("/api/fhir/patients/1000").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("upstream_unavailable"));
    }

    @Test
    void getPatientReturnsBadGatewayWhenFhirServerReturnsHttpError() throws Exception {
        when(patientService.getPatientById("1000"))
                .thenThrow(FhirClientException.httpError(500));

        mockMvc.perform(get("/api/fhir/patients/1000").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("upstream_error"));
    }

    @Test
    void getPatientReturnsBadRequestWhenIdIsBlank() throws Exception {
        when(patientService.getPatientById(" "))
                .thenThrow(new InvalidPatientIdException());

        mockMvc.perform(get("/api/fhir/patients/{id}", " ").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_id"));
    }
}
