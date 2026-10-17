package com.healthcare.interoperability.fhirintegration.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import com.healthcare.interoperability.fhirintegration.dto.IntegrationPatientResponse;
import com.healthcare.interoperability.fhirintegration.fhir.FhirHumanName;
import com.healthcare.interoperability.fhirintegration.fhir.FhirPatient;
import java.util.List;
import org.junit.jupiter.api.Test;

class PatientMapperTest {

    private final PatientMapper mapper = new PatientMapper();

    @Test
    void mapsTheFirstGivenNameAndFamily() {
        FhirPatient patient = new FhirPatient(
                "Patient",
                "1000",
                List.of(new FhirHumanName("Example", List.of("Lucia", "Maria"))),
                "female",
                "1988-03-20");

        IntegrationPatientResponse response = mapper.toIntegrationPatient(patient);

        assertThat(response.id()).isEqualTo("1000");
        assertThat(response.firstName()).isEqualTo("Lucia");
        assertThat(response.lastName()).isEqualTo("Example");
        assertThat(response.gender()).isEqualTo("female");
        assertThat(response.birthDate()).isEqualTo("1988-03-20");
    }

    @Test
    void leavesNameFieldsEmptyWhenThePatientHasNoName() {
        FhirPatient patient = new FhirPatient("Patient", "1000", List.of(), "unknown", null);

        IntegrationPatientResponse response = mapper.toIntegrationPatient(patient);

        assertThat(response.firstName()).isNull();
        assertThat(response.lastName()).isNull();
        assertThat(response.gender()).isEqualTo("unknown");
        assertThat(response.birthDate()).isNull();
    }
}
