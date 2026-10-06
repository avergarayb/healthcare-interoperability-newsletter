package com.healthcare.interoperability.profiles.service;

import com.healthcare.interoperability.profiles.dto.ValidatedHealthcarePatient;
import com.healthcare.interoperability.profiles.dto.ValidationError;
import com.healthcare.interoperability.profiles.exception.ProfileConstraintException;
import com.healthcare.interoperability.profiles.mapping.PatientMapper;
import com.healthcare.interoperability.profiles.validation.FhirProfileValidationService;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientValidationService {

    private final FhirProfileValidationService profileValidationService;
    private final PatientMapper patientMapper;

    public PatientValidationService(
            FhirProfileValidationService profileValidationService,
            PatientMapper patientMapper
    ) {
        this.profileValidationService = profileValidationService;
        this.patientMapper = patientMapper;
    }

    public ValidatedHealthcarePatient validate(String json) {
        Patient patient = profileValidationService.parsePatient(json);
        if (patient == null) {
            throw new ProfileConstraintException(List.of(
                    new ValidationError("profile_validation_error", "Payload is not a Patient")
            ));
        }
        List<ValidationError> errors = profileValidationService.validate(patient);
        if (!errors.isEmpty()) {
            throw new ProfileConstraintException(errors);
        }
        return patientMapper.toApplicationPatient(patient);
    }
}
