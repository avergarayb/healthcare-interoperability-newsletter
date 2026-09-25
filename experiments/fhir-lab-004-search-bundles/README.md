# FHIR Lab #004 — Search and Bundles

Spring Boot application that searches FHIR R4 `Patient` resources by `identifier` and maps a `searchset` `Bundle` to a smaller application response.

Lab #001, Lab #002, and Lab #003 are not modified by this laboratory.

## 1. Objective

Show what changes when a backend searches FHIR instead of reading one resource by logical id.

Lab #003:

```text
GET {fhir.base-url}/Patient/{id}
        -> one Patient
```

Lab #004:

```text
GET {fhir.base-url}/Patient?identifier={identifier}
        -> Bundle type=searchset
        -> entry[].resource (Patient)
```

Application endpoint:

```text
GET http://localhost:8086/api/fhir/patients/search?identifier={identifier}
```

## 2. Scope

| In scope | Out of scope |
| --- | --- |
| `identifier` search via Spring `RestClient` | HAPI Generic Client |
| Minimal `Bundle` DTO (`type`, `total`, `entry`, `link`) | Full FHIR Bundle model |
| Application response distinct from the Bundle | Following `next` / pagination engine |
| Empty `searchset` as HTTP 200 | Treating “no matches” as HTTP 404 |
| HTTP 400 / 502 / 500 | OAuth2, SMART, database, messaging, Docker changes |

## 3. Technologies

| Item | Value |
| --- | --- |
| Java | 21 |
| Maven | no wrapper |
| Spring Boot | **3.5.16** |
| HTTP client | `RestClient` from `spring-boot-starter-web` |
| Tests | `spring-boot-starter-test`, `MockRestServiceServer` |
| FHIR | R4 on the existing HAPI at `http://localhost:18080/fhir` |

No extra Maven dependencies.

## 4. Architecture

```text
GET /api/fhir/patients/search?identifier=...
        |
        v
PatientSearchController
        |
        v
PatientSearchService
        |
        v
FhirPatientSearchClient
        |
        v
RestClient
        |
        v
GET {fhir.base-url}/Patient?identifier=...
Accept: application/fhir+json
        |
        v
FhirBundle
        |
        v
PatientSearchResponse
```

| Layer | Class | Role |
| --- | --- | --- |
| Config | `RestClientConfig`, `FhirProperties` | `baseUrl` from `fhir.base-url`, `Accept: application/fhir+json` |
| Client | `FhirPatientSearchClient` | Query parameter via `UriBuilder`, HTTP errors |
| Service | `PatientSearchService` | Blank check, require `searchset`, map entries and links |
| Controller | `PatientSearchController` | `GET /api/fhir/patients/search` |
| Errors | `ApiExceptionHandler` | JSON `{ error, message }`, no stack traces |

`fhir.base-url` is not hardcoded in the client. The client appends `/Patient` and `identifier` with `UriBuilder` (`path` + `queryParam`). A unit test asserts the resolved URL `http://localhost:18080/fhir/Patient?identifier=LAB-002-PATIENT-001`, so the `/fhir` base path is kept.

This application listens on **8086**, the same port as Lab #003. Only one of those processes can bind that port at a time.

## 5. FHIR Search and the application response

The caller sends `identifier` as an application query parameter. The client forwards that string as the FHIR search parameter `identifier`. It does not add a system, and it does not assume a fixed value.

`PatientSearchResponse` is not a Bundle:

- `total` is `Bundle.total` (not `entry.size()`).
- `patients` are `entry[].resource` values whose `resourceType` is `Patient` (`id`, `name`, `gender`, `birthDate`). Other FHIR fields are ignored.
- `links` copies `Bundle.link` (`relation`, `url`) so the navigation links are visible without returning the whole Bundle.

Several `name` entries are kept when FHIR sends them. None are invented.

## 6. Read by id versus FHIR Search

| | Read by logical id | FHIR Search |
| --- | --- | --- |
| Request | `GET /Patient/{id}` | `GET /Patient?identifier=...` |
| Success body | one `Patient` | a `Bundle` |
| Unknown target | HTTP **404** and `OperationOutcome` | HTTP **200** and a `searchset` with `total` = 0 |
| This application's status | Lab #003 returns **404** | this API returns **200** with `patients: []` |

A search with no matches is a successful search. It is not documented as HTTP 404.

### 6.1 `Bundle` `searchset`

On this HAPI server, `GET /Patient?identifier=...` returned:

- `resourceType` = `Bundle`
- `type` = `searchset`

`total` is the match count reported by the server. `entry` is the list of matches included in that response. They are not the same field. With `_count=1` and three matches, `total` stayed **3** while `entry` contained one Patient.

### 6.2 Search with a match

`GET /Patient/1000` returned the stored identifier:

