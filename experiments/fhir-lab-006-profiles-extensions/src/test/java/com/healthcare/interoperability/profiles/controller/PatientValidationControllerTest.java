package com.healthcare.interoperability.profiles.controller;

import com.healthcare.interoperability.profiles.dto.ValidatedHealthcarePatient;
import com.healthcare.interoperability.profiles.dto.ValidationError;
import com.healthcare.interoperability.profiles.exception.ProfileConstraintException;
import com.healthcare.interoperability.profiles.exception.UnreadableFhirPayloadException;
import com.healthcare.interoperability.profiles.service.PatientValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientValidationController.class)
@Import(ApiExceptionHandler.class)
class PatientValidationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientValidationService patientValidationService;

    @Test
    void returnsTheApplicationContract() throws Exception {
        when(patientValidationService.validate("{\"resourceType\":\"Patient\"}"))
                .thenReturn(new ValidatedHealthcarePatient("1000", "Lucia", "Example", "1988-03-20", "email"));

        mockMvc.perform(post("/api/fhir/patients/validate")
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"Patient\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.patient.id").value("1000"))
                .andExpect(jsonPath("$.patient.firstName").value("Lucia"))
                .andExpect(jsonPath("$.patient.lastName").value("Example"))
                .andExpect(jsonPath("$.patient.birthDate").value("1988-03-20"))
                .andExpect(jsonPath("$.patient.preferredContactChannel").value("email"))
                .andExpect(jsonPath("$.patient.resourceType").doesNotExist())
                .andExpect(jsonPath("$.resourceType").doesNotExist());
    }

    @Test
    void returnsValidationErrorsWithoutValidatorText() throws Exception {
        when(patientValidationService.validate("{\"resourceType\":\"Patient\"}"))
                .thenThrow(new ProfileConstraintException(List.of(
                        new ValidationError("profile_validation_error", "Patient.identifier is required")
                )));

        mockMvc.perform(post("/api/fhir/patients/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceType\":\"Patient\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("profile_validation_error"))
                .andExpect(jsonPath("$.errors[0].message").value("Patient.identifier is required"))
                .andExpect(jsonPath("$.errors[0].message").value(not(org.hamcrest.Matchers.containsString("ca.uhn"))));
    }

    @Test
    void returnsAStableBodyForMalformedPayloads() throws Exception {
        when(patientValidationService.validate("not-json"))
                .thenThrow(new UnreadableFhirPayloadException());

        mockMvc.perform(post("/api/fhir/patients/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("unreadable_payload"))
                .andExpect(jsonPath("$.errors[0].message").value("Request body is not readable FHIR JSON"));
    }

    @Test
    void hidesUnexpectedExceptionDetails() throws Exception {
        when(patientValidationService.validate("{\"resourceType\":\"Patient\"}"))
                .thenThrow(new IllegalStateException("secret-internal-detail"));

        mockMvc.perform(post("/api/fhir/patients/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceType\":\"Patient\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("internal_error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.message").value(not("secret-internal-detail")));
    }
}
