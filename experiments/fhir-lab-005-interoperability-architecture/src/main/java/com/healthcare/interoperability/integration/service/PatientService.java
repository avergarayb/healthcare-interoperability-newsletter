package com.healthcare.interoperability.integration.service;

import com.healthcare.interoperability.integration.dto.HealthcarePatient;
import com.healthcare.interoperability.integration.exception.InvalidPatientIdException;
import com.healthcare.interoperability.integration.adapter.HealthcareIntegrationService;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

    private final HealthcareIntegrationService healthcareIntegrationService;

    public PatientService(HealthcareIntegrationService healthcareIntegrationService) {
        this.healthcareIntegrationService = healthcareIntegrationService;
    }

    public HealthcarePatient getPatient(String patientId) {
        if (patientId == null || patientId.isBlank()) {
            throw new InvalidPatientIdException();
        }
        return healthcareIntegrationService.readPatient(patientId);
    }
}
