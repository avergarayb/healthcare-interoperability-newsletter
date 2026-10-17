# Synchronous microservice boundary for a patient read

This lab shows a small, synchronous split between an application API and a FHIR integration service. It is a simplified architecture used to demonstrate boundaries. It is not a universal healthcare architecture.

## Objective

Read one patient through two independently runnable Spring Boot applications:

```text
Client
  |
  v
Patient Service :8089
  |
  | internal application contract
  v
FHIR Integration Service :8090
  |
  | FHIR
  v
FHIR Server :18080
```

The client calls only the patient service. The patient service calls only the integration service. Only the integration service calls the FHIR server.

## Problem

A FHIR `Patient` carries resource structure, representation headers, and FHIR error payloads such as `OperationOutcome`. If every service depends on that model, a FHIR change spreads through the application API.

This lab keeps FHIR knowledge in one process. The public API stays a small application contract.

## Responsibilities

### Patient service (`patient-service`, port 8089)

- Exposes `GET /api/patients/{id}`.
- Rejects a blank id before any downstream call.
- Calls the integration service through `FhirIntegrationClient`.
- Owns `PatientResponse`.
- Translates downstream failures into its own status codes and messages.

It does not parse a FHIR `Patient`, does not read `fhir.base-url`, and does not depend on HAPI FHIR.

The route `/internal/fhir/patients/{id}` is the name of the integration endpoint. Knowing that path does not mean the patient service understands a FHIR resource.

### FHIR integration service (`fhir-integration-service`, port 8090)

- Exposes `GET /internal/fhir/patients/{id}`.
- Calls `GET {fhir.base-url}/Patient/{id}` with `Accept: application/fhir+json`.
- Parses only the FHIR fields this lab needs.
- Maps them to `IntegrationPatientResponse`.
- Translates FHIR HTTP errors, connection failures, and unreadable payloads into a stable internal contract.

The HTTP response of this service is not a FHIR `Patient`. `resourceType`, `meta`, `identifier`, `extension`, and `OperationOutcome` stay inside this process.

## Why only one service knows FHIR

`patient-service/pom.xml` depends on Spring Web and Spring Boot Test. It has no HAPI artifact. Its Java types are `id`, `firstName`, `lastName`, `gender`, and `birthDate`.

`fhir-integration-service` is the FHIR-aware boundary. It still does not use the HAPI client. The subset of `Patient` required here is small, so Jackson records (`FhirPatient`, `FhirHumanName`) are enough. Those records are not part of the HTTP response.

## Contracts

### Public contract

`GET http://localhost:8089/api/patients/{id}`

```json
{
  "id": "1000",
  "firstName": "Lucia",
  "lastName": "Example",
  "gender": "female",
  "birthDate": "1988-03-20"
}
```

Java type: `com.healthcare.interoperability.patientservice.dto.PatientResponse`.

### Internal contract

`GET http://localhost:8090/internal/fhir/patients/{id}`

The JSON fields match the public contract. The Java type is different and lives only in the integration service: `IntegrationPatientResponse`.

There is no shared model library. The two services can change independently. Structural similarity of the JSON is a lab choice, not compile-time coupling.

### FHIR contract

`GET http://localhost:18080/fhir/Patient/{id}`

Header: `Accept: application/fhir+json`

The integration client models only:

- `resourceType`
- `id`
- `name` (`family`, `given`)
- `gender`
- `birthDate`

Unknown JSON fields are ignored. A body that is not JSON, or whose `resourceType` is not `Patient`, becomes `502 unreadable_payload`.

## Request flow

```text
PatientController
  -> PatientService
  -> FhirIntegrationClient
  -> GET /internal/fhir/patients/{id}

FhirPatientController
  -> FhirPatientIntegrationService
  -> FhirPatientClient
  -> GET {fhir.base-url}/Patient/{id}
  -> PatientMapper
  -> IntegrationPatientResponse
```

