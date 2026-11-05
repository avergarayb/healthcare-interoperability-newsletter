# Asynchronous FHIR observation events

This lab shows a small event-driven path for one FHIR-related change. It is a simplified demonstration of boundaries. It is not a universal healthcare architecture and it is not a general introduction to Kafka.

## Objective

When an Observation becomes available, a producer publishes a reference event. A consumer reads that event later, loads the current FHIR Observation, and records a small application result.

```text
FHIR Observation created/available
        |
        v
Clinical Event Producer :8091
        |
        | ObservationAvailable
        v
Kafka :9092
        |
        v
Clinical Event Consumer :8092
        |
        v
FHIR Integration Client
        |
        v
FHIR Server :18080
        |
        v
Observation
        |
        v
Application processing
```

REST alone answers the caller immediately. This lab is for work that continues after the publication request has returned.

## Event and FHIR resource

The event says that something happened. The FHIR Observation is the clinical data.

```text
Event
{
  "eventType": "ObservationAvailable",
  "resourceId": "1000"
}

FHIR
{
  "resourceType": "Observation",
  "id": "1000",
  "status": "final",
  "code": { "coding": [ { "code": "LAB-GLUCOSE" } ] },
  "valueQuantity": { "value": 92, "unit": "mg/dL" }
}
```

`eventId` identifies one event instance. `resourceId` identifies the FHIR resource. They are not interchangeable.

The Kafka record does not contain `status`, `code`, `valueQuantity`, or `subject`.

## Why the event carries only a reference

The event payload stays small. The event contract stays separate from the FHIR resource contract. Clinical data is not copied into the broker. The consumer reads the current representation when it runs.

The tradeoff is explicit: the FHIR resource might be unreadable when the event is consumed. That race is a failure scenario in this lab, with retry and a dead-letter topic. This reference style is not universally better than embedding a resource.

In a healthcare system the smaller payload also keeps ownership of the clinical record with the FHIR server. This lab does not make a legal or compliance claim.

## Services

### Producer (`clinical-event-producer`, port 8091)

`POST /api/events/observations`

```json
{ "resourceId": "1000", "correlationId": "demo-001" }
```

`correlationId` may be omitted. The producer then generates one.

HTTP 202:

```json
{
  "eventId": "...",
  "eventType": "ObservationAvailable",
  "resourceType": "Observation",
  "resourceId": "1000",
  "status": "accepted"
}
```

Flow:

```text
ObservationEventController
  -> ObservationEventService
  -> ObservationEventPublisher
  -> KafkaTemplate
  -> healthcare.observation.available.v1
```

The producer validates the id, generates `eventId` and `occurredAt`, and waits until the broker accepts the record. It does not read FHIR and it has no HAPI dependency. The fixed strings `ObservationAvailable` and `Observation` are event-contract values, not a parsed Observation.

| Condition | HTTP | Body |
| --- | --- | --- |
| Blank `resourceId` | 400 | `invalid_resource_id` |
| Broker does not accept the record | 503 | `broker_unavailable` |
| Unexpected error | 500 | `internal_error` |

The 503 body does not include the broker address or an exception class name.

### Consumer (`clinical-event-consumer`, port 8092)

```text
Kafka listener
  -> ObservationEventHandler
  -> IdempotencyGuard
  -> FhirObservationClient
  -> FHIR server
  -> ObservationMapper
  -> ProcessedObservationStore
```

There is no HTTP endpoint that consumes the event. `GET /api/processed-observations` and `GET /api/processed-observations/{eventId}` only inspect the in-memory result. They are not part of the event path.

## Topic and contract

Topic: `healthcare.observation.available.v1`

Dead-letter topic: `healthcare.observation.available.v1.DLT`

```json
{
  "eventId": "3f6f1c0a-6b1e-4a1a-9a1a-3f6f1c0a6b1e",
  "eventType": "ObservationAvailable",
  "resourceType": "Observation",
  "resourceId": "1000",
  "occurredAt": "2026-11-04T21:00:00Z",
  "correlationId": "demo-001"
}
```

`occurredAt` is ISO-8601 UTC. `eventType` is always `ObservationAvailable`. `resourceType` on the event is always `Observation`.

The suffix `.v1` records that the contract can change. This lab does not implement a second version and does not use a schema registry.

Each service owns its own `ObservationAvailableEvent` type. There is no shared Java module. The JSON document is the contract. The tradeoff is duplicated types and no compile-time check that the two classes still match.

## Message key, partitions, and group

The Kafka key is `resourceId`. With one partition, events for the same key stay in order inside that partition. This lab does not claim global ordering and does not publish other event types.

Consumer group: `lab-008-observation-processor`. The group owns the main topic. This lab runs one instance. A dead-letter inspector uses the separate group `lab-008-dlt-inspector` and only logs `eventId`, `resourceId`, and `correlationId`.

## FHIR lookup and mapping

`GET {fhir.base-url}/Observation/{resourceId}`

