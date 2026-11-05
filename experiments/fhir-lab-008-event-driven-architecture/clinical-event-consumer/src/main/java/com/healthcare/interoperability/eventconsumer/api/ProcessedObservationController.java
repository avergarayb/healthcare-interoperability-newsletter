package com.healthcare.interoperability.eventconsumer.api;

import com.healthcare.interoperability.eventconsumer.exception.ProcessedEventNotFoundException;
import com.healthcare.interoperability.eventconsumer.model.ProcessedObservation;
import com.healthcare.interoperability.eventconsumer.service.ProcessedObservationStore;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/processed-observations")
public class ProcessedObservationController {

    private final ProcessedObservationStore processedObservationStore;

    public ProcessedObservationController(ProcessedObservationStore processedObservationStore) {
        this.processedObservationStore = processedObservationStore;
    }

    @GetMapping
    public List<ProcessedObservation> list() {
        return processedObservationStore.findAll();
    }

    @GetMapping("/{eventId}")
    public ProcessedObservation get(@PathVariable String eventId) {
        return processedObservationStore.find(eventId)
                .orElseThrow(() -> new ProcessedEventNotFoundException(eventId));
    }
}
