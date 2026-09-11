# Java Client — Search Patient Design

Design only. This document does not implement code, change Docker, or claim Java search results. Those results do not exist yet.

Statement kinds used below:

- **Official documentation:** taken from HL7 FHIR R4 or HAPI FHIR docs.
- **Laboratory decision:** a choice for this experiment.
- **Prior HTTP observation:** recorded in this laboratory's HTTP notes, not produced by a Java client.
- **Expected observation:** what the later Java run is intended to check. Not a result.

## 1. Objective

Learn how a Java / Spring Boot consumer executes a FHIR **search** with the HAPI FHIR Generic (Fluent) Client, and how the response is represented as a FHIR `Bundle` rather than a single `Patient`.

The Java read already documented in [results/04-java-client-read-patient.md](../results/04-java-client-read-patient.md) showed:

```text
read().resource(Patient.class).withId("1000").execute()
        ↓
org.hl7.fhir.r4.model.Patient
```

Search is a different interaction. Official FHIR search returns a collection ([HTTP search](https://hl7.org/fhir/R4/http.html#search)):

```text
GET [base]/[type]?[parameters]
        ↓
Bundle (type = searchset)
        ↓
zero or more matching resources
```

The laboratory question is: can the same Generic Client that read `Patient/1000` now search by `identifier` and let Java inspect the `Bundle` and the enclosed `Patient`?

This step does **not** create, read-by-id, update, or delete a Patient from Java. It does not add authentication.

## 2. Scope

**Laboratory decision:** one search only.

| In scope | Out of scope |
| --- | --- |
| Search `Patient` by `identifier` (`system\|value`) | Pagination, `_count`, `_sort` |
| Execution from the existing Java client module | `_include`, `_revinclude` |
| Receive `org.hl7.fhir.r4.model.Bundle` | Multiple search parameters |
| Inspect `Bundle.type`, `Bundle.total`, entry count | Chained search, composite search |
| Inspect the matching `Patient` (`id`, `identifier`, `name`) | HTTP POST search |
| | Update, delete |
| | Authentication |
| | Terminology, profiles |
| | Advanced / remaining FHIR Search features |

Reuse the existing stack. Do not add Maven dependencies. Do not create a second module.

```text
Java 21
   ↓
Spring Boot 3.5.16
   ↓
HAPI FHIR 8.12.0
   ↓
FHIR R4 4.0.1
```

Base URL remains `http://localhost:18080/fhir`. Target identifier remains the fictitious laboratory pair already used in HTTP search ([results/03-search-patient.md](../results/03-search-patient.md)):

- system: `https://example.org/fhir/identifier/patient`
- value: `LAB-001-0001`

Those values belong in configuration at implementation time, not hardcoded in Java source. This design does not change `application.yml`.

## 3. HTTP Operation

**Official FHIR concept:** type-level search is `GET [base]/[type]{?[parameters]{&[parameters]}}` ([HTTP search](https://hl7.org/fhir/R4/http.html#search)).

**Laboratory decision:** the single query is:

```text
GET /fhir/Patient?identifier=https://example.org/fhir/identifier/patient|LAB-001-0001
```

Full URL:

```text
http://localhost:18080/fhir/Patient?identifier=https://example.org/fhir/identifier/patient|LAB-001-0001
```

`identifier` is a token search parameter. The `system|value` form asks for that business identifier, not for the logical resource `id`.

**Official documentation:** if search succeeds, the server SHALL return HTTP **200** and a `Bundle` with `type` = `searchset` containing zero or more matching resources ([HTTP search](https://hl7.org/fhir/R4/http.html#search)). An empty match set is still a successful search, not a 404.

**Prior HTTP observation (not Java):** curl/Postman search with this identifier was already recorded in [results/03-search-patient.md](../results/03-search-patient.md). That note is HTTP evidence from an earlier run. It is not a Java client result. The Java search has not been executed.

On the wire, `|` is typically encoded as `%7C`. Encoding is an HTTP detail for the later manual check and for HAPI's client. This design does not claim a Java-observed encoded URL.

## 4. HAPI FHIR Java Approach

Reuse the existing `FhirContext.forR4()` and Generic Client beans. Add a search call. Do not replace the read experiment.

**Official HAPI pattern** ([Generic Client — Search](https://hapifhir.io/hapi-fhir/docs/client/generic_client.html)):

```text
client.search()
      .forResource(Patient.class)
      .where(...)
      .returnBundle(Bundle.class)
      .execute()
      → org.hl7.fhir.r4.model.Bundle
```

HAPI documents fluent `where` clauses built from generated search-parameter constants (for example `Patient.FAMILY`, `Patient.BIRTHDATE`). For a token `identifier`, the same documentation uses:

```text
Patient.IDENTIFIER.exactly().systemAndIdentifier(system, value)
```

**Technical recommendation:** at implementation, confirm that exact fluent call against HAPI FHIR **8.12.0** source/Javadoc before writing code. Do not invent method names from memory. Official alternatives on the same page include `search().byUrl(...)` if the fluent identifier form needs a fallback. Prefer fluent `forResource` + `where` if the 8.12.0 API matches the docs.

Conceptual implementation shape (not source to paste as-is):

```text
FhirContext.forR4()
  → newRestfulGenericClient(baseUrl)
  → client.search()
        .forResource(Patient.class)
        .where(identifier system + value)
        .returnBundle(Bundle.class)
        .execute()
  → Bundle
```

Do not call `count()`, `sort()`, `include()`, `revInclude()`, or `usingHttpPost()` in this experiment.

Proposed runtime (later implementation):

```text
Application startup
        ↓
Load configuration (base URL, identifier system, identifier value)
        ↓
Reuse R4 FhirContext and IGenericClient
        ↓
Search Patient by identifier
        ↓
Bundle (searchset)
        ↓
Inspect type, total, entry count
        ↓
Take Patient from entry.resource
        ↓
Inspect Patient id, identifier, name
        ↓
Console / log evidence
```

Keep one `FhirContext` for the process lifetime. Clients remain cheap and thread-safe (**official documentation**).

## 5. Expected Java Object Model

Search does not return a bare `Patient`. The primary Java type is the R4 `Bundle`.

```text
org.hl7.fhir.r4.model.Bundle
 └── type = searchset
 └── total
 └── entry[]
      └── resource
           └── org.hl7.fhir.r4.model.Patient
```

Fields intended for later inspection:

| Object | Field | Why |
| --- | --- | --- |
| `Bundle` | `type` | Confirms the searchset contract |
| `Bundle` | `total` | Server-stated match count |
| `Bundle` | `entry` size | Collection actually present on this page |
| `Patient` | `id` | Logical id of a match (`1000` is the expected instance) |
| `Patient` | `identifier` | Business identifier used as the search key |
| `Patient` | `name` | Enough demographics to recognize the laboratory Patient |

No extra `Patient` fields in this step. Do not introduce a custom DTO for `Bundle` or `Patient`.

`total` and `entry.size()` are not the same concept if a server pages results. This experiment does not study paging; if both are present they should be logged as observed, without assuming they are always equal on every FHIR server.

## 6. Expected Request Flow

```text
Spring Boot application (Java 21)
        ↓
HAPI FHIR Generic Client
        ↓
HTTP GET /fhir/Patient?identifier={system}|{value}
        ↓
HAPI FHIR Server 8.12.0
        ↓
FHIR Search
        ↓
Bundle (type = searchset)
        ↓
entry.resource = Patient
        ↓
Java inspection of Bundle + Patient
```

The HTTP GET is the FHIR search interaction performed by the client. After implementation, the Java application should treat `execute()`'s `Bundle` as the primary evidence, not a hand-parsed JSON string. HTTP status/headers should not be claimed as Java-observed unless the application actually prints them.

## 7. Evidence Strategy

Same split as the Java read ([results/04-java-client-read-patient.md](../results/04-java-client-read-patient.md)).

**A. Java evidence (later implementation run)**

- Spring Boot start, HAPI version, R4 context
- Search executed through the Generic Client
- `Bundle` received from `execute()`
- `type`, `total`, entry count from the model
- `Patient` taken from an entry, if any
- Process exit code

**B. Manual HTTP evidence (later, by the operator)**

- The same search URL via curl or Postman
- HTTP status, `Content-Type`, JSON `resourceType` / `type`

Future result notes must label these separately. Do not present a Postman 200 as output of the Java client. Do not present Java getters as HTTP headers.

This design does **not** claim that the manual search for the Java experiment has already been run. [results/03-search-patient.md](../results/03-search-patient.md) is an earlier HTTP search of the same parameter, recorded before the Java client existed.

## 8. Manual Reproduction Plan

Do not execute this request in the design step.

```text
GET http://127.0.0.1:18080/fhir/Patient?identifier=https://example.org/fhir/identifier/patient|LAB-001-0001
```

Header:

```text
Accept: application/fhir+json
```

curl form for a later check:

```text
curl -i -G "http://127.0.0.1:18080/fhir/Patient" --data-urlencode "identifier=https://example.org/fhir/identifier/patient|LAB-001-0001" -H "Accept: application/fhir+json"
```

Postman: **GET** the URL above, same `Accept` header.

`Patient/1000` must still exist on the H2 server. If the container was recreated, search can legally return an empty `searchset`. That is an operational precondition, not a reason to POST from Java.

## 9. Expected Observations

These are **expected**, not observed from a Java search.

| Topic | Expected | Status |
| --- | --- | --- |
| HTTP (manual check) | **200** | Expected |
| Resource type | `Bundle` | Expected |
| `Bundle.type` | `searchset` | Expected |
| Entries | at least one | Expected, if `Patient/1000` is still stored |
| Match | `Patient/1000` with identifier `LAB-001-0001` | Expected, same caveat |
| Java type | `org.hl7.fhir.r4.model.Bundle` | Expected |
| Enclosed resource | `org.hl7.fhir.r4.model.Patient` | Expected |

**Prior HTTP observation (03 only):** a previous curl search with this identifier returned HTTP 200, `Bundle.type` = `searchset`, `total` = 1, and `Patient/1000`. That does not count as the Java experiment result.

If Java later sees zero entries, document that as a new observation (for example H2 reset). Do not invent a match.

## 10. Error Scenarios

Study at implementation. Do not add handlers in this step.

| Scenario | Official / expected behaviour | What to study in Java |
| --- | --- | --- |
| Server down / wrong host | No FHIR response | Connection failure from the HTTP provider; the read client already names `FhirClientConnectionException` — confirm at implementation |
| Identifier matches nothing | Official search still **200** + empty `searchset` | `Bundle` with `total` 0 / empty `entry`; **not** `ResourceNotFoundException` (that exception is a 404 / missing resource on **read**) |
| Unexpected payload | Parse or type mismatch | Entry resource is not a `Patient`, or body is not a `Bundle` |

Do not add retry, paging fallbacks, or OAuth error flows.

## 11. Limitations

This experiment, even after a successful Java run, will not show:

- that every FHIR Search parameter works
- pagination, `_count`, or `_sort`
- performance or result-set size behaviour
- chained, composite, or multi-parameter search
- POST search
- authentication or authorization
- interoperability across different FHIR server products
- that `Bundle.total` always equals `entry.size()` on every server

It only aims to show that this client, against this HAPI 8.12.0 / R4 4.0.1 server, can search one `Patient` identifier and read a `searchset` `Bundle` in Java.

The Patient remains fictitious.

## 12. References

- [HL7 FHIR R4 (v4.0.1)](https://hl7.org/fhir/R4/index.html)
- [HL7 FHIR R4 RESTful API — search](https://hl7.org/fhir/R4/http.html#search)
- [HL7 FHIR R4 Search](https://hl7.org/fhir/R4/search.html)
- [HL7 FHIR R4 Bundle](https://hl7.org/fhir/R4/bundle.html)
- [HL7 FHIR R4 Patient](https://hl7.org/fhir/R4/patient.html)
- [HAPI FHIR — Generic (Fluent) Client](https://hapifhir.io/hapi-fhir/docs/client/generic_client.html)
- [HAPI FHIR — Client Introduction](https://hapifhir.io/hapi-fhir/docs/client/introduction.html)

Related laboratory notes (context, not Java search evidence):

- [results/03-search-patient.md](../results/03-search-patient.md) — prior HTTP search
- [results/04-java-client-read-patient.md](../results/04-java-client-read-patient.md) — Java read
- [docs/java-client-read-design.md](java-client-read-design.md) — Java read design
