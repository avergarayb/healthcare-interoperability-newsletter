package org.hin.fhirlab001.javaclient;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
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

import java.util.List;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "fhir.operation", havingValue = "search")
public class PatientSearchRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PatientSearchRunner.class);

    private final IGenericClient fhirClient;
    private final FhirProperties properties;

    public PatientSearchRunner(IGenericClient fhirClient, FhirProperties properties) {
        this.fhirClient = fhirClient;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String system = properties.identifier().system();
        String value = properties.identifier().value();
        String baseUrl = properties.server().baseUrl();

        log.info("Searching FHIR Patient by identifier {}|{} at {}", system, value, baseUrl);

        try {
            Bundle bundle = fhirClient
                    .search()
                    .forResource(Patient.class)
                    .where(Patient.IDENTIFIER.exactly().systemAndIdentifier(system, value))
                    .returnBundle(Bundle.class)
                    .execute();

            printBundle(bundle);
        } catch (FhirClientConnectionException exception) {
            log.error("FHIR server is not available at {}", baseUrl);
            throw exception;
        }
    }

    private void printBundle(Bundle bundle) {
        System.out.println("FHIR Patient search successfully");
        System.out.println("Bundle type: " + formatBundleType(bundle));
        System.out.println("Total matches: " + (bundle.hasTotal() ? bundle.getTotal() : "(none)"));
        System.out.println("Entries: " + bundle.getEntry().size());

        List<Patient> patients = bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(this::isPatient)
                .map(Patient.class::cast)
                .toList();

        if (patients.isEmpty()) {
            System.out.println("No Patient resources found");
            return;
        }

        for (Patient patient : patients) {
            System.out.println("Patient ID: " + patient.getIdElement().getIdPart());
            System.out.println("Identifier: " + formatIdentifiers(patient));
            System.out.println("Name: " + formatNames(patient));
        }
    }

    private boolean isPatient(IBaseResource resource) {
        return resource instanceof Patient;
    }

    private String formatBundleType(Bundle bundle) {
        return bundle.hasType() ? bundle.getType().toCode() : "(none)";
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
}
