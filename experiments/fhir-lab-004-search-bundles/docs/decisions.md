# Lab #004 — Technical decisions

## ADR-001 — RestClient and a minimal Bundle DTO

**Status:** Accepted.

**Decision:** Search with Spring `RestClient`. Do not add `hapi-fhir-client`, `hapi-fhir-structures-r4`, or WebClient.

**Reasons:**

1. The call is one blocking GET with a query parameter. `RestClient` already did that style of call in Lab #003.
2. The learning target is the `searchset` Bundle, not the HAPI Java model.
3. Jackson maps only `resourceType`, `type`, `total`, `entry` (`fullUrl`, Patient subset), and `link` (`relation`, `url`).

**Consequences:** `meta`, `identifier`, and `entry.search` are ignored. `OperationOutcome` is not parsed.

## ADR-002 — Application response is not the Bundle

**Status:** Accepted.

**Decision:** `GET /api/fhir/patients/search` returns `total`, `patients`, and `links`.

`total` is `Bundle.total`. `patients` are Patient resources from `entry`. `links` are copied so `Bundle.link` is visible without returning the Bundle document.

An empty `searchset` stays HTTP 200. It is not turned into 404. Lab #003 uses 404 for `GET /Patient/{id}` when the id is unknown. Those are different FHIR interactions.

## ADR-003 — Identifier is forwarded as supplied

**Status:** Accepted.

**Decision:** The `identifier` query parameter is sent with `UriBuilder.queryParam`. The client does not prepend a system and does not hardcode `LAB-002-PATIENT-001` or `LAB-004-PATIENT-001`.

A blank or missing value is rejected in `PatientSearchService` before the HTTP call (HTTP 400).

## ADR-004 — Do not follow pagination links

**Status:** Accepted for this laboratory.

**Decision:** Copy `Bundle.link` from the single search response. Do not accept `_count`. Do not request `next` or `previous`. Do not add a paging operation to `GET /api/fhir/patients/search`.

**Observed on this HAPI process, by calling HAPI directly:**

- `GET /Patient?identifier=LAB-002-PATIENT-001` returned HTTP 200, `searchset`, `total=1`, `entry` = `Patient/1000`. `GET /Patient/1000` stored `system=https://example.org/fhir/identifier/lab-002` and `value=LAB-002-PATIENT-001`. The `system|value` form returned the same Patient.
- A search with no match returned HTTP 200, `searchset`, `total=0`, no `entry`, and `link.relation=self`.
- One match produced only `self`. Three Patients sharing `identifier.value=LAB-004-PAGE-001`, searched with `_count=1`, produced `total=3` and `next`. The first `next` URL returned `Patient/1004` plus `previous`. The following page returned `Patient/1005` and no further `next`.
- Those page URLs are HAPI `Bundle.link` values. This application does not call them.

## ADR-005 — Cached search responses on this server

**Status:** Observed here. Not stated as general FHIR behavior.

On `http://localhost:18080/fhir`, repeating a search URL returned the same Bundle `id` and the header `X-Cache: HIT from http://localhost:18080/fhir`.

After a search of `LAB-004-CACHE-PROBE` had returned `total=0`, `POST /Patient` created `Patient/1002` with that exact `identifier.value`. The same search URL still returned the previous Bundle and `total=0`, with `X-Cache: HIT`. Adding `_count=1` returned `total=1` and `Patient/1002`.

`GET /Patient/1000` confirmed the stored identifier. The earlier `total=0` for that search URL was not a different stored value.

## Other decisions

| Topic | Decision |
| --- | --- |
| Base URL | `fhir.base-url` in `application.yml`. |
| Port | 8086, same as Lab #003. One process at a time. |
| FHIR HTTP errors | `FhirClientException.httpError` → 502. |
| Connection failure | `FhirClientException.unavailable` → 502. |
| Non-searchset or unreadable body | `UnexpectedFhirResponseException` → 500. No stack trace in the JSON body. |
| Tests | `MockRestServiceServer` for the client. `@WebMvcTest` for the controller. No live HAPI in unit tests. |
| Shared HAPI | This laboratory started the existing `hin-fhir-lab-001` container. It did not change Lab #001 compose files. H2 had no Patient at startup; two fictitious Patients were created so a search with matches could be observed. |
