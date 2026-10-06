package com.healthcare.interoperability.profiles.controller;

import com.healthcare.interoperability.profiles.dto.ValidatedHealthcarePatient;
import com.healthcare.interoperability.profiles.dto.ValidationSuccessResponse;
import com.healthcare.interoperability.profiles.service.PatientValidationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fhir/patients")
public class PatientValidationController {

    private final PatientValidationService patientValidationService;

    public PatientValidationController(PatientValidationService patientValidationService) {
        this.patientValidationService = patientValidationService;
    }

    @PostMapping(value = "/validate", consumes = {MediaType.APPLICATION_JSON_VALUE, "application/fhir+json"})
    public ValidationSuccessResponse validate(@RequestBody String body) {
        ValidatedHealthcarePatient patient = patientValidationService.validate(body);
        return new ValidationSuccessResponse(true, patient);
    }
}
