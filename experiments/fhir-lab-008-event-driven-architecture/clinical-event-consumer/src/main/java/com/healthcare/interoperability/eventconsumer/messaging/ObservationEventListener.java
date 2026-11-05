package com.healthcare.interoperability.eventconsumer.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.interoperability.eventconsumer.service.ObservationEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ObservationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ObservationEventListener.class);

    private final ObjectMapper objectMapper;
    private final ObservationEventHandler observationEventHandler;

    public ObservationEventListener(ObjectMapper objectMapper, ObservationEventHandler observationEventHandler) {
        this.objectMapper = objectMapper;
        this.observationEventHandler = observationEventHandler;
    }

    @KafkaListener(topics = "${events.topic}", groupId = "${events.group-id}")
    public void onEvent(String payload) throws Exception {
        ObservationAvailableEvent event = objectMapper.readValue(payload, ObservationAvailableEvent.class);
        log.info("event=consumed eventId={} resourceId={} correlationId={}",
                event.eventId(), event.resourceId(), event.correlationId());
        observationEventHandler.handle(event);
    }
}
