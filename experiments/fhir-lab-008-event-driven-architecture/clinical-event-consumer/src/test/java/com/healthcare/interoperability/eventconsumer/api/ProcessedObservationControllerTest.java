package com.healthcare.interoperability.eventconsumer.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.healthcare.interoperability.eventconsumer.model.ProcessedObservation;
import com.healthcare.interoperability.eventconsumer.service.ProcessedObservationStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProcessedObservationController.class)
@Import({ApiExceptionHandler.class, ProcessedObservationStore.class})
class ProcessedObservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProcessedObservationStore store;

    @Test
    void returnsAProcessedObservation() throws Exception {
        store.save(new ProcessedObservation(
                "event-1", "1000", "final", "LAB-GLUCOSE", "Glucose", "92", "mg/dL", "Patient/2000", "manual-lab-008"));

        mockMvc.perform(get("/api/processed-observations/event-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("event-1"))
                .andExpect(jsonPath("$.code").value("LAB-GLUCOSE"))
                .andExpect(jsonPath("$.correlationId").value("manual-lab-008"))
                .andExpect(jsonPath("$.resourceType").doesNotExist());
    }

    @Test
    void returnsNotFoundForAnUnknownEvent() throws Exception {
        mockMvc.perform(get("/api/processed-observations/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"))
                .andExpect(jsonPath("$.message").value("Processed event missing was not found"));
    }
}
