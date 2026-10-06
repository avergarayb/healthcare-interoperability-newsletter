package com.healthcare.interoperability.profiles.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.validation.FhirValidator;
import com.healthcare.interoperability.profiles.fhir.FhirDefinitionLoader;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirValidationConfig {

    @Bean
    FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    @Bean
    FhirDefinitionLoader fhirDefinitionLoader(FhirContext fhirContext) {
        return new FhirDefinitionLoader(fhirContext);
    }

    @Bean
    FhirValidator fhirValidator(FhirContext fhirContext, FhirDefinitionLoader loader) {
        DefaultProfileValidationSupport defaultSupport = new DefaultProfileValidationSupport(fhirContext);
        PrePopulatedValidationSupport localSupport = new PrePopulatedValidationSupport(fhirContext);
        localSupport.addValueSet(loader.loadPreferredContactChannelValueSet());
        localSupport.addCodeSystem(loader.loadPreferredContactChannelCodeSystem());

        SnapshotGeneratingValidationSupport snapshotSupport = new SnapshotGeneratingValidationSupport(fhirContext);
        ValidationSupportChain snapshotChain = new ValidationSupportChain(
                defaultSupport,
                localSupport,
                snapshotSupport
        );

        StructureDefinition extension = withSnapshot(snapshotSupport, snapshotChain, loader.loadPreferredContactChannel());
        localSupport.addStructureDefinition(extension);
        StructureDefinition profile = withSnapshot(snapshotSupport, snapshotChain, loader.loadPatientProfile());
        localSupport.addStructureDefinition(profile);

        ValidationSupportChain validationSupport = new ValidationSupportChain(
                defaultSupport,
                localSupport,
                new InMemoryTerminologyServerValidationSupport(fhirContext),
                new CommonCodeSystemsTerminologyService(fhirContext),
                snapshotSupport
        );
        FhirInstanceValidator instanceValidator = new FhirInstanceValidator(validationSupport);
        instanceValidator.setAnyExtensionsAllowed(true);
        instanceValidator.setErrorForUnknownProfiles(false);

        return fhirContext.newValidator().registerValidatorModule(instanceValidator);
    }

    private StructureDefinition withSnapshot(
            SnapshotGeneratingValidationSupport snapshotSupport,
            ValidationSupportChain snapshotChain,
            StructureDefinition definition
    ) {
        ValidationSupportContext context = new ValidationSupportContext(snapshotChain);
        StructureDefinition snapshot = (StructureDefinition) snapshotSupport.generateSnapshot(
                context,
                definition,
                definition.getUrl(),
                null,
                definition.getName()
        );
        if (snapshot == null) {
            throw new IllegalStateException("Snapshot was not generated for " + definition.getUrl());
        }
        return snapshot;
    }
}
