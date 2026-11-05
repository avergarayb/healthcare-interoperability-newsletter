package com.healthcare.interoperability.eventconsumer.service;

import com.healthcare.interoperability.eventconsumer.model.ProcessedObservation;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ProcessedObservationStore {

    private final ConcurrentHashMap<String, ProcessedObservation> byEventId = new ConcurrentHashMap<>();

    public boolean contains(String eventId) {
        return byEventId.containsKey(eventId);
    }

    public void save(ProcessedObservation observation) {
        byEventId.put(observation.eventId(), observation);
    }

    public Optional<ProcessedObservation> find(String eventId) {
        return Optional.ofNullable(byEventId.get(eventId));
    }

    public List<ProcessedObservation> findAll() {
        return List.copyOf(byEventId.values());
    }
}