- `system` = `https://example.org/fhir/identifier/lab-002`
- `value` = `LAB-002-PATIENT-001`

`GET /Patient?identifier=LAB-002-PATIENT-001` then returned HTTP **200**, `type=searchset`, `total=1`, `entry` = `Patient/1000`.

`GET /Patient?identifier=https://example.org/fhir/identifier/lab-002|LAB-002-PATIENT-001` also returned `Patient/1000`.

### 6.3 Search with no matches

`GET /Patient?identifier=DOES-NOT-EXIST` returned HTTP **200**:

- `type` = `searchset`
- `total` = 0
- no `entry`
- `link` with `relation` = `self` only

The application maps that to HTTP 200, `patients: []`, and the copied `self` link.

### 6.4 `_count` and `Bundle.link` on HAPI

These calls were made **directly to HAPI**. This Spring API does not accept `_count` and does not call `_getpages`.

`_count` limits how many matches are placed in `entry`. It does not change `total`.

One match, including `identifier=LAB-002-PATIENT-001&_count=1`, returned only `relation=self`.

Three Patients were created with the same `identifier.value` `LAB-004-PAGE-001` before that value was searched:

| `id` | `name.given` |
| --- | --- |
| `1003` | Ada |
| `1004` | Bea |
| `1005` | Ciro |

`GET /Patient?identifier=LAB-004-PAGE-001&_count=1`:

| Page | `total` | `entry` | `link.relation` |
| --- | --- | --- | --- |
| First search URL | 3 | `Patient/1003` | `self`, `next` |
| `GET` of `next` (`_getpagesoffset=1`) | 3 | `Patient/1004` | `self`, `next`, `previous` |
| `GET` of the following `next` (`_getpagesoffset=2`) | 3 | `Patient/1005` | `self`, `previous` |

`self` is the URL of the current page. `next` and `previous` are URLs HAPI put in `Bundle.link`. The page Bundle `id` stayed `cda765e4-e462-4240-9932-7891880978e5`. The third page had no `next`.

That navigation is FHIR Search behavior observed on this HAPI process. It is not a feature of `GET /api/fhir/patients/search`.

### 6.5 What this API does with links

`GET /api/fhir/patients/search?identifier=LAB-004-PATIENT-001` returned HTTP **200**. The body is not a Bundle. Observed:

```json
{
  "total": 1,
  "patients": [
    {
      "resourceType": "Patient",
      "id": "1001",
      "name": [{ "family": "Example", "given": ["Lucia"] }],
      "gender": "female",
      "birthDate": "1988-03-20"
    }
  ],
  "links": [
    {
      "relation": "self",
      "url": "http://localhost:18080/fhir/Patient?identifier=LAB-004-PATIENT-001"
    }
  ]
}
```

`patients` holds the Patient fields taken from `entry` on that one response. `links` copies `Bundle.link` from that same response. With one match, the copied link was only `self`.

This API does not:

- accept `_count`
- request `next` or `previous`
- walk pages
- turn `next` into its own paging operation

### 6.6 Cache observed on this HAPI process

This is an observation of this laboratory server (`X-Cache` on `http://localhost:18080/fhir`). It is not stated as FHIR behavior in general.

`LAB-004-CACHE-PROBE` was searched before any Patient had that value:

| Step | Observed |
| --- | --- |
| First search | HTTP 200, `total=0`, Bundle `id` `c819075b-0589-469c-b106-c7c5d6e92e68`. No `X-Cache`. |
| Same URL again | Same Bundle `id`, `total=0`, header `X-Cache: HIT from http://localhost:18080/fhir`. |
| `POST /Patient` with `identifier.value` `LAB-004-CACHE-PROBE` | HTTP **201**, `id` **1002**. The stored value matched the searched string. |
| Same search URL again | Same Bundle `id`, `total` still **0**, `X-Cache: HIT`. |
| Same identifier plus `_count=1` | HTTP 200, `total=1`, `entry` = `Patient/1002`. |

`GET /Patient/1000` showed `identifier.value` `LAB-002-PATIENT-001` while an earlier identical search URL was still returning a previous `total=0` Bundle. A different query string saw the Patient. A later repeat of `GET /Patient?identifier=LAB-002-PATIENT-001` returned `total=1` and `Patient/1000`; the second of those calls was `X-Cache: HIT` with the same Bundle `id`.

## 7. Error handling

| Condition | Application HTTP | `error` |
| --- | --- | --- |
| Missing or blank `identifier` | **400** | `invalid_identifier` |
| FHIR HTTP 4xx/5xx | **502** | `upstream_error` |
| Connection failure | **502** | `upstream_unavailable` |
| Body is not a `searchset` Bundle, or cannot be read | **500** | `internal_error` |

