package com.healthcare.interoperability.fhirintegration.service;

import com.healthcare.interoperability.fhirintegration.client.FhirPatientClient;
import com.healthcare.interoperability.fhirintegration.dto.IntegrationPatientResponse;
import com.healthcare.interoperability.fhirintegration.exception.InvalidPatientIdException;
import com.healthcare.interoperability.fhirintegration.mapping.PatientMapper;
import org.springframework.stereotype.Service;

@Service
public class FhirPatientIntegrationService {

    private final FhirPatientClient fhirPatientClient;
    private final PatientMapper patientMapper;

    public FhirPatientIntegrationService(FhirPatientClient fhirPatientClient, PatientMapper patientMapper) {
        this.fhirPatientClient = fhirPatientClient;
        this.patientMapper = patientMapper;
    }

    public IntegrationPatientResponse getPatient(String patientId) {
        if (patientId == null || patientId.isBlank()) {
            throw new InvalidPatientIdException();
        }
        return patientMapper.toIntegrationPatient(fhirPatientClient.getPatientById(patientId));
    }
}