Controllers do not call HTTP clients. `PatientService` validates the id and delegates. Mapping from FHIR to the integration DTO happens only in the integration service.

## Mapping

These rules are specific to this lab. They are not universal FHIR name rules.

| Integration field | Source |
| --- | --- |
| `id` | `Patient.id` |
| `firstName` | first `given` value of the first `HumanName` |
| `lastName` | `family` of the first `HumanName` |
| `gender` | `Patient.gender` |
| `birthDate` | `Patient.birthDate` |

If `name` is missing, `firstName` and `lastName` are null. The mapper does not invent an id.

The patient service does not map again. It deserializes the integration JSON into its own `PatientResponse` because the two JSON contracts were aligned on purpose.

## Error translation

Each boundary replaces the downstream body. Stack traces, exception class names, HAPI diagnostics, and internal URLs are not copied into the response.

### FHIR integration service

| Condition | HTTP | `error` | `message` |
| --- | --- | --- | --- |
| Blank id | 400 | `invalid_id` | `Patient id must not be blank` |
| FHIR HTTP 404 | 404 | `not_found` | `Patient {id} was not found` |
| FHIR HTTP 5xx | 502 | `upstream_error` | `FHIR server returned an unexpected error` |
| FHIR connection failure | 502 | `upstream_unavailable` | `FHIR server is not reachable` |
| Unreadable FHIR payload | 502 | `unreadable_payload` | `FHIR response could not be read` |
| Unexpected error | 500 | `internal_error` | `An unexpected error occurred` |

A FHIR 404 `OperationOutcome` is consumed by `FhirPatientClient`. The caller receives the 404 row above, not the FHIR payload.

### Patient service

| Condition | HTTP | `error` | `message` |
| --- | --- | --- | --- |
| Blank id | 400 | `invalid_id` | `Patient id must not be blank` |
| Integration service returns 404 | 404 | `not_found` | `Patient {id} was not found` |
| Integration service cannot be reached | 503 | `integration_unavailable` | `Integration service is not reachable` |
| Integration service returns another error status, or an unreadable body | 502 | `upstream_error` | `Patient data is temporarily unavailable` |
| Unexpected error | 500 | `internal_error` | `An unexpected error occurred` |

The 503 is intentional. The patient service is up, but the required internal service is not. That is different from a FHIR failure that the integration service already translated.

The patient service does not forward the integration response body. A FHIR outage therefore does not appear to the client as a FHIR message. It appears as the patient service's own 502.

## Failure boundaries

```text
Client
  |
  v
Patient Service
  |
  X  integration service unavailable  ->  HTTP 503 integration_unavailable
```

```text
Client
  |
  v
Patient Service
  |
  v
FHIR Integration Service
  |
  X  FHIR server unavailable  ->  integration HTTP 502 upstream_unavailable
                                ->  patient service HTTP 502 upstream_error
```

Splitting the call does not remove either failure. It makes the failing hop visible as a different contract.

## Configuration

`patient-service/src/main/resources/application.yml`

- `server.port`: `8089`
- `integration.base-url`: `http://localhost:8090`
- `integration.connect-timeout`: `2s`
- `integration.read-timeout`: `5s`

`fhir-integration-service/src/main/resources/application.yml`

- `server.port`: `8090`
- `fhir.base-url`: `http://localhost:18080/fhir`
- `fhir.connect-timeout`: `2s`
- `fhir.read-timeout`: `5s`

Both HTTP clients use `RestClient` and `SimpleClientHttpRequestFactory`. There are no retries and no circuit breaker.

## Security

This lab has no authentication.

In a real environment, service-to-service communication and FHIR access would require authentication and authorization.

## Observability

Correlation ids, distributed tracing, metrics, and centralized logs are production concerns. They are not implemented here. The lab does not claim that a request can be traced across the two processes.

## Tests

Automated tests do not use the network, a live FHIR server, or the other microservice.