`OperationOutcome` is not parsed. If HAPI returns one with an error status, the client only keeps the HTTP status.

## 8. Automated tests

Command, from this directory:

```text
mvn clean test
```

**Observed:** `BUILD SUCCESS`. Tests run: **17**, Failures: 0, Errors: 0, Skipped: 0. Finished at `2026-09-25T11:19:47-05:00`.

| Class | Tests |
| --- | --- |
| `FhirPatientSearchClientTest` | 5: match, `total` = 0, query param + Accept, HTTP 500, connection `IOException`, non-JSON body |
| `PatientSearchServiceTest` | 4: blank/null, mapped entries and links, empty searchset, non-`searchset` rejected |
| `PatientSearchControllerTest` | 7: 200 with patients, 200 empty, 400 blank, 400 missing, 502 HTTP error, 502 unavailable, 500 without internal detail |
| `FhirSearchApplicationTests` | 1: context loads |

Unit tests do not call HAPI.

## 9. Manual tests

Docker was not running. Docker Desktop was started and the existing container `hin-fhir-lab-001` was started. No compose file was edited.

### 9.1 Store at startup

```text
GET http://localhost:18080/fhir/Patient/1000
Accept: application/fhir+json
```

**Observed:** HTTP **404**

```json
{
  "resourceType": "OperationOutcome",
  "issue": [ {
    "severity": "error",
    "code": "processing",
    "diagnostics": "HAPI-2001: Resource Patient/1000 is not known"
  } ]
}
```

```text
GET http://localhost:18080/fhir/Patient?identifier=LAB-002-PATIENT-001
```

**Observed:** HTTP **200**, `Bundle.type` = `searchset`, `total` = **0**, `link[0].relation` = `self`. No `entry`. No `next`.

The same shape was observed for `identifier=DOES-NOT-EXIST` and for `system|LAB-002-PATIENT-001`.

### 9.2 Patient created for a positive search

H2 had no Patient. Two fictitious Patients were POSTed (UTF-8 without BOM):

| `identifier.value` | Server `id` | POST |
| --- | --- | --- |
| `LAB-002-PATIENT-001` | **1000** | **201** |
| `LAB-004-PATIENT-001` | **1001** | **201** |

`LAB-002-PATIENT-001` had already been searched while the store was empty. Repeating that exact URL after the POST still returned the same Bundle `id` (`b47b3bed-7ca2-4e93-87fb-edb0de806b59`), `total` = **0**, `lastUpdated` = `2026-09-25T16:20:50.781+00:00`.

A different query on the same identifier, `Patient?identifier=LAB-002-PATIENT-001&_count=1`, returned `total` = **1** and `Patient/1000`. This laboratory does not send `_count`.

`LAB-004-PATIENT-001` was searched only after create. That is the identifier used for the positive application test.

### 9.3 Direct Bundle for `LAB-004-PATIENT-001`

```text
GET http://localhost:18080/fhir/Patient?identifier=LAB-004-PATIENT-001
```

**Observed:** HTTP **200**

- `resourceType`: `Bundle`
- `type`: `searchset`
- `total`: **1**
- `link`: one element, `relation` = `self`. No `next`, `previous`, `first`, or `last`.
- `entry[0].fullUrl`: `http://localhost:18080/fhir/Patient/1001`
- `entry[0].resource`: `Patient` `1001`, name Example / Lucia, `gender` female, `birthDate` 1988-03-20
- `entry[0].search.mode`: `match` (not copied into the application DTO)

`_count=1` on the one-match `LAB-002` query also returned only `self`.

### 9.4 Application

`mvn spring-boot:run` — Tomcat on **8086**.

```text
GET http://localhost:8086/api/fhir/patients/search?identifier=LAB-004-PATIENT-001
```

**Observed:** HTTP **200**

```json
{"total":1,"patients":[{"resourceType":"Patient","id":"1001","name":[{"family":"Example","given":["Lucia"]}],"gender":"female","birthDate":"1988-03-20"}],"links":[{"relation":"self","url":"http://localhost:18080/fhir/Patient?identifier=LAB-004-PATIENT-001"}]}
```

```text
GET http://localhost:8086/api/fhir/patients/search?identifier=DOES-NOT-EXIST
```

**Observed:** HTTP **200**

```json
{"total":0,"patients":[],"links":[{"relation":"self","url":"http://localhost:18080/fhir/Patient?identifier=DOES-NOT-EXIST"}]}
```

```text
GET http://localhost:8086/api/fhir/patients/search?identifier=%20
```

**Observed:** HTTP **400**

```json
{"error":"invalid_identifier","message":"identifier must not be blank"}
```

```text
GET http://localhost:8086/api/fhir/patients/search?identifier=LAB-002-PATIENT-001
```