Header: `Accept: application/fhir+json`

`fhir.base-url` defaults to `http://localhost:18080/fhir`. Connect timeout is 2 seconds. Read timeout is 5 seconds.

`FhirObservation` keeps `resourceType`, `id`, `status`, `code`, `subject`, `effectiveDateTime`, and `valueQuantity`. A body whose `resourceType` is not `Observation` fails processing.

Mapping into `ProcessedObservation` is specific to this lab:

| Application field | Source |
| --- | --- |
| `eventId`, `correlationId` | the event |
| `observationId` | `Observation.id` |
| `status` | `Observation.status` |
| `code` | `code` of the first `coding` |
| `display` | `display` of that coding, or `code.text` |
| `value` | `valueQuantity.value` |
| `unit` | `valueQuantity.unit` |
| `patientReference` | `subject.reference` |

`effectiveDateTime` is read with the subset and is not copied into the application result.

The sample Observation uses code `LAB-GLUCOSE` and display `Glucose` from `https://example.org/fhir/CodeSystem/lab-008`, value `92`, unit `mg/dL`. That code system is local to the lab. It is not LOINC. This lab does not use SNOMED.

## Idempotency

Kafka can deliver a record more than once. After a successful process, the same `eventId` is ignored. The duplicate path does not call FHIR and does not store a second result.

Failed attempts are not marked complete, so a retry can call FHIR again.

The registry is the in-memory `ProcessedObservationStore`. Restarting the consumer drops it. A real deployment would need durable or shared idempotency storage. This lab does not claim exactly-once processing. The consumer is duplicate-tolerant for `eventId` while the process stays up: delivery is at-least-once, and a repeated `eventId` does not create a second result.

## Retry and dead letter

`FixedBackOff(500ms, 2)` means:

```text
initial delivery
+ 2 retries
= 3 total attempts
```

The wait between attempts is 500 milliseconds. There is no long backoff.

A processing exception, including FHIR 404, FHIR 5xx, a connection failure, an unreadable body, or a non-Observation payload, fails the attempt. After the third failure the original event is published to `healthcare.observation.available.v1.DLT`. The consumer process keeps running. The dead-letter value is the original event JSON, not the FHIR error body.

The retry log writes `deliveryAttempt` once for each failed execution, including the last one. The values 1, 2, and 3 are those three executions. Value 3 is the failure recovered to the dead-letter topic. It is not a fourth attempt.

The main listener subscribes only to `healthcare.observation.available.v1`, group `lab-008-observation-processor`. A separate listener in group `lab-008-dlt-inspector` reads the dead-letter topic, logs `eventId`, `resourceId`, and `correlationId`, and does not throw. It does not call FHIR and it does not publish another record, so a dead-letter record does not re-enter the main topic. Inspect the topic with the Kafka console consumer from the beginning, or read that log line. The dead-letter record is not stored as a `ProcessedObservation`.

```text
event
  |
  v
consumer
  |
  X repeated processing failure
  |
  v
healthcare.observation.available.v1.DLT
```

## Failure boundaries

### FHIR resource not readable yet

The event can arrive before `GET /Observation/{id}` succeeds. HTTP 404 does not become a processed success. The consumer retries and then dead-letters the event.

### FHIR server down

The producer can still receive HTTP 202 if Kafka accepts the record. The consumer fails later, retries, and dead-letters the event if the server stays down. Publication success is not processing success.

In the synchronous lab, the caller receives the FHIR failure in the same HTTP response. Here the publisher can already have finished.

### Kafka down during publication

`POST /api/events/observations` waits for the broker acknowledgement (`KafkaTemplate.send(...).get(...)`, with `max.block.ms` 3000). If Kafka does not accept the record, the response is 503 `broker_unavailable`, not 202.

## HTTP 202

202 means the event was accepted by Kafka. It does not mean the Observation was read or processed.

## Correlation id

The caller may send `correlationId`. Otherwise the producer generates one. The same value is stored on the event, written in producer and consumer logs, and copied onto `ProcessedObservation`. This lab does not export traces.

## Security and observability

There is no Kafka authentication and no FHIR authentication. A real environment needs authentication and authorization for the broker and for FHIR access.

Consumer lag, retry metrics, dead-letter volume, and distributed tracing are production concerns. Only `correlationId` is implemented here.

## Tests

From this directory:

```text
mvn test
```

Automated tests do not use the network. Kafka integration tests use embedded Kafka. FHIR calls in those tests use an in-process HTTP server or `MockRestServiceServer`.

The producer tests cover event creation, fixed event fields, correlation id, the `resourceId` key, blank id 400, broker failure 503, unexpected 500, and the absence of broker details in the HTTP body.

The consumer tests cover FHIR URL and `Accept`, mapping, duplicate suppression, FHIR 404, FHIR failure, a non-Observation payload, and the pipeline: consume, duplicate, and dead letter after three attempts.

## Manual setup

