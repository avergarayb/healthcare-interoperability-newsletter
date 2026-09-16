package com.healthcare.interoperability.fhirclient.service;

import com.healthcare.interoperability.fhirclient.client.FhirPatientClient;
import com.healthcare.interoperability.fhirclient.dto.PatientResponse;
import com.healthcare.interoperability.fhirclient.exception.InvalidPatientIdException;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

    private final FhirPatientClient fhirPatientClient;

    public PatientService(FhirPatientClient fhirPatientClient) {
        this.fhirPatientClient = fhirPatientClient;
    }

    public PatientResponse getPatientById(String patientId) {
        if (patientId == null || patientId.isBlank()) {
            throw new InvalidPatientIdException();
        }
        return fhirPatientClient.getPatientById(patientId);
    }
}
