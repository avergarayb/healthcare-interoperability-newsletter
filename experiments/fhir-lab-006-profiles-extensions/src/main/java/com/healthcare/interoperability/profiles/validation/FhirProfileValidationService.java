package com.healthcare.interoperability.profiles.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import com.healthcare.interoperability.profiles.dto.ValidationError;
import com.healthcare.interoperability.profiles.exception.UnreadableFhirPayloadException;
import com.healthcare.interoperability.profiles.fhir.LabProfileUrls;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FhirProfileValidationService {

    private final FhirContext fhirContext;
    private final FhirValidator fhirValidator;
    private final ValidationMessageTranslator translator;

    public FhirProfileValidationService(
            FhirContext fhirContext,
            FhirValidator fhirValidator,
            ValidationMessageTranslator translator
    ) {
        this.fhirContext = fhirContext;
        this.fhirValidator = fhirValidator;
        this.translator = translator;
    }

    public Patient parsePatient(String json) {
        if (json == null || json.isBlank()) {
            throw new UnreadableFhirPayloadException();
        }
        IBaseResource resource;
        try {
            resource = fhirContext.newJsonParser().parseResource(json);
        } catch (DataFormatException ex) {
            throw new UnreadableFhirPayloadException();
        }
        if (!(resource instanceof Patient patient)) {
            return null;
        }
        return patient;
    }

    public List<ValidationError> validate(Patient patient) {
        ValidationOptions options = new ValidationOptions().addProfile(LabProfileUrls.PATIENT_PROFILE);
        ValidationResult result = fhirValidator.validateWithResult(patient, options);
        if (result.isSuccessful()) {
            return List.of();
        }
        return translator.translate(result.getMessages());
    }
}
