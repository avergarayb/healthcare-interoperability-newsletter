package com.healthcare.interoperability.fhirclient.controller;

import com.healthcare.interoperability.fhirclient.dto.PatientResponse;
import com.healthcare.interoperability.fhirclient.service.PatientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fhir/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/{id}")
    public PatientResponse getPatient(@PathVariable String id) {
        return patientService.getPatientById(id);
    }
}
