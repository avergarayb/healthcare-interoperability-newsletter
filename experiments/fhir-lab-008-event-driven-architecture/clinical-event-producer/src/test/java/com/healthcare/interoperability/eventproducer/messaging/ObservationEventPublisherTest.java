package com.healthcare.interoperability.eventproducer.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.healthcare.interoperability.eventproducer.config.EventProperties;
import com.healthcare.interoperability.eventproducer.exception.BrokerUnavailableException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class ObservationEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;
    private ObservationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        publisher = new ObservationEventPublisher(
                kafkaTemplate,
                objectMapper,
                new EventProperties("healthcare.observation.available.v1", 1)
        );
    }

    @Test
    void publishesTheReferenceEventWithTheResourceIdAsKey() throws Exception {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        ObservationAvailableEvent event = sample();

        publisher.publish(event);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("healthcare.observation.available.v1"), eq("1000"), payload.capture());
        JsonNode json = objectMapper.readTree(payload.getValue());
        assertThat(json.get("eventId").asText()).isEqualTo(event.eventId());
        assertThat(json.get("eventType").asText()).isEqualTo("ObservationAvailable");
        assertThat(json.get("resourceType").asText()).isEqualTo("Observation");
        assertThat(json.get("resourceId").asText()).isEqualTo("1000");
        assertThat(json.get("occurredAt").asText()).isEqualTo("2026-11-04T21:00:00Z");
        assertThat(json.get("correlationId").asText()).isEqualTo("demo-001");
        assertThat(json.has("valueQuantity")).isFalse();
        assertThat(json.has("status")).isFalse();
        assertThat(json.has("code")).isFalse();
        assertThat(json.has("subject")).isFalse();
    }

    @Test
    void translatesABrokerFailureWithoutKeepingItsMessage() {
        CompletableFuture<org.springframework.kafka.support.SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("localhost:9092 secret-internal-detail"));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failed);

        assertThatThrownBy(() -> publisher.publish(sample()))
                .isInstanceOf(BrokerUnavailableException.class)
                .hasMessage("Event broker is not reachable")
                .hasMessageNotContaining("9092")
                .hasMessageNotContaining("secret-internal-detail");
    }

    private ObservationAvailableEvent sample() {
        return new ObservationAvailableEvent(
                "3f6f1c0a-6b1e-4a1a-9a1a-3f6f1c0a6b1e",
                "ObservationAvailable",
                "Observation",
                "1000",
                Instant.parse("2026-11-04T21:00:00Z"),
                "demo-001"
        );
    }
}
