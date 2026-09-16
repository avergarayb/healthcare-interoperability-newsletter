# Lab #003 — Technical decisions

Statement kinds:

- **Laboratory decision:** a choice for this experiment.
- **Official documentation:** Spring / FHIR documentation used as justification.
- **Observed:** recorded in this HTTP phase.

## ADR-001 — RestClient instead of WebClient and HAPI Generic Client

**Status:** Accepted for this phase.

**Context:** Lab #003 must call `GET {fhir.base-url}/Patient/{id}` from Spring Boot 3.5.16 and expose `GET /api/fhir/patients/{id}`. Lab #001 already used HAPI Generic Client (`IGenericClient`) in a different module.

**Decision:** Use Spring `RestClient` from `spring-boot-starter-web`. Do not add WebClient / WebFlux. Do not add `hapi-fhir-client` or `hapi-fhir-structures-r4`.

**Reasons:**

1. The call is a single blocking GET. `RestClient` is the current Spring MVC HTTP client for that style.
2. WebClient would pull a reactive stack that this laboratory does not need.
3. HAPI Generic Client would deserialize to `org.hl7.fhir.r4.model.Patient`. This phase maps a small DTO (`resourceType`, `id`, `name`, `gender`, `birthDate`) to show the difference between a FHIR resource and an application contract.
4. `Accept: application/fhir+json` is set on the `RestClient` bean so the FHIR media type is explicit in application code.

**Consequences:**

- Extra FHIR fields (`meta`, `identifier`, …) are ignored by Jackson, not rejected.
- Search and `Bundle` handling remain a later phase.
- This module is not a replacement for Lab #001’s HAPI client.

## Other laboratory decisions

| Topic | Decision |
| --- | --- |
| Configuration | Typed `FhirProperties` (`fhir.base-url`). No hardcoded HAPI URL in the client. |
| Layers | Controller → Service → `FhirPatientClient` → `RestClient`. |
| Client method | `getPatientById(String patientId)`. |
| Names | `HumanNameResponse` (`family`, `given`). All `name` entries from FHIR are kept. |
| Blank id | `InvalidPatientIdException` → HTTP 400. Checked in `PatientService` before the HTTP call. |
| Missing Patient | HAPI HTTP 404 → `FhirResourceNotFoundException` → HTTP 404. |
| Other HAPI HTTP errors | `FhirClientException.httpError` → HTTP 502 (`upstream_error`). |
| Connection failure | `FhirClientException.unavailable` → HTTP 502 (`upstream_unavailable`). |
| Tests | `MockRestServiceServer` on `RestClient.Builder` for the client; `@WebMvcTest` + `@MockitoBean` for the controller. No live HAPI in unit tests. |
| Persistence of HAPI data | H2 without volume. `Patient/1000` can disappear after a container recreate. |
