package com.healthcare.interoperability.fhirsearch.service;

import com.healthcare.interoperability.fhirsearch.client.FhirPatientSearchClient;
import com.healthcare.interoperability.fhirsearch.dto.HumanNameResponse;
import com.healthcare.interoperability.fhirsearch.dto.PatientSearchResponse;
import com.healthcare.interoperability.fhirsearch.exception.InvalidSearchIdentifierException;
import com.healthcare.interoperability.fhirsearch.exception.UnexpectedFhirResponseException;
import com.healthcare.interoperability.fhirsearch.fhir.FhirBundle;
import com.healthcare.interoperability.fhirsearch.fhir.FhirBundleEntry;
import com.healthcare.interoperability.fhirsearch.fhir.FhirBundleLink;
import com.healthcare.interoperability.fhirsearch.fhir.FhirPatientResource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PatientSearchServiceTest {

    @Test
    void searchByIdentifierRejectsBlankValueWithoutCallingFhir() {
        FhirPatientSearchClient client = mock(FhirPatientSearchClient.class);
        PatientSearchService service = new PatientSearchService(client);

        assertThrows(InvalidSearchIdentifierException.class, () -> service.searchByIdentifier(null));
        assertThrows(InvalidSearchIdentifierException.class, () -> service.searchByIdentifier(""));
        assertThrows(InvalidSearchIdentifierException.class, () -> service.searchByIdentifier(" "));
        verifyNoInteractions(client);
    }

    @Test
    void searchByIdentifierMapsSearchsetEntriesAndLinks() {
        FhirPatientSearchClient client = mock(FhirPatientSearchClient.class);
        when(client.searchByIdentifier("LAB-002-PATIENT-001")).thenReturn(new FhirBundle(
                "Bundle",
                "searchset",
                1,
                List.of(new FhirBundleEntry(
                        "http://localhost:18080/fhir/Patient/1000",
                        new FhirPatientResource(
                                "Patient",
                                "1000",
                                List.of(new HumanNameResponse("Example", List.of("Lucia"))),
                                "female",
                                "1988-03-20"
                        )
                )),
                List.of(new FhirBundleLink("self", "http://localhost:18080/fhir/Patient?identifier=LAB-002-PATIENT-001"))
        ));

        PatientSearchResponse response = new PatientSearchService(client).searchByIdentifier("LAB-002-PATIENT-001");

        assertEquals(1, response.total());
        assertEquals("1000", response.patients().getFirst().id());
        assertEquals("Lucia", response.patients().getFirst().name().getFirst().given().getFirst());
        assertEquals("self", response.links().getFirst().relation());
    }

    @Test
    void searchByIdentifierKeepsEmptySearchsetAsEmptyPatientList() {
        FhirPatientSearchClient client = mock(FhirPatientSearchClient.class);
        when(client.searchByIdentifier("DOES-NOT-EXIST")).thenReturn(new FhirBundle(
                "Bundle",
                "searchset",
                0,
                null,
                List.of(new FhirBundleLink("self", "http://localhost:18080/fhir/Patient?identifier=DOES-NOT-EXIST"))
        ));

        PatientSearchResponse response = new PatientSearchService(client).searchByIdentifier("DOES-NOT-EXIST");

        assertEquals(0, response.total());
        assertTrue(response.patients().isEmpty());
    }

    @Test
    void searchByIdentifierRejectsBundleThatIsNotSearchset() {
        FhirPatientSearchClient client = mock(FhirPatientSearchClient.class);
        when(client.searchByIdentifier("LAB-002-PATIENT-001")).thenReturn(new FhirBundle(
                "Bundle",
                "collection",
                1,
                List.of(),
                List.of()
        ));

        assertThrows(
                UnexpectedFhirResponseException.class,
                () -> new PatientSearchService(client).searchByIdentifier("LAB-002-PATIENT-001")
        );
    }
}
