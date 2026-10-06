package com.healthcare.interoperability.profiles.fhir;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public class FhirDefinitionLoader {

    private final FhirContext fhirContext;

    public FhirDefinitionLoader(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    public StructureDefinition loadPatientProfile() {
        return load("fhir/lab-006-patient-profile.json", StructureDefinition.class);
    }

    public StructureDefinition loadPreferredContactChannel() {
        return load("fhir/preferred-contact-channel-extension.json", StructureDefinition.class);
    }

    public ValueSet loadPreferredContactChannelValueSet() {
        return load("fhir/preferred-contact-channel-valueset.json", ValueSet.class);
    }

    public CodeSystem loadPreferredContactChannelCodeSystem() {
        return load("fhir/preferred-contact-channel-codesystem.json", CodeSystem.class);
    }

    private <T extends IBaseResource> T load(String classpathLocation, Class<T> type) {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        try (InputStream input = resource.getInputStream();
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return fhirContext.newJsonParser().parseResource(type, reader);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not read " + classpathLocation, ex);
        }
    }
}
