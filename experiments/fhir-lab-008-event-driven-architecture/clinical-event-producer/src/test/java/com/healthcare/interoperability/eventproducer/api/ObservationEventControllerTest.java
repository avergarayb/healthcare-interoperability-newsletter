package com.healthcare.interoperability.eventproducer.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.healthcare.interoperability.eventproducer.exception.BrokerUnavailableException;
import com.healthcare.interoperability.eventproducer.exception.InvalidResourceIdException;
import com.healthcare.interoperability.eventproducer.messaging.ObservationAvailableEvent;
import com.healthcare.interoperability.eventproducer.service.ObservationEventService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ObservationEventController.class)
@Import(ApiExceptionHandler.class)
class ObservationEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ObservationEventService observationEventService;

    @Test
    void acceptsAPublication() throws Exception {
        when(observationEventService.publish("1000", "demo-001")).thenReturn(new ObservationAvailableEvent(
                "3f6f1c0a-6b1e-4a1a-9a1a-3f6f1c0a6b1e",
                "ObservationAvailable",
                "Observation",
                "1000",
                Instant.parse("2026-11-04T21:00:00Z"),
                "demo-001"
        ));

        mockMvc.perform(post("/api/events/observations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":\"1000\",\"correlationId\":\"demo-001\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value("3f6f1c0a-6b1e-4a1a-9a1a-3f6f1c0a6b1e"))
                .andExpect(jsonPath("$.eventType").value("ObservationAvailable"))
                .andExpect(jsonPath("$.resourceType").value("Observation"))
                .andExpect(jsonPath("$.resourceId").value("1000"))
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.valueQuantity").doesNotExist())
                .andExpect(content().string(not(containsString("9092"))));
    }

    @Test
    void rejectsABlankResourceId() throws Exception {
        when(observationEventService.publish(" ", null)).thenThrow(new InvalidResourceIdException());

        mockMvc.perform(post("/api/events/observations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_resource_id"))
                .andExpect(jsonPath("$.message").value("Observation resource id must not be blank"));
    }

    @Test
    void returnsServiceUnavailableWhenTheBrokerCannotAcceptTheEvent() throws Exception {
        when(observationEventService.publish("1000", null))
                .thenThrow(new BrokerUnavailableException(new IllegalStateException("localhost:9092 secret-internal-detail")));

        mockMvc.perform(post("/api/events/observations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":\"1000\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("broker_unavailable"))
                .andExpect(jsonPath("$.message").value("Event broker is not reachable"))
                .andExpect(content().string(not(containsString("9092"))))
                .andExpect(content().string(not(containsString("secret-internal-detail"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void hidesUnexpectedFailures() throws Exception {
        when(observationEventService.publish("1000", null))
                .thenThrow(new IllegalStateException("secret-internal-detail"));

        mockMvc.perform(post("/api/events/observations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":\"1000\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("internal_error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("secret-internal-detail"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }
}
