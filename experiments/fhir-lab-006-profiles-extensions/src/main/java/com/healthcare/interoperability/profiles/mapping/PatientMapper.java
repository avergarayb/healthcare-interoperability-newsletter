package com.healthcare.interoperability.profiles.mapping;

import com.healthcare.interoperability.profiles.dto.ValidatedHealthcarePatient;
import com.healthcare.interoperability.profiles.fhir.LabProfileUrls;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {

    public ValidatedHealthcarePatient toApplicationPatient(Patient patient) {
        String firstName = null;
        String lastName = null;
        if (!patient.getName().isEmpty() && patient.getName().get(0) != null) {
            HumanName name = patient.getName().get(0);
            lastName = name.getFamily();
            if (!name.getGiven().isEmpty() && name.getGiven().get(0) != null) {
                firstName = name.getGiven().get(0).getValue();
            }
        }
        String birthDate = patient.hasBirthDateElement() ? patient.getBirthDateElement().getValueAsString() : null;
        String id = patient.hasIdElement() ? patient.getIdElement().getIdPart() : null;
        return new ValidatedHealthcarePatient(id, firstName, lastName, birthDate, contactChannel(patient));
    }

    private String contactChannel(Patient patient) {
        for (Extension extension : patient.getExtension()) {
            if (LabProfileUrls.PREFERRED_CONTACT_CHANNEL.equals(extension.getUrl())
                    && extension.getValue() instanceof CodeType code) {
                return code.getCode();
            }
        }
        return null;
    }
}
