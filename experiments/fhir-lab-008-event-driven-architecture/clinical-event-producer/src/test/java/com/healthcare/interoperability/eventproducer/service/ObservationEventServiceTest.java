package com.healthcare.interoperability.eventproducer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.healthcare.interoperability.eventproducer.exception.InvalidResourceIdException;
import com.healthcare.interoperability.eventproducer.messaging.ObservationAvailableEvent;
import com.healthcare.interoperability.eventproducer.messaging.ObservationEventPublisher;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObservationEventServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T21:00:00Z");

    @Mock
    private ObservationEventPublisher publisher;

    @Mock
    private Clock clock;

    @InjectMocks
    private ObservationEventService service;

    @Test
    void rejectsABlankResourceId() {
        assertThatThrownBy(() -> service.publish(" ", "demo-001"))
                .isInstanceOf(InvalidResourceIdException.class);
    }

    @Test
    void buildsAReferenceEventAndPropagatesTheCorrelationId() {
        org.mockito.Mockito.when(clock.instant()).thenReturn(NOW);

        ObservationAvailableEvent event = service.publish("1000", "demo-001");

        assertThat(event.eventId()).isNotBlank();
        assertThat(event.eventType()).isEqualTo("ObservationAvailable");
        assertThat(event.resourceType()).isEqualTo("Observation");
        assertThat(event.resourceId()).isEqualTo("1000");
        assertThat(event.occurredAt()).isEqualTo(NOW);
        assertThat(event.correlationId()).isEqualTo("demo-001");
        assertThat(event.eventId()).isNotEqualTo(event.resourceId());
        verify(publisher).publish(event);
    }

    @Test
    void generatesACorrelationIdWhenTheCallerOmitsIt() {
        org.mockito.Mockito.when(clock.instant()).thenReturn(NOW);

        ObservationAvailableEvent event = service.publish("1000", " ");

        assertThat(event.correlationId()).isNotBlank();
        assertThat(event.correlationId()).isNotEqualTo(" ");
    }

    @Test
    void generatesADistinctEventIdForEachPublication() {
        org.mockito.Mockito.when(clock.instant()).thenReturn(NOW);

        ObservationAvailableEvent first = service.publish("1000", "demo-001");
        ObservationAvailableEvent second = service.publish("1000", "demo-001");

        assertThat(first.eventId()).isNotEqualTo(second.eventId());
        ArgumentCaptor<ObservationAvailableEvent> published = ArgumentCaptor.forClass(ObservationAvailableEvent.class);
        verify(publisher, org.mockito.Mockito.times(2)).publish(published.capture());
    }
}
