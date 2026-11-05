package com.healthcare.interoperability.eventconsumer.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DeadLetterListener {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterListener.class);

    private final ObjectMapper objectMapper;

    public DeadLetterListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${events.dead-letter-topic}", groupId = "${events.dead-letter-group-id}")
    public void onDeadLetter(String payload) {
        try {
            ObservationAvailableEvent event = objectMapper.readValue(payload, ObservationAvailableEvent.class);
            log.warn("event=dead-letter eventId={} resourceId={} correlationId={}",
                    event.eventId(), event.resourceId(), event.correlationId());
        } catch (Exception ex) {
            log.warn("event=dead-letter-unreadable");
        }
    }
}
