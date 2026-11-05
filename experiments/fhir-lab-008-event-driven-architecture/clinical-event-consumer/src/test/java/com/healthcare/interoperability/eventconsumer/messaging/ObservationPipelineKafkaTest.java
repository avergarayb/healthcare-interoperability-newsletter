package com.healthcare.interoperability.eventconsumer.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.interoperability.eventconsumer.model.ProcessedObservation;
import com.healthcare.interoperability.eventconsumer.service.ObservationEventHandler;
import com.healthcare.interoperability.eventconsumer.service.ProcessedObservationStore;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {
                "healthcare.observation.available.v1",
                "healthcare.observation.available.v1.DLT"
        },
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class ObservationPipelineKafkaTest {

    private static final String OBSERVATION_JSON = """
            {
              "resourceType": "Observation",
              "id": "1000",
              "status": "final",
              "code": {"coding": [{"system": "https://example.org/fhir/CodeSystem/lab-008", "code": "LAB-GLUCOSE", "display": "Glucose"}]},
              "subject": {"reference": "Patient/2000"},
              "effectiveDateTime": "2026-11-04T21:00:00Z",
              "valueQuantity": {"value": 92, "unit": "mg/dL"}
            }
            """;

    private static HttpServer httpServer;
    private static final AtomicInteger fhirCalls = new AtomicInteger();
    private static volatile int fhirStatus = 200;
    private static volatile String fhirBody = OBSERVATION_JSON;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProcessedObservationStore store;

    @Autowired
    private ObservationEventHandler observationEventHandler;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @DynamicPropertySource
    static void fhirServer(DynamicPropertyRegistry registry) throws Exception {
        if (httpServer == null) {
            httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            httpServer.createContext("/", exchange -> {
                fhirCalls.incrementAndGet();
                byte[] bytes = fhirBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/fhir+json");
                exchange.sendResponseHeaders(fhirStatus, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            httpServer.start();
        }
        int port = httpServer.getAddress().getPort();
        registry.add("fhir.base-url", () -> "http://127.0.0.1:" + port + "/fhir");
    }

    @AfterAll
    static void stopServer() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @BeforeEach
    void resetFhir() {
        fhirStatus = 200;
        fhirBody = OBSERVATION_JSON;
        fhirCalls.set(0);
    }

    @Test
    void consumesAReferenceEventAndStoresTheMappedObservation() throws Exception {
        publish("event-success", "1000", "manual-lab-008");

        ProcessedObservation processed = Awaitility.await().atMost(Duration.ofSeconds(20))
                .until(() -> store.find("event-success").orElse(null), result -> result != null);

        assertThat(processed.observationId()).isEqualTo("1000");
        assertThat(processed.code()).isEqualTo("LAB-GLUCOSE");
        assertThat(processed.display()).isEqualTo("Glucose");
        assertThat(processed.value()).isEqualTo("92");
        assertThat(processed.unit()).isEqualTo("mg/dL");
        assertThat(processed.patientReference()).isEqualTo("Patient/2000");
        assertThat(processed.correlationId()).isEqualTo("manual-lab-008");
        assertThat(fhirCalls.get()).isEqualTo(1);
    }

    @Test
    void aSecondDeliveryOfTheSameEventDoesNotCallFhirAgain() throws Exception {
        publish("event-duplicate", "1000", "manual-lab-008");
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> store.contains("event-duplicate"));
        int callsAfterFirst = fhirCalls.get();
        int duplicatesBefore = observationEventHandler.duplicatesIgnored();

        publish("event-duplicate", "1000", "manual-lab-008");
        Awaitility.await().atMost(Duration.ofSeconds(20))
                .until(() -> observationEventHandler.duplicatesIgnored() > duplicatesBefore);

        assertThat(store.findAll().stream().filter(item -> item.eventId().equals("event-duplicate")).count()).isEqualTo(1);
        assertThat(fhirCalls.get()).isEqualTo(callsAfterFirst);
    }

    @Test
    void aMissingObservationIsRetriedAndThenSentToTheDeadLetterTopic() throws Exception {
        fhirStatus = 404;
        fhirBody = "{\"resourceType\":\"OperationOutcome\",\"issue\":[{\"diagnostics\":\"HAPI-2001\"}]}";
        publish("event-missing", "does-not-exist", "manual-lab-008");

        ConsumerRecord<String, String> deadLetter = pollDeadLetter("event-missing");

        assertThat(fhirCalls.get()).isEqualTo(3);
        assertThat(deadLetter.key()).isEqualTo("does-not-exist");
        assertThat(deadLetter.value()).contains("event-missing");
        assertThat(deadLetter.value()).contains("does-not-exist");
        assertThat(deadLetter.value()).doesNotContain("OperationOutcome");
        assertThat(deadLetter.value()).doesNotContain("HAPI-2001");
        assertThat(store.contains("event-missing")).isFalse();

        fhirStatus = 200;
        fhirBody = OBSERVATION_JSON;
        int callsBeforeRecovery = fhirCalls.get();
        publish("event-after-dlt", "1000", "manual-lab-008");
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> store.contains("event-after-dlt"));
        assertThat(fhirCalls.get()).isEqualTo(callsBeforeRecovery + 1);
    }

    private void publish(String eventId, String resourceId, String correlationId) throws Exception {
        ObservationAvailableEvent event = new ObservationAvailableEvent(
                eventId,
                "ObservationAvailable",
                "Observation",
                resourceId,
                Instant.parse("2026-11-04T21:00:00Z"),
                correlationId
        );
        kafkaTemplate.send(new ProducerRecord<>("healthcare.observation.available.v1", resourceId, objectMapper.writeValueAsString(event)))
                .get(10, java.util.concurrent.TimeUnit.SECONDS);
    }

    private ConsumerRecord<String, String> pollDeadLetter(String eventId) {
        Map<String, Object> props = KafkaTestUtils.consumerProps("dlt-assert-" + eventId, "true", broker);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (Consumer<String, String> consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props)) {
            broker.consumeFromAnEmbeddedTopic(consumer, "healthcare.observation.available.v1.DLT");
            long deadline = System.currentTimeMillis() + 20000;
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : KafkaTestUtils.getRecords(consumer, Duration.ofMillis(500))) {
                    if (record.value() != null && record.value().contains(eventId)) {
                        return record;
                    }
                }
            }
        }
        throw new AssertionError("Dead-letter record was not found for " + eventId);
    }
}
