package com.healthcare.interoperability.eventproducer.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EmbeddedKafka(
        partitions = 1,
        topics = "healthcare.observation.available.v1",
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class ObservationPublicationKafkaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void httpAcceptedMeansTheBrokerStoredTheReferenceEvent() throws Exception {
        String body = mockMvc.perform(MockMvcRequestBuilders.post("/api/events/observations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":\"1000\",\"correlationId\":\"demo-001\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode response = objectMapper.readTree(body);
        assertThat(response.get("status").asText()).isEqualTo("accepted");

        try (Consumer<String, String> consumer = brokerConsumer()) {
            broker.consumeFromAnEmbeddedTopic(consumer, "healthcare.observation.available.v1");
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
            assertThat(records.count()).isEqualTo(1);
            ConsumerRecord<String, String> record = records.iterator().next();
            assertThat(record.key()).isEqualTo("1000");
            JsonNode event = objectMapper.readTree(record.value());
            assertThat(event.get("eventId").asText()).isEqualTo(response.get("eventId").asText());
            assertThat(event.get("eventType").asText()).isEqualTo("ObservationAvailable");
            assertThat(event.get("resourceType").asText()).isEqualTo("Observation");
            assertThat(event.get("resourceId").asText()).isEqualTo("1000");
            assertThat(event.get("correlationId").asText()).isEqualTo("demo-001");
            assertThat(event.get("occurredAt").asText()).endsWith("Z");
            assertThat(event.has("valueQuantity")).isFalse();
        }
    }

    private Consumer<String, String> brokerConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps("producer-assertion", "true", broker);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);
    }
}
