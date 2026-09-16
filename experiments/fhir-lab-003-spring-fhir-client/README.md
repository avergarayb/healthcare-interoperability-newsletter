# FHIR Lab #003 — Spring FHIR Client

Spring Boot application that reads a FHIR R4 `Patient` from the reused HAPI server and exposes a smaller application API.

Lab #001 and Lab #002 remain closed. This laboratory does not modify those experiments, their results, Java client, or infrastructure.

## 1. Objective

Consume:

```text
GET {fhir.base-url}/Patient/{id}
```

from Java 21 / Spring Boot **3.5.16** using Spring `RestClient`, and expose:

```text
GET http://localhost:8086/api/fhir/patients/{id}
```

Example: `GET http://localhost:8086/api/fhir/patients/1000` → `GET http://localhost:18080/fhir/Patient/1000`.

## 2. Scope of this phase

| In scope | Out of scope |
| --- | --- |
| `RestClient` with `fhir.base-url` and `Accept: application/fhir+json` | WebClient / WebFlux |
| Controller → Service → FHIR client | HAPI FHIR Client / HAPI structures |
| Small Patient DTOs | Full FHIR Patient model |
| HTTP 200 / 400 / 404 / 502 / 500 | OAuth2 / SMART / security |
| Tests that do not call live HAPI | FHIR Search / `Bundle` |
| | Database, RabbitMQ, Docker, AI |

## 3. Technologies

| Item | Version / value |
| --- | --- |
| Language | Java 21 |
| Build | Maven (no wrapper) |
| Spring Boot | **3.5.16** |
| HTTP client | Spring `RestClient` (`spring-boot-starter-web`) |
| Test | `spring-boot-starter-test` |
| JSON | Jackson, via Spring Web |
| FHIR | R4 on HAPI 8.12.0 |

No extra Maven dependencies were added.

## 4. Architecture and request flow

```text
HTTP client
    |
    v
GET /api/fhir/patients/{id}          (this application, port 8086)
    |
    v
PatientController
    |
    v
PatientService  (rejects blank id)
    |
    v
FhirPatientClient.getPatientById  (RestClient)
    |
    v
GET {fhir.base-url}/Patient/{id}     (HAPI, port 18080)
    |
    v
FHIR JSON Patient
    |
    v
PatientResponse  (subset of fields)
```

| Layer | Class | Responsibility |
| --- | --- | --- |
| Config | `RestClientConfig`, `FhirProperties` | `RestClient` from `fhir.base-url` |
| Client | `FhirPatientClient` | `GET /Patient/{id}`, HTTP error mapping |
| Service | `PatientService` | Blank-id check, then delegate |
| Controller | `PatientController` | `GET /api/fhir/patients/{id}` |
| Errors | `ApiExceptionHandler` | JSON `{ error, message }`, no stack traces |

The application path is not a FHIR endpoint. It is not `/fhir/Patient/{id}`.

## 5. Configuration

```yaml
server:
  port: 8086

spring:
  application:
    name: lab-003-spring-fhir-client

fhir:
  base-url: http://localhost:18080/fhir
```

`FhirPatientClient` only appends `/Patient/{id}`. The HAPI URL is not hardcoded in the client.

## 6. DTOs

`PatientResponse`:

- `resourceType`
- `id`
- `name` (`HumanNameResponse`: `family`, `given`)
- `gender`
- `birthDate`

If FHIR returns several `name` entries, the list is kept as returned. No extra names are invented.

Jackson ignores other FHIR fields (`meta`, `identifier`, …). They are not copied into the application body.

## 7. Error handling

| Condition | Application HTTP | `error` |
| --- | --- | --- |
| Blank `{id}` | **400** | `invalid_id` |
| HAPI 404 | **404** | `not_found` |
| HAPI other 4xx/5xx | **502** | `upstream_error` |
| Connection failure | **502** | `upstream_unavailable` |
| Unexpected | **500** | `internal_error` |

Errors are not returned as HTTP 200.

## 8. Build, test, and run

From this directory:

