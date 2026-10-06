package com.healthcare.interoperability.profiles.validation;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import com.healthcare.interoperability.profiles.dto.ValidationError;
import com.healthcare.interoperability.profiles.fhir.LabProfileUrls;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class ValidationMessageTranslator {

    static final String CODE = "profile_validation_error";

    public List<ValidationError> translate(List<SingleValidationMessage> messages) {
        Set<String> seen = new LinkedHashSet<>();
        List<ValidationError> errors = new ArrayList<>();
        for (SingleValidationMessage message : messages) {
            if (!isFailure(message.getSeverity())) {
                continue;
            }
            String text = stableMessage(message);
            if (seen.add(text)) {
                errors.add(new ValidationError(CODE, text));
            }
        }
        if (errors.isEmpty()) {
            errors.add(new ValidationError(CODE, "Patient does not conform to Lab006PatientProfile"));
        }
        return List.copyOf(errors);
    }

    private boolean isFailure(ResultSeverityEnum severity) {
        return severity == ResultSeverityEnum.ERROR || severity == ResultSeverityEnum.FATAL;
    }

    private String stableMessage(SingleValidationMessage message) {
        String location = lower(message.getLocationString());
        String detail = lower(message.getMessage());
        String combined = location + " " + detail;
        if (combined.contains("patient.identifier") && isCardinality(detail)) {
            return "Patient.identifier is required";
        }
        if (combined.contains("patient.name") && isCardinality(detail) && !combined.contains("patient.name.given")) {
            return "Patient.name is required";
        }
        if (combined.contains("patient.birthdate") && isCardinality(detail)) {
            return "Patient.birthDate is required";
        }
        if (isWrongExtensionType(combined)) {
            return "preferredContactChannel extension is not valid";
        }
        if (isChannelCode(combined)) {
            return "preferredContactChannel must be phone, email, or sms";
        }
        if (combined.contains("not a patient") || combined.contains("resource type")) {
            return "Payload is not a Patient";
        }
        return "Patient does not conform to Lab006PatientProfile";
    }

    private boolean isCardinality(String detail) {
        return detail.contains("minimum required") || detail.contains("mínimo requerido");
    }

    private boolean isChannelCode(String combined) {
        return combined.contains(LabProfileUrls.PREFERRED_CONTACT_CHANNEL_VALUE_SET.toLowerCase(Locale.ROOT))
                || combined.contains("unknown code")
                || combined.contains("conjunto de valores")
                || combined.contains("value set");
    }

    private boolean isWrongExtensionType(String combined) {
        return combined.contains(LabProfileUrls.PREFERRED_CONTACT_CHANNEL.toLowerCase(Locale.ROOT))
                && (combined.contains("permite los tipos")
                || combined.contains("allows for the types")
                || combined.contains("found type"));
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