From this directory:

```text
mvn test
```

`patient-service` covers a successful read, a blank id, a correct integration response, integration 404, integration connection failure (503), integration 5xx or unreadable body (502), an unexpected error (500), and the absence of downstream details in the public body.

`fhir-integration-service` covers FHIR 200 mapped to the internal DTO, FHIR 404, FHIR 5xx, FHIR connection failure, an unreadable FHIR body, mapper rules, a blank id, an unexpected error, the exact URL `{base}/Patient/{id}`, and `Accept: application/fhir+json`.

## Manual setup

Start the three processes separately. This lab does not add Docker Compose.

1. FHIR server on `http://localhost:18080/fhir`. This workspace uses the existing HAPI container `hin-fhir-lab-001` (`hapiproject/hapi:v8.12.0-1`, host port 18080). Its H2 database has no volume, so logical ids are not stable across container lifecycles. Do not treat a previously seen id such as `1000` as permanent.
2. Integration service:

```text
mvn -f fhir-integration-service/pom.xml spring-boot:run
```

3. Patient service:

```text
mvn -f patient-service/pom.xml spring-boot:run
```

If no suitable `Patient` exists, create a temporary one with `POST /fhir/Patient` and record the id HAPI assigns. That id belongs to the current H2 lifecycle only.

The JSON body must be UTF-8 without a BOM.

## Manual checks

Use an id that exists in the current HAPI lifecycle. Replace `{id}` below.

| Check | Call | Expected |
| --- | --- | --- |
| A. Full chain | `GET http://localhost:8089/api/patients/{id}` | 200 application DTO |
| B. Integration only | `GET http://localhost:8090/internal/fhir/patients/{id}` | 200 integration DTO, no `resourceType` |
| C. Missing patient through the chain | `GET http://localhost:8089/api/patients/does-not-exist` | 404 `not_found`, no `OperationOutcome` |
| D. Missing patient on HAPI | `GET http://localhost:18080/fhir/Patient/does-not-exist` | HAPI's own 404 `OperationOutcome`. The upper services must not return that body |
| E. Stop the integration service, keep the patient service | `GET http://localhost:8089/api/patients/{id}` | 503 `integration_unavailable` |
| F. Stop HAPI, keep both Spring services | `GET http://localhost:8089/api/patients/{id}` | patient service 502 `upstream_error`. Direct call to port 8090 returns 502 `upstream_unavailable` |

Observed results from the local run are in [docs/decisions.md](docs/decisions.md) under "Manual evidence".

## Limitations

- Two services and one read-by-id operation.
- Synchronous HTTP only.
- Neither Spring service persists data. There is no shared database, service discovery, API gateway, or deployment manifest.
- No authentication, retries, or circuit breaker.
- No correlation id or tracing.
- Name mapping uses the first `HumanName` only.
- Jackson ignores FHIR fields that this lab does not map. That is not a statement that those fields are unimportant.
- Timeouts are fixed at 2 seconds to connect and 5 seconds to read. They are not tuned for a production network.
- Event-driven communication is intentionally absent. A later lab can show asynchronous integration without replacing this synchronous boundary.

## Closure

Reviewed on 2026-10-16. `mvn clean test` from this directory reported 14 tests in `patient-service`, 17 in `fhir-integration-service`, and 31 overall, with 0 failures, 0 errors, and 0 skipped.

Production code in `patient-service` does not model a FHIR `Patient`, does not depend on HAPI, and does not configure `fhir.base-url`. Its tests mention FHIR error text only to assert that the public body omits it.

A later local run, after the H2 database had been reset, created identifier `LAB-007-PATIENT-002`. HAPI assigned logical id `1000` again. That repetition belongs to an empty database lifecycle and does not make the id stable. The same run reproduced 200 on both services, 404 `not_found` through the patient service, 503 when port 8090 was stopped, and the 502 pair when the FHIR server was stopped.
