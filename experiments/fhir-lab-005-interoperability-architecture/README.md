# FHIR Lab #005 — Interoperability architecture

Spring Boot application that reads one FHIR R4 `Patient` from the reused HAPI server and returns an application contract. The consumer does not call HAPI.

Previous laboratory: FHIR Search / Bundle handling (Lab #004). Labs #001–#004 are not modified by this experiment.

This laboratory is a small teaching architecture. It does not claim that production healthcare systems use this exact shape.

## 1. Objective

Expose:

```text
GET http://localhost:8087/api/healthcare/patients/{id}
```

so that a consumer receives an application patient, while this process calls:

```text
GET {fhir.base-url}/Patient/{id}
```

Example observed in this run: `GET http://localhost:8087/api/healthcare/patients/1000` read `GET http://localhost:18080/fhir/Patient/1000`.

## 2. Problem

Lab #003 already calls HAPI with `RestClient` and returns a subset of the FHIR Patient (`resourceType`, `name.family`, `name.given`). That response is still the FHIR shape.

A backend that sits between an application and an external FHIR server has a second contract. The application asks for a patient. FHIR answers with a resource. Those models change for different reasons. This laboratory puts that translation in one place.

## 3. Scope

| In scope | Out of scope |
| --- | --- |
| Read one Patient by logical id | FHIR Search, `Bundle`, pagination |
| Application DTO distinct from the FHIR resource | Profiles, extensions, terminology |
| Mapping of the first `HumanName` | OAuth2 / SMART / JWT |
| HTTP 200 / 400 / 404 / 502 / 500 | Database, local FHIR persistence |
| Tests that do not call live HAPI | Microservices, messaging, events |
| Reuse of the existing HAPI process | A second FHIR server, AI |

## 4. Architecture

```text
Client / application
        |
        v
GET /api/healthcare/patients/{id}     this process, port 8087
        |
        v
PatientController
        |
        v
PatientService                        application request check
        |
        v
HealthcareIntegrationService          FHIR errors and mapping
        |
        +-- PatientMapper
        |
        v
FhirPatientClient                     HTTP only
        |
        v
GET {fhir.base-url}/Patient/{id}      HAPI FHIR, port 18080
```

| Layer | Class | Responsibility |
| --- | --- | --- |
| API | `PatientController` | Application route. Returns `HealthcarePatient`. |
| Application | `PatientService` | Rejects a blank id. Does not read FHIR JSON. |
| Integration | `HealthcareIntegrationService` | Calls the FHIR client. Translates client failures. |
| Mapping | `PatientMapper` | `FhirPatient` → `HealthcarePatient`. |
| FHIR client | `FhirPatientClient` | `GET /Patient/{id}`. Returns `FhirPatient`. |
| External model | `FhirPatient`, `FhirHumanName` | Fields read from the FHIR JSON. Not the API body. |

`FhirPatientClient` does not build `firstName` or `lastName`. `PatientController` does not import the FHIR model.

## 5. Request flow

1. The consumer calls `GET /api/healthcare/patients/{id}`.
2. `PatientService` rejects a null or blank id before any HTTP call.
3. `HealthcareIntegrationService` asks `FhirPatientClient` for that id.
4. The client sends `GET {fhir.base-url}/Patient/{id}` with `Accept: application/fhir+json`.
5. Jackson maps the JSON onto `FhirPatient`. Extra FHIR fields are ignored.
6. `PatientMapper` checks `resourceType=Patient` and builds `HealthcarePatient`.
7. The controller returns that object. The FHIR document is not the response body.

## 6. Application contract

```http
GET /api/healthcare/patients/1000
```

Observed HTTP **200**:

```json
{
  "id": "1000",
  "firstName": "Lucia",
  "lastName": "Example",
  "gender": "female",
  "birthDate": "1988-03-20"
}
```

There is no `resourceType` and no `name` array. That is a choice of this API, not a FHIR rule.

## 7. FHIR contract

The external interaction used here is FHIR R4 read:

```http
GET http://localhost:18080/fhir/Patient/1000
Accept: application/fhir+json
```

Observed HTTP **200** from HAPI 8.12.0 (`X-Powered-By: HAPI FHIR 8.12.0 REST Server (FHIR Server; FHIR 4.0.1/R4)`):

```json
{
  "resourceType": "Patient",
  "id": "1000",
  "meta": {
    "versionId": "1",
    "lastUpdated": "2026-09-28T03:19:51.957+00:00"
  },
  "identifier": [ {
    "system": "https://example.org/fhir/identifier/lab-005",
    "value": "LAB-005-PATIENT-001"
  } ],
  "name": [ {
    "family": "Example",
    "given": [ "Lucia" ]
  } ],
  "gender": "female",
  "birthDate": "1988-03-20"
}
```

`meta` and `identifier` stay on the FHIR resource. They are not copied into `HealthcarePatient`.

A missing id on this HAPI process is HTTP **404** and `OperationOutcome` (`HAPI-2001`). This application does not parse `OperationOutcome`. It maps that HTTP status to its own 404 body.

## 8. Integration layer

`HealthcareIntegrationService` is the boundary between the two contracts.

It exists so that:

- the application service depends on `HealthcarePatient`, not on `RestClient` or `HumanName`;
- a FHIR 404 becomes `PatientNotFoundException` before it reaches the controller;
- a connection failure or another FHIR HTTP error becomes `IntegrationException`;
- a body that is not a Patient fails in the mapper, not in the controller.

Lab #003 stops at Controller → Service → client, and the client returns the API DTO. Changing `Patient.name` there changes the API. Here the client returns `FhirPatient`. Only `PatientMapper` chooses `firstName` and `lastName`.

## 9. Mapping

Laboratory decision, not a FHIR requirement:

| FHIR | Application |
| --- | --- |
| `id` | `id` |
| `name[0].given[0]` | `firstName` |
| `name[0].family` | `lastName` |
| `gender` | `gender` |
| `birthDate` | `birthDate` |

If `name` is missing, `firstName` and `lastName` are null and the rest of the patient is still returned. Further `name` entries and further `given` values are not copied. `resourceType` other than `Patient` is rejected.

## 10. Error handling

| Condition | HTTP | `error` |
| --- | --- | --- |
| Patient read and mapped | 200 | — |
| Blank id | 400 | `invalid_id` |
| HAPI 404 | 404 | `not_found` |
| HAPI not reachable | 502 | `upstream_unavailable` |
| Other HAPI HTTP error | 502 | `upstream_error` |
| Unreadable body, non-Patient resource, or other unexpected failure | 500 | `internal_error` |

The 500 body is `{"error":"internal_error","message":"An unexpected error occurred"}`. Exception text and stack traces are not written into the JSON.

## 11. Configuration

`src/main/resources/application.yml`:

```yaml
server:
  port: 8087

fhir:
  base-url: http://localhost:18080/fhir
```

`RestClientConfig` sets `baseUrl` from `FhirProperties`. The client method only appends `/Patient/{id}`.

Port **8087** is used because Labs #003 and #004 bind **8086**. One of those processes and this one can run at the same time. They still share one HAPI listener on port 18080.

Override the FHIR base URL without editing code:

```text
mvn spring-boot:run "-Dspring-boot.run.arguments=--fhir.base-url=http://127.0.0.1:19999/fhir"
```

## 12. Build and run

From `experiments/fhir-lab-005-interoperability-architecture`:

```text
mvn clean test
mvn spring-boot:run
```

Java 21. Maven. No wrapper. Spring Boot **3.5.16**. Dependencies: `spring-boot-starter-web`, `spring-boot-starter-test`. Jackson comes with Spring Web. No HAPI Java client.

HAPI is the existing container `hin-fhir-lab-001` (`hapiproject/hapi:v8.12.0-1`, host `18080`). This laboratory does not add a compose file.

## 13. Automated tests

`mvn clean test` on 2026-09-27, repeated at closure:

```text
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-09-27T22:26:11-05:00
```

| Test | What it checks |
| --- | --- |
| `FhirPatientClientTest` | URL, `Accept`, 200, 404, HTTP 500, connection failure, unreadable body |
| `PatientMapperTest` | First name only, missing name, non-Patient |
| `HealthcareIntegrationServiceTest` | Mapping, client delegation, 404 / unavailable / upstream translation |
| `PatientServiceTest` | Blank id does not call integration; a real id is delegated |
| `PatientControllerTest` | 200 application JSON, 404, 502, 500 without internal text, 400 |
| `HealthcareArchitectureApplicationTests` | Context loads |

`MockRestServiceServer` and `@WebMvcTest` do not call HAPI. `@WebMvcTest` imports `ApiExceptionHandler`.

## 14. Manual tests

Docker Desktop was not running. It was started, then the existing container `hin-fhir-lab-001` was started. No second HAPI was created. Metadata returned HTTP 200 after several empty replies while the process was still starting.

H2 has no volume. At startup `GET /Patient/1000` returned HTTP **404** and `HAPI-2001: Resource Patient/1000 is not known`. A fictitious Patient was posted so the read path could be observed. HAPI assigned `Patient/1000` on this empty store. That id is not stable across a container recreate.

The outbound call from this application was not captured with a proxy. `FhirPatientClientTest` expects `GET http://localhost:18080/fhir/Patient/{id}` and `Accept: application/fhir+json`. The manual results below are the consumer call, the FHIR resource stored at that URL, and the application body.

### Consumer — patient found

```text
curl.exe -sS -D - -H "Accept: application/json" http://localhost:8087/api/healthcare/patients/1000
```

**Observed:** HTTP **200**

```json
{"id":"1000","firstName":"Lucia","lastName":"Example","gender":"female","birthDate":"1988-03-20"}
```

### FHIR server — same patient

```text
curl.exe -sS -D - -H "Accept: application/fhir+json" http://localhost:18080/fhir/Patient/1000
```

**Observed:** HTTP **200**, `application/fhir+json`, body in section 7. `identifier` and `meta` are present there and absent from the application body.

### Consumer — unknown id

```text
curl.exe -sS -D - -H "Accept: application/json" http://localhost:8087/api/healthcare/patients/does-not-exist
```

**Observed:** HTTP **404**

```json
{"error":"not_found","message":"Patient does-not-exist was not found"}
```

Direct HAPI `GET /Patient/does-not-exist` returned HTTP **404**, `OperationOutcome`, `HAPI-2001: Resource Patient/does-not-exist is not known`.

### Consumer — blank id

```text
curl.exe -sS -D - "http://localhost:8087/api/healthcare/patients/%20"
```

**Observed:** HTTP **400**

```json
{"error":"invalid_id","message":"Patient id must not be blank"}
```

### Consumer — FHIR base URL unreachable

The application was restarted with `--fhir.base-url=http://127.0.0.1:19999/fhir`. HAPI stayed on port 18080.

```text
curl.exe -sS -D - http://localhost:8087/api/healthcare/patients/1000
```

**Observed:** HTTP **502**

```json
{"error":"upstream_unavailable","message":"FHIR server is not reachable"}
```

## 15. Difficulties

| Issue | What happened |
| --- | --- |
| Docker daemon down | `docker` could not open `dockerDesktopLinuxEngine`. Docker Desktop was started. Server version 29.4.0. |
| HAPI not ready | The first metadata calls returned an empty reply. Try 12 returned HTTP 200. |
| Empty H2 | `Patient/1000` was unknown until the fictitious POST in section 14. |

`mvn clean test` did not fail.

## 16. Limitations

This laboratory does not implement:

- search, `Bundle`, or pagination
- `_count` or following `Bundle.link`
- profiles or extensions
- authentication
- local persistence
- more than one resource type
- more than the first `HumanName` and the first `given`
- parsing of `OperationOutcome`
- microservices or event publication

`X-Cache` and search behavior recorded in Lab #004 are not retested here. Logical ids on this HAPI process are not portable. The posted patient is fictitious (`LAB-005-PATIENT-001`). It is not Lab #001 Ana and it is not the Lab #002 Lucia resource from an earlier H2 life.

The layering is a simplification used to show an integration boundary. It is not a statement that every healthcare backend uses these class names or this number of layers.

## 17. Status

**Lab #005 is closed.** The closure review did not change production code. The path remains `GET /api/healthcare/patients/{id}` → `PatientService` → `HealthcareIntegrationService` → `FhirPatientClient` → `GET {fhir.base-url}/Patient/{id}`.

Closure check against the running process, 2026-09-27:

- `GET /api/healthcare/patients/1000` returned HTTP 200 and `HealthcarePatient` (`id`, `firstName`, `lastName`, `gender`, `birthDate`). The body had no `resourceType`, `meta`, or `identifier`.
- `GET /api/healthcare/patients/does-not-exist` returned HTTP 404 `not_found`. The body did not include `HAPI-2001`, a stack trace, or an exception class name.

The generic HTTP 500 body is covered by `PatientControllerTest`, which expects `An unexpected error occurred` and rejects an internal exception message.

## 18. Next laboratory

Next technical step: FHIR Profiles and Extensions.

Not implemented here: FHIR with microservices, and FHIR with event-driven processing.

## Coordinates

| Field | Value |
| --- | --- |
| Group | `com.healthcare.interoperability` |
| Artifact | `lab-005-interoperability-architecture` |
| Base package | `com.healthcare.interoperability.integration` |
| Local endpoint | `GET /api/healthcare/patients/{id}` |
| FHIR endpoint | `GET {fhir.base-url}/Patient/{id}` |
| Port | 8087 |

Decision record: [docs/decisions.md](docs/decisions.md).
