# Lab #005 — Technical decisions

Statement kinds:

- **Laboratory decision:** a choice for this experiment.
- **Observed:** recorded against the reused HAPI process in this run.
- **General principle:** a reason for the boundary. Not a claim about every healthcare system.

## ADR-001 — Integration boundary between the application and FHIR

**Status:** Accepted.

**Context:** Lab #003 already implements Controller → Service → `FhirPatientClient` → HAPI. The client returns the API DTO, and that DTO still uses `resourceType` and `name` (`family`, `given`).

**Decision:** Add `HealthcareIntegrationService` between `PatientService` and `FhirPatientClient`. The client returns `FhirPatient`. The integration service translates FHIR failures and calls `PatientMapper`.

**Reasons:**

1. The application route and the FHIR read are different contracts. One class should own the translation.
2. `PatientService` checks the caller's id and then asks for a `HealthcarePatient`. It does not see `RestClient` or `HumanName`.
3. A renamed copy of Lab #003 would not change that dependency. The extra type is the change: `FhirPatient` is not `HealthcarePatient`.

**Consequences:** The controller handles `PatientNotFoundException` and `IntegrationException`. It does not handle `FhirClientException`. This is still one process, not a separate integration deployable.

## ADR-002 — Do not return the FHIR Patient as the API body

**Status:** Accepted.

**Decision:** `GET /api/healthcare/patients/{id}` returns `id`, `firstName`, `lastName`, `gender`, and `birthDate`.

`firstName` is `name[0].given[0]`. `lastName` is `name[0].family`. Other `name` entries and other `given` values are dropped. A missing `name` leaves both name fields null.

**Reasons:** The consumer in this laboratory does not need the FHIR document. Keeping `resourceType` and the `name` array would make the API follow the FHIR model, which is what Lab #003 already demonstrates.

**This is not a FHIR rule.** FHIR `Patient.name` is a list of `HumanName`. Selecting the first entry is a mapping choice for this API.

`resourceType` must be `Patient`. Anything else is an unexpected response (HTTP 500), not a mapped patient.

## ADR-003 — RestClient, no HAPI Java client

**Status:** Accepted.

**Decision:** Use Spring `RestClient` from `spring-boot-starter-web`. Do not add WebClient, `hapi-fhir-client`, or `hapi-fhir-structures-r4`.

**Reasons:**

1. The call is one blocking GET. Labs #003 and #004 already used `RestClient` for that.
2. Lab #001 uses HAPI `IGenericClient` and the HAPI R4 model. This laboratory needs the opposite: a small local model and an explicit map.
3. `fhir.base-url` is `FhirProperties`. The URL is not hardcoded in `FhirPatientClient`. `Accept` is `application/fhir+json`.

## ADR-004 — No messaging, no extra service, no local store

**Status:** Accepted for this laboratory.

**Decision:** Do not add RabbitMQ, Kafka, a database, or a second Spring process.

**Reasons:** The question in this laboratory is the boundary between an application contract and a FHIR read. Events and service splits are later steps. A database would store a copy of the patient and hide the live read.

HAPI remains the existing `hin-fhir-lab-001` container. This module does not change Lab #001 compose files and does not start another FHIR server.

## ADR-005 — Port 8087

**Status:** Accepted.

**Decision:** Listen on 8087. Labs #003 and #004 listen on 8086.

**Reason:** Those applications can stay running while this one is tried. They still share `http://localhost:18080/fhir`.

## Other decisions

| Topic | Decision |
| --- | --- |
| Blank id | `InvalidPatientIdException` in `PatientService` before the integration call. HTTP 400. |
| HAPI 404 | Client throws `FhirResourceNotFoundException`. Integration throws `PatientNotFoundException`. HTTP 404. `OperationOutcome` is not parsed. |
| Connection failure | `FhirClientException.unavailable` → `IntegrationException` → HTTP 502 `upstream_unavailable`. |
| Other HAPI HTTP error | HTTP 502 `upstream_error`. |
| Unreadable body | `UnexpectedFhirResponseException` → HTTP 500. |
| Tests | `MockRestServiceServer` for the client. Mockito for service and integration. `@WebMvcTest` plus `@Import(ApiExceptionHandler.class)`. No live HAPI in unit tests. |

## Observed on this HAPI process

Not stated as general FHIR behavior.

- After container start, `GET /Patient/1000` returned HTTP 404 and `HAPI-2001: Resource Patient/1000 is not known`.
- `POST /Patient` of a fictitious patient returned HTTP 201 and `Patient/1000` (`LAB-005-PATIENT-001`, family `Example`, given `Lucia`).
- `GET /Patient/does-not-exist` returned HTTP 404 and `HAPI-2001`.
- With `fhir.base-url` pointed at `127.0.0.1:19999`, this application returned HTTP 502. HAPI on port 18080 was still up. That 502 is this process failing to connect, not a HAPI status code.

H2 has no volume. The logical id `1000` belongs to this container life only.

## Closure review

**Status:** Accepted. No production code change.

The review confirmed the boundary already present in this module:

- `FhirPatient` is the external read model. `HealthcarePatient` is the API body and does not reference HAPI types.
- `PatientMapper` is the only translation from one to the other.
- `FhirPatientClient` performs the HTTP call. `HealthcareIntegrationService` translates not-found and upstream failures. `PatientService` validates the id and uses `HealthcarePatient`. `PatientController` depends on `PatientService` only.
- This remains one Spring Boot process. It is not a microservice deployment.

Next technical step stays FHIR Profiles and Extensions.
