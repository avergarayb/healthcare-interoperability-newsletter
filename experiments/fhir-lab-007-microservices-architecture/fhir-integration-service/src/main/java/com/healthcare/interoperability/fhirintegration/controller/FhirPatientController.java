package com.healthcare.interoperability.fhirintegration.controller;

import com.healthcare.interoperability.fhirintegration.dto.IntegrationPatientResponse;
import com.healthcare.interoperability.fhirintegration.service.FhirPatientIntegrationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/fhir/patients")
public class FhirPatientController {

    private final FhirPatientIntegrationService integrationService;

    public FhirPatientController(FhirPatientIntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @GetMapping("/{id}")
    public IntegrationPatientResponse getPatient(@PathVariable String id) {
        return integrationService.getPatient(id);
    }
}
