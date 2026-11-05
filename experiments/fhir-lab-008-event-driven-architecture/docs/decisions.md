# Decisions

## ObservationAvailable

The scenario needs a fact the producer can state without reading clinical data: an Observation is available. The consumer is the component that learns what the Observation contains.

## Reference instead of the full resource

The event carries `eventId`, `eventType`, `resourceType`, `resourceId`, `occurredAt`, and `correlationId`. It does not carry the Observation body.

Reasons for this lab: a smaller payload, a separate event contract, less copied clinical data, and a consumer that can read the current FHIR representation. The cost is a race. The resource may be missing or the FHIR server may be down when the event is delivered. That failure is retried and then dead-lettered. The pattern is not presented as universally better.

## Kafka

The demonstration needs a broker that can accept a record now and deliver it to another process later, including more than once. Kafka matches that shape. The lab is not a tour of Kafka features.

## Why synchronous HTTP is not enough here

A synchronous read returns the Observation, or an error, to the caller that is waiting. This scenario continues after publication. The producer can succeed while the consumer later fails. Those are different outcomes and they use different status signals: HTTP 202 versus retry and the dead-letter topic.

## Topic version

The topic name ends in `.v1` so a future incompatible payload can use another name. No second version and no schema registry are implemented. Schema governance remains a production concern.

## Key

The record key is `resourceId`. Kafka can keep records with the same key ordered inside one partition. The topic has one partition. This is not global ordering.

## Idempotency by eventId

Redelivery is identified by `eventId`, not by `resourceId`. Two different events may refer to the same Observation. A repeated `eventId` is a duplicate of one occurrence.

The guard reads `ProcessedObservationStore`. A successful map is the mark. A failure does not store the event, so retries still call FHIR. The memory disappears when the process stops. That is intentional for the lab and is not production-grade idempotency.

The consumer is duplicate-tolerant. It does not claim exactly-once processing.

## Retry and dead letter

`retry-attempts` is 2 and `retry-interval` is 500 milliseconds. `FixedBackOff` treats the second argument as the number of retries after the first delivery. Total listener executions are 3. The retry log reports `deliveryAttempt` 1, 2, and 3 for those executions. The third value is the exhausted attempt that is recovered. It is not a fourth execution. The same policy covers FHIR 404, HTTP 5xx, connection failure, an unreadable body, and a payload whose `resourceType` is not Observation. None of those failures is stored as a processed result, so the retry still calls FHIR. An event that reaches the dead-letter topic is not recorded as processed.

The recoverer writes the original record to `healthcare.observation.available.v1.DLT`, partition 0. A small listener in group `lab-008-dlt-inspector` logs the event identity and swallows a parse failure. The main listener does not subscribe to that topic, so the dead letter does not loop back into processing. Inspection is that log line or a console consumer on the dead-letter topic. It does not start another processing service.

## HTTP 202

`KafkaTemplate.send(...).get(...)` must complete before the controller returns 202. A broker failure becomes 503 `broker_unavailable`. 202 means the broker accepted the event. It does not mean the consumer processed the Observation.

Producer settings that keep this wait bounded: `max.block.ms` 3000, `request.timeout.ms` 3000, `delivery.timeout.ms` 5000. Producer idempotence is off.

## No shared Java event library

Each service defines `ObservationAvailableEvent`. The JSON contract is the integration contract. A shared jar would hide that independence behind a compile-time dependency.

## No database

The processed-event store exists so a manual run can see a result. It is not durable persistence. Transactional outbox, change data capture, event sourcing, and CQRS are out of scope. An outbox would be a production technique for publishing reliably from a database transaction. This producer has no database.

## No OAuth and no tracing stack

No authentication is implemented for Kafka or FHIR. A real environment needs authentication and authorization, and secured broker and service communication.

Logs include `correlationId`. Metrics, consumer lag, retry counts as a metric system, dead-letter volume dashboards, and distributed tracing are not implemented.

## Later processing

A later lab can take a clinical event of this kind and add processing beyond FHIR resolution. This module does not implement that step.
