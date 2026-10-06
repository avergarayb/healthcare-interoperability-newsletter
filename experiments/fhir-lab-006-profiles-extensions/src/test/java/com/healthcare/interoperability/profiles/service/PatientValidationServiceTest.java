package com.healthcare.interoperability.profiles.service;

import com.healthcare.interoperability.profiles.dto.ValidatedHealthcarePatient;
import com.healthcare.interoperability.profiles.dto.ValidationError;
import com.healthcare.interoperability.profiles.exception.ProfileConstraintException;
import com.healthcare.interoperability.profiles.mapping.PatientMapper;
import com.healthcare.interoperability.profiles.validation.FhirProfileValidationService;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientValidationServiceTest {

    @Test
    void returnsTheMappedPatientWhenTheProfileAcceptsIt() {
        FhirProfileValidationService profiles = mock(FhirProfileValidationService.class);
        PatientMapper mapper = mock(PatientMapper.class);
        Patient patient = new Patient();
        ValidatedHealthcarePatient mapped = new ValidatedHealthcarePatient("1000", "Lucia", "Example", "1988-03-20", "email");
        when(profiles.parsePatient("{ }")).thenReturn(patient);
        when(profiles.validate(patient)).thenReturn(List.of());
        when(mapper.toApplicationPatient(patient)).thenReturn(mapped);
        PatientValidationService service = new PatientValidationService(profiles, mapper);

        assertEquals(mapped, service.validate("{ }"));
        verify(profiles).validate(patient);
    }

    @Test
    void rejectsAResourceThatIsNotAPatient() {
        FhirProfileValidationService profiles = mock(FhirProfileValidationService.class);
        when(profiles.parsePatient("{ }")).thenReturn(null);
        PatientValidationService service = new PatientValidationService(profiles, mock(PatientMapper.class));

        ProfileConstraintException ex = assertThrows(ProfileConstraintException.class, () -> service.validate("{ }"));
        assertEquals("Payload is not a Patient", ex.getErrors().get(0).message());
    }

    @Test
    void rejectsProfileErrorsFromTheValidator() {
        FhirProfileValidationService profiles = mock(FhirProfileValidationService.class);
        Patient patient = new Patient();
        when(profiles.parsePatient("{ }")).thenReturn(patient);
        when(profiles.validate(patient)).thenReturn(List.of(
                new ValidationError("profile_validation_error", "Patient.identifier is required")
        ));
        PatientValidationService service = new PatientValidationService(profiles, mock(PatientMapper.class));

        ProfileConstraintException ex = assertThrows(ProfileConstraintException.class, () -> service.validate("{ }"));
        assertEquals("Patient.identifier is required", ex.getErrors().get(0).message());
    }
}