```text
mvn clean test
mvn spring-boot:run
```

Unit tests do not call HAPI (`MockRestServiceServer` and mocks).

## 9. Automated tests (executed)

Command: `mvn clean test`

First run failed: `MockRestResponseCreators.withException` requires `IOException`, not `ResourceAccessException`. The test was changed to `new IOException("Connection refused")`.

Second run **Observed:** `BUILD SUCCESS`. Tests run: **12**, Failures: 0, Errors: 0, Skipped: 0.

| Class | Tests | Coverage |
| --- | --- | --- |
| `FhirPatientClientTest` | 5 | 200 + Accept/`baseUrl`; several names; 404; HAPI 500; connection `IOException` |
| `PatientControllerTest` | 5 | 200, 404, 502 unavailable, 502 HTTP error, 400 blank id |
| `PatientServiceTest` | 1 | Blank / empty / null id does not call the FHIR client |
| `FhirClientApplicationTests` | 1 | Context loads |

## 10. Manual tests (executed)

HAPI already held `Patient/1000` (Lucia / `LAB-002-PATIENT-001`) from the earlier restore in this laboratory.

Application: `mvn spring-boot:run` — Tomcat on **8086**.

### Application — existing patient

```text
curl.exe -i -sS -m 15 http://localhost:8086/api/fhir/patients/1000
```

**Observed:** HTTP **200**

```json
{"resourceType":"Patient","id":"1000","name":[{"family":"Example","given":["Lucia"]}],"gender":"female","birthDate":"1988-03-20"}
```

### Application — missing patient

```text
curl.exe -i -sS -m 15 http://localhost:8086/api/fhir/patients/does-not-exist
```

**Observed:** HTTP **404**

```json
{"error":"not_found","message":"Patient does-not-exist was not found"}
```

### Application — blank id

```text
curl.exe -i -sS -m 15 "http://localhost:8086/api/fhir/patients/%20"
```

**Observed:** HTTP **400**

```json
{"error":"invalid_id","message":"Patient id must not be blank"}
```

### HAPI — same instance

```text
curl.exe -i -sS -m 15 -H "Accept: application/fhir+json" http://localhost:18080/fhir/Patient/1000
```

**Observed:** HTTP **200**, `application/fhir+json`, body includes `meta` and `identifier` that the application DTO omits.

### Application — HAPI unreachable

HAPI was left running. The application was restarted with:

```text
mvn spring-boot:run "-Dspring-boot.run.arguments=--fhir.base-url=http://127.0.0.1:19999/fhir"
```

```text
curl.exe -i -sS -m 15 http://localhost:8086/api/fhir/patients/1000
```

**Observed:** HTTP **502**

```json
{"error":"upstream_unavailable","message":"FHIR server is not reachable"}
```

## 11. Difficulties

| Issue | Fix |
| --- | --- |
| `withException(ResourceAccessException)` did not compile | Use `IOException` |
| `@WebMvcTest` does not load `@RestControllerAdvice` | `@Import(ApiExceptionHandler.class)` |

## 12. Limitations

- Logical id `1000` is not portable across HAPI / H2 recreates.
- Read by id only. No search, no `Bundle`, no other resource types.
- DTO is a subset, not a FHIR profile.
- No authentication.
- Data are fictitious. Lucia / `LAB-002-PATIENT-001` is not Lab #001 Ana / `LAB-001-0001`.

## 13. Next technical step

Implement FHIR Search and process a `searchset` `Bundle` (for example `Patient?identifier=...`), still without HAPI Generic Client unless a later decision changes that.

## 14. Coordinates

| Field | Value |
| --- | --- |
| Group | `com.healthcare.interoperability` |
| Artifact | `lab-003-spring-fhir-client` |
| Base package | `com.healthcare.interoperability.fhirclient` |
| Local endpoint | `GET /api/fhir/patients/{id}` |
| FHIR endpoint | `GET {fhir.base-url}/Patient/{id}` |

Decision record: [docs/decisions.md](docs/decisions.md).
