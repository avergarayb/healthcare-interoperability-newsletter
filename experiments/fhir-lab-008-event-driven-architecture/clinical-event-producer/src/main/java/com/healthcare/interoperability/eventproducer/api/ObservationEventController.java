package com.healthcare.interoperability.eventproducer.api;

import com.healthcare.interoperability.eventproducer.messaging.ObservationAvailableEvent;
import com.healthcare.interoperability.eventproducer.service.ObservationEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/observations")
public class ObservationEventController {

    private final ObservationEventService observationEventService;

    public ObservationEventController(ObservationEventService observationEventService) {
        this.observationEventService = observationEventService;
    }

    @PostMapping
    public ResponseEntity<PublishObservationResponse> publish(@RequestBody PublishObservationRequest request) {
        ObservationAvailableEvent event = observationEventService.publish(request.resourceId(), request.correlationId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new PublishObservationResponse(
                event.eventId(),
                event.eventType(),
                event.resourceType(),
                event.resourceId(),
                "accepted"
        ));
    }
}
