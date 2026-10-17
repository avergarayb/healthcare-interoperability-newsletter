package com.healthcare.interoperability.patientservice.service;

import com.healthcare.interoperability.patientservice.client.FhirIntegrationClient;
import com.healthcare.interoperability.patientservice.dto.PatientResponse;
import com.healthcare.interoperability.patientservice.exception.InvalidPatientIdException;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

    private final FhirIntegrationClient fhirIntegrationClient;

    public PatientService(FhirIntegrationClient fhirIntegrationClient) {
        this.fhirIntegrationClient = fhirIntegrationClient;
    }

    public PatientResponse getPatient(String patientId) {
        if (patientId == null || patientId.isBlank()) {
            throw new InvalidPatientIdException();
        }
        return fhirIntegrationClient.getPatient(patientId);
    }
}
