package com.healthcare.interoperability.profiles.fhir;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FhirDefinitionLoaderTest {

    private final FhirDefinitionLoader loader = new FhirDefinitionLoader(FhirContext.forR4());

    @Test
    void loadsThePatientProfile() {
        StructureDefinition profile = loader.loadPatientProfile();

        assertEquals(LabProfileUrls.PATIENT_PROFILE, profile.getUrl());
        assertEquals("Lab006PatientProfile", profile.getName());
        assertEquals(StructureDefinition.StructureDefinitionKind.RESOURCE, profile.getKind());
        assertEquals("Patient", profile.getType());
        assertEquals("http://hl7.org/fhir/StructureDefinition/Patient", profile.getBaseDefinition());
        assertEquals(StructureDefinition.TypeDerivationRule.CONSTRAINT, profile.getDerivation());
        assertTrue(minOf(profile, "Patient.identifier") >= 1);
        assertTrue(minOf(profile, "Patient.name") >= 1);
        assertTrue(minOf(profile, "Patient.birthDate") >= 1);
        assertTrue(profile.getDifferential().getElement().stream()
                .anyMatch(element -> "preferredContactChannel".equals(element.getSliceName())));
    }

    @Test
    void loadsTheExtensionAndValueSet() {
        StructureDefinition extension = loader.loadPreferredContactChannel();
        ValueSet valueSet = loader.loadPreferredContactChannelValueSet();
        CodeSystem codeSystem = loader.loadPreferredContactChannelCodeSystem();

        assertEquals(LabProfileUrls.PREFERRED_CONTACT_CHANNEL, extension.getUrl());
        assertEquals("PreferredContactChannel", extension.getName());
        assertEquals(StructureDefinition.StructureDefinitionKind.COMPLEXTYPE, extension.getKind());
        assertEquals("Extension", extension.getType());
        assertEquals("http://hl7.org/fhir/StructureDefinition/Extension", extension.getBaseDefinition());
        assertEquals(LabProfileUrls.PREFERRED_CONTACT_CHANNEL_VALUE_SET, valueSet.getUrl());
        assertEquals(LabProfileUrls.PREFERRED_CONTACT_CHANNEL_CODE_SYSTEM, codeSystem.getUrl());
        assertEquals(3, codeSystem.getConcept().size());
        assertFalse(extension.getDifferential().getElement().isEmpty());
    }

    private int minOf(StructureDefinition profile, String path) {
        return profile.getDifferential().getElement().stream()
                .filter(element -> path.equals(element.getPath()))
                .map(ElementDefinition::getMin)
                .findFirst()
                .orElse(-1);
    }
}
