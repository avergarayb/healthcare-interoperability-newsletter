package com.healthcare.interoperability.eventproducer.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.interoperability.eventproducer.config.EventProperties;
import com.healthcare.interoperability.eventproducer.exception.BrokerUnavailableException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ObservationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ObservationEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final EventProperties eventProperties;

    public ObservationEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            EventProperties eventProperties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.eventProperties = eventProperties;
    }

    public void publish(ObservationAvailableEvent event) {
        String payload = serialize(event);
        try {
            kafkaTemplate.send(eventProperties.topic(), event.resourceId(), payload)
                    .get(6, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BrokerUnavailableException(ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new BrokerUnavailableException(ex);
        }
        log.info("event=published eventId={} resourceId={} correlationId={} topic={}",
                event.eventId(), event.resourceId(), event.correlationId(), eventProperties.topic());
    }

    private String serialize(ObservationAvailableEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Event payload could not be written", ex);
        }
    }
}