This lab adds `docker-compose.yml` only for Kafka. It does not change other infrastructure and it does not start the FHIR server.

```text
docker compose up -d
mvn -f clinical-event-consumer/pom.xml spring-boot:run
mvn -f clinical-event-producer/pom.xml spring-boot:run
```

FHIR server: `http://localhost:18080/fhir`, using the existing HAPI container when it is already available. Its H2 database has no volume, so logical ids are not stable. Create a temporary Patient and Observation when needed and record the ids HAPI assigns. The Observation JSON must be UTF-8 without a BOM.

Kafka console tools inside `lab-008-kafka` can republish an exact event or read the dead-letter topic. The public producer always generates a new `eventId`, so duplicate delivery is demonstrated with the console producer rather than a second HTTP call.

### Observed manual run

HAPI assigned `Patient/1000` and `Observation/1001` for this process lifetime. The Observation used code `LAB-GLUCOSE` from `https://example.org/fhir/CodeSystem/lab-008`, display `Glucose`, status `final`, value `92 mg/dL`, and subject `Patient/1000`. That code system is local to the lab.

Success: `POST /api/events/observations` with `resourceId` `1001` and `correlationId` `manual-lab-008` returned HTTP 202 and `eventId` `18022cd0-ed9b-434c-ab81-8ad3f4eb54c4`. The Kafka record key was `1001`. The value contained only event metadata and the resource reference. The consumer store then held `observationId` `1001`, code `LAB-GLUCOSE`, value `92`, unit `mg/dL`, patient reference `Patient/1000`, and the same correlation id.

Duplicate: the console producer wrote event id `manual-dup-008` twice, key `1001`. The consumer logged one `fhir-lookup`, one `processed`, and one `duplicate-ignored`. The store kept a single row for that event id.

Missing resource: `resourceId` `does-not-exist` was accepted with HTTP 202 (`52ece723-77f1-475b-a56a-5bb24e8e3d37`). The consumer logged three `fhir-lookup` calls and `deliveryAttempt` 1, 2, and 3, about 500 ms apart, then `event=dead-letter`. The dead-letter value was the original event. A later event for `Observation/1001` (`alive-after-dlt`) was still processed.

FHIR stopped: the producer still returned HTTP 202 for `8d78ab5f-fd06-4262-8313-5e81cabd172e`. The consumer logged three lookups and then a dead letter. `GET /api/processed-observations/{eventId}` returned 404 `not_found`. HAPI was started again afterwards. Its H2 store is empty after a container restart, so `Observation/1001` from the earlier process was gone until it was created again.

Kafka stopped: the same endpoint returned HTTP 503 `{"error":"broker_unavailable","message":"Event broker is not reachable"}`. The body did not include a broker address or an exception class. Kafka was started again and a later publish returned HTTP 202.

Blank `resourceId` returned HTTP 400 `invalid_resource_id` with message `Observation resource id must not be blank`.

### Closure verification

A later run repeated the same boundaries. `Observation/1001` and `Patient/1000` were still present. `correlationId` `closure-success` returned HTTP 202 for event `7cef36f1-abe8-4872-a7e6-610b489cf773` and the diagnostic store showed code `LAB-GLUCOSE`, value `92`, unit `mg/dL`, and patient reference `Patient/1000`.

The console producer wrote `closure-dup-008` twice. The log shows one `fhir-lookup`, one `processed`, and one `duplicate-ignored`.

`resourceId` `closure-missing` returned HTTP 202 for event `8c86b732-4f15-4eca-8e72-c46255b9994a`. The consumer logged three lookups and `deliveryAttempt` 1, 2, and 3, then `event=dead-letter`. The dead-letter record key was `closure-missing` and the value was the original event, including `correlationId` `closure-dlt`. The diagnostic endpoint returned 404 `not_found`.

With the FHIR server stopped, event `61191e5d-377f-4543-8eb7-beca236ba972` was still HTTP 202. Three lookups and `deliveryAttempt` 1, 2, and 3 ended in a dead letter. No processed row was stored. With Kafka stopped, the producer returned HTTP 503 `broker_unavailable` and message `Event broker is not reachable`. Both processes were started again. Because the FHIR server keeps no volume, `Observation/1001` had to be created again. A following publish, event `2a822d45-07ab-4feb-9867-8a00cf4e4cc3`, returned HTTP 202 and was processed with `correlationId` `closure-restored`.

## Limitations

- One event type and one read of Observation.
- One partition and one consumer instance.
- In-memory processing store and idempotency. Not durable.
- No database, transactional outbox, schema registry, Avro, Protobuf, or Kafka Streams.
- No authentication, service discovery, API gateway, or deployment manifests.
- No exactly-once claim.
- Producer idempotence is disabled so the lab does not imply a stronger delivery guarantee than the consumer implements.
- Name and quantity mapping use the first coding and `valueQuantity` only.
- A later lab can consume clinical events for additional processing. This lab stops at asynchronous FHIR resolution.