**Observed at this moment:** HTTP **200**, `total` = 0. The application forwarded the identifier. HAPI returned the Bundle already recorded for that URL in section 9.2. A later direct read showed `Patient/1000` does store `LAB-002-PATIENT-001`, and a later direct search of the same URL returned `total=1` (section 6.2 and 9.6).

### 9.5 Invalid FHIR search (direct) and unreachable server

```text
GET http://localhost:18080/fhir/Patient?birthdate=not-a-date
```

**Observed:** HTTP **400**, `OperationOutcome`, diagnostics `HAPI-1941: Invalid prefix: "not"`. The application does not call `birthdate` and does not parse `OperationOutcome`.

Application restarted with `--fhir.base-url=http://127.0.0.1:19999/fhir`:

```text
GET http://localhost:8086/api/fhir/patients/search?identifier=LAB-004-PATIENT-001
```

**Observed:** HTTP **502**

```json
{"error":"upstream_unavailable","message":"FHIR server is not reachable"}
```

### 9.6 Follow-up: identifier value and `X-Cache`

Later reads of the stored resources:

| `GET` | HTTP | `identifier.system` | `identifier.value` |
| --- | --- | --- | --- |
| `/Patient/1000` | **200** | `https://example.org/fhir/identifier/lab-002` | `LAB-002-PATIENT-001` |
| `/Patient/1001` | **200** | `https://example.org/fhir/identifier/lab-004` | `LAB-004-PATIENT-001` |

Repeating `GET /Patient?identifier=LAB-002-PATIENT-001` after that read returned HTTP **200**, `type=searchset`, `total=1`, `entry` = `Patient/1000`. The second identical call returned the same Bundle `id` and the header `X-Cache: HIT from http://localhost:18080/fhir`. `system|value` also returned `Patient/1000`.

A controlled repeat used an identifier that did not exist yet, `LAB-004-CACHE-PROBE`:

| Step | Result |
| --- | --- |
| First search | HTTP 200, `total=0`, Bundle `id` `c819075b-0589-469c-b106-c7c5d6e92e68`. No `X-Cache`. |
| Second identical search | HTTP 200, same Bundle `id`, `total=0`. Header `X-Cache: HIT`. |
| `POST /Patient` with that exact `identifier.value` | HTTP **201**, `id` **1002**. |
| Same search URL again | HTTP 200, same Bundle `id`, `total` still **0**, `X-Cache: HIT`. |
| Same identifier plus `_count=1` | HTTP 200, `total=1`, `entry` = `Patient/1002`. No `X-Cache` on that first call. |

The stored identifier matched the searched value. The URL that had already returned `total=0` kept returning that Bundle. A different query string saw the new Patient.

### 9.7 Follow-up: `relation=next`

Three Patients were created with the same `identifier.value` `LAB-004-PAGE-001` before that value was searched: `1003` Ada, `1004` Bea, `1005` Ciro.

```text
GET /Patient?identifier=LAB-004-PAGE-001&_count=1
```

**Observed:** HTTP **200**, `type=searchset`, `total=3`, one `entry` (`Patient/1003`). `link`:

- `self` → the search URL above
- `next` → `http://localhost:18080/fhir?_getpages=cda765e4-e462-4240-9932-7891880978e5&_getpagesoffset=1&_count=1&_pretty=true&_bundletype=searchset`

`GET` of that `next` URL: HTTP **200**, same Bundle `id`, `total=3`, `entry` = `Patient/1004`. `link` relations: `self`, `next`, `previous`.

`GET` of the second `next` (`_getpagesoffset=2`): HTTP **200**, `total=3`, `entry` = `Patient/1005`. `link` relations: `self`, `previous`. No further `next`.

This application does not call `_count` or `_getpages`.

## 10. Limitations

This application:

- searches only by `identifier`
- does not accept `_count` or any other FHIR search parameter
- copies `Bundle.link` from the one response it received; it does not follow `next` or `previous`
- does not implement page navigation
- does not parse `OperationOutcome`
- does not authenticate to the FHIR server
- does not persist Bundles or Patients

`_count`, `next`, and `previous` were observed by calling HAPI directly (section 6.4). They are not operations of `GET /api/fhir/patients/search`.

The `X-Cache: HIT` behavior in section 6.6 was observed on this HAPI process. It is not documented here as a rule of FHIR.

Logical ids are not stable across an H2 reset.

## 11. Status

**Lab #004 is closed** for identifier search and `searchset` Bundle mapping.

## 12. Next technical step

Teach this application to accept a page size and to request the `next` URL that HAPI already returned in `Bundle.link`. That follow-up is not implemented. Section 6.4 records the HAPI pages; section 6.5 records that this API stops after one search.

Decision record: [docs/decisions.md](docs/decisions.md).
