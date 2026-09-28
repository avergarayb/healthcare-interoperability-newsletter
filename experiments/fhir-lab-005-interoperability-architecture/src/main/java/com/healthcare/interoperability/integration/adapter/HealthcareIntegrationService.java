package com.healthcare.interoperability.integration.adapter;

import com.healthcare.interoperability.integration.client.FhirPatientClient;
import com.healthcare.interoperability.integration.dto.HealthcarePatient;
import com.healthcare.interoperability.integration.exception.FhirClientException;
import com.healthcare.interoperability.integration.exception.FhirResourceNotFoundException;
import com.healthcare.interoperability.integration.exception.IntegrationException;
import com.healthcare.interoperability.integration.exception.PatientNotFoundException;
import com.healthcare.interoperability.integration.fhir.FhirPatient;
import org.springframework.stereotype.Service;

@Service
public class HealthcareIntegrationService {

    private final FhirPatientClient fhirPatientClient;
    private final PatientMapper patientMapper;

    public HealthcareIntegrationService(FhirPatientClient fhirPatientClient, PatientMapper patientMapper) {
        this.fhirPatientClient = fhirPatientClient;
        this.patientMapper = patientMapper;
    }

    public HealthcarePatient readPatient(String patientId) {
        FhirPatient fhirPatient;
        try {
            fhirPatient = fhirPatientClient.getPatientById(patientId);
        } catch (FhirResourceNotFoundException ex) {
            throw new PatientNotFoundException(patientId);
        } catch (FhirClientException ex) {
            if (ex.isUnavailable()) {
                throw IntegrationException.unavailable(ex);
            }
            throw IntegrationException.upstreamError(ex);
        }
        return patientMapper.toHealthcarePatient(fhirPatient);
    }
}
