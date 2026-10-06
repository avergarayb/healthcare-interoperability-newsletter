package com.healthcare.interoperability.profiles.validation;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import com.healthcare.interoperability.profiles.dto.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationMessageTranslatorTest {

    private final ValidationMessageTranslator translator = new ValidationMessageTranslator();

    @Test
    void mapsCardinalityMessagesToStableText() {
        List<ValidationError> errors = translator.translate(List.of(
                message("Patient", "Patient.identifier: mínimo requerido = 1, pero sólo se han encontrado 0"),
                message("Patient.birthDate", "Patient.birthDate: minimum required = 1, but only found 0")
        ));

        assertEquals("Patient.identifier is required", errors.get(0).message());
        assertEquals("Patient.birthDate is required", errors.get(1).message());
        assertTrue(errors.stream().noneMatch(error -> error.message().contains("HAPI-")));
    }

    @Test
    void hidesAnUnrecognizedValidatorMessage() {
        List<ValidationError> errors = translator.translate(List.of(
                message("Patient", "ca.uhn.fhir.rest.server.exceptions.InternalErrorException: secret-internal-detail")
        ));

        assertEquals("Patient does not conform to Lab006PatientProfile", errors.get(0).message());
        assertTrue(errors.get(0).message().indexOf("secret-internal-detail") < 0);
    }

    private SingleValidationMessage message(String location, String text) {
        SingleValidationMessage message = new SingleValidationMessage();
        message.setSeverity(ResultSeverityEnum.ERROR);
        message.setLocationString(location);
        message.setMessage(text);
        return message;
    }
}
