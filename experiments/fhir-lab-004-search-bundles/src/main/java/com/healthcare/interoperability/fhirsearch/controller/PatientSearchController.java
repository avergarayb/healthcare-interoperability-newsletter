package com.healthcare.interoperability.fhirsearch.controller;

import com.healthcare.interoperability.fhirsearch.dto.PatientSearchResponse;
import com.healthcare.interoperability.fhirsearch.service.PatientSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fhir/patients")
public class PatientSearchController {

    private final PatientSearchService patientSearchService;

    public PatientSearchController(PatientSearchService patientSearchService) {
        this.patientSearchService = patientSearchService;
    }

    @GetMapping("/search")
    public PatientSearchResponse search(@RequestParam(required = false) String identifier) {
        return patientSearchService.searchByIdentifier(identifier);
    }
}
