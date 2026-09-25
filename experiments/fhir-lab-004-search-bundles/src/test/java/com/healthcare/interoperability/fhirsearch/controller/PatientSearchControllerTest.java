package com.healthcare.interoperability.fhirsearch.controller;

import com.healthcare.interoperability.fhirsearch.dto.BundleLinkResponse;
import com.healthcare.interoperability.fhirsearch.dto.HumanNameResponse;
import com.healthcare.interoperability.fhirsearch.dto.PatientSearchResponse;
import com.healthcare.interoperability.fhirsearch.dto.PatientSummary;
import com.healthcare.interoperability.fhirsearch.exception.FhirClientException;
import com.healthcare.interoperability.fhirsearch.exception.InvalidSearchIdentifierException;
import com.healthcare.interoperability.fhirsearch.service.PatientSearchService;
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

@WebMvcTest(PatientSearchController.class)
@Import(ApiExceptionHandler.class)
class PatientSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientSearchService patientSearchService;

    @Test
    void searchReturnsPatients() throws Exception {
        when(patientSearchService.searchByIdentifier("LAB-002-PATIENT-001")).thenReturn(new PatientSearchResponse(
                1,
                List.of(new PatientSummary(
                        "Patient",
                        "1000",
                        List.of(new HumanNameResponse("Example", List.of("Lucia"))),
                        "female",
                        "1988-03-20"
                )),
                List.of(new BundleLinkResponse("self", "http://localhost:18080/fhir/Patient?identifier=LAB-002-PATIENT-001"))
        ));

        mockMvc.perform(get("/api/fhir/patients/search")
                        .param("identifier", "LAB-002-PATIENT-001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.patients[0].id").value("1000"))
                .andExpect(jsonPath("$.patients[0].name[0].family").value("Example"))
                .andExpect(jsonPath("$.links[0].relation").value("self"));
    }

    @Test
    void searchReturnsOkWhenNoPatientsMatch() throws Exception {
        when(patientSearchService.searchByIdentifier("DOES-NOT-EXIST")).thenReturn(new PatientSearchResponse(
                0,
                List.of(),
                List.of(new BundleLinkResponse("self", "http://localhost:18080/fhir/Patient?identifier=DOES-NOT-EXIST"))
        ));

        mockMvc.perform(get("/api/fhir/patients/search")
                        .param("identifier", "DOES-NOT-EXIST")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.patients").isEmpty());
    }

    @Test
    void searchReturnsBadRequestWhenIdentifierIsBlank() throws Exception {
        when(patientSearchService.searchByIdentifier(" ")).thenThrow(new InvalidSearchIdentifierException());

        mockMvc.perform(get("/api/fhir/patients/search")
                        .param("identifier", " ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_identifier"));
    }

    @Test
    void searchReturnsBadRequestWhenIdentifierIsMissing() throws Exception {
        when(patientSearchService.searchByIdentifier(null)).thenThrow(new InvalidSearchIdentifierException());

        mockMvc.perform(get("/api/fhir/patients/search").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_identifier"));
    }

    @Test
    void searchReturnsBadGatewayWhenFhirReturnsHttpError() throws Exception {
        when(patientSearchService.searchByIdentifier("LAB-002-PATIENT-001"))
                .thenThrow(FhirClientException.httpError(500));

        mockMvc.perform(get("/api/fhir/patients/search")
                        .param("identifier", "LAB-002-PATIENT-001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("upstream_error"));
    }

    @Test
    void searchReturnsBadGatewayWhenFhirIsUnavailable() throws Exception {
        when(patientSearchService.searchByIdentifier("LAB-002-PATIENT-001"))
                .thenThrow(FhirClientException.unavailable(new RuntimeException("Connection refused")));

        mockMvc.perform(get("/api/fhir/patients/search")
                        .param("identifier", "LAB-002-PATIENT-001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("upstream_unavailable"));
    }

    @Test
    void searchReturnsInternalErrorWithoutInternalDetails() throws Exception {
        when(patientSearchService.searchByIdentifier("LAB-002-PATIENT-001"))
                .thenThrow(new IllegalStateException("secret-internal-detail"));

        mockMvc.perform(get("/api/fhir/patients/search")
                        .param("identifier", "LAB-002-PATIENT-001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("internal_error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not("secret-internal-detail")));
    }
}
