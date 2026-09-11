package org.hin.fhirlab001.javaclient;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "fhir.operation", havingValue = "read")
public class PatientReadRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PatientReadRunner.class);

    private final IGenericClient fhirClient;
    private final FhirProperties properties;

    public PatientReadRunner(IGenericClient fhirClient, FhirProperties properties) {
        this.fhirClient = fhirClient;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String patientId = properties.patient().id();
        String baseUrl = properties.server().baseUrl();

        log.info("Reading FHIR Patient {} from {}", patientId, baseUrl);

        try {
            Patient patient = fhirClient
                    .read()
                    .resource(Patient.class)
                    .withId(patientId)
                    .execute();

            printPatient(patient);
        } catch (ResourceNotFoundException exception) {
            log.error("Patient not found: {}/Patient/{}", baseUrl, patientId);
            throw exception;
        } catch (FhirClientConnectionException exception) {
            log.error("FHIR server is not available at {}", baseUrl);
            throw exception;
        }
    }

    private void printPatient(Patient patient) {
        System.out.println("FHIR Patient read successfully");
        System.out.println("Resource ID: " + patient.getIdElement().getIdPart());
        System.out.println("Identifier: " + formatIdentifiers(patient));
        System.out.println("Name: " + formatNames(patient));
        System.out.println("Telecom: " + formatTelecom(patient));
        System.out.println("Gender: " + formatGender(patient.getGender()));
        System.out.println("Birth Date: " + formatBirthDate(patient.getBirthDate()));
    }

    private String formatIdentifiers(Patient patient) {
        if (!patient.hasIdentifier()) {
            return "(none)";
        }
        return patient.getIdentifier().stream()
                .map(this::formatIdentifier)
                .collect(Collectors.joining("; "));
    }

    private String formatIdentifier(Identifier identifier) {
        return identifier.getSystem() + "|" + identifier.getValue();
    }

    private String formatNames(Patient patient) {
        if (!patient.hasName()) {
            return "(none)";
        }
        return patient.getName().stream()
                .map(this::formatName)
                .collect(Collectors.joining("; "));
    }

    private String formatName(HumanName name) {
        String given = name.getGiven().stream()
                .map(StringType::getValue)
                .collect(Collectors.joining(" "));
        return given + " " + name.getFamily();
    }

    private String formatTelecom(Patient patient) {
        if (!patient.hasTelecom()) {
            return "(none)";
        }
        return patient.getTelecom().stream()
                .map(this::formatContact)
                .collect(Collectors.joining("; "));
    }

    private String formatContact(ContactPoint contactPoint) {
        String system = contactPoint.hasSystem() ? contactPoint.getSystem().toCode() : "";
        String use = contactPoint.hasUse() ? contactPoint.getUse().toCode() : "";
        return system + " " + contactPoint.getValue() + " (" + use + ")";
    }

    private String formatGender(Enumerations.AdministrativeGender gender) {
        return gender == null ? "(none)" : gender.toCode();
    }

    private String formatBirthDate(Date birthDate) {
        if (birthDate == null) {
            return "(none)";
        }
        return DateTimeFormatter.ISO_LOCAL_DATE.format(birthDate.toInstant().atZone(ZoneOffset.UTC).toLocalDate());
    }
}
