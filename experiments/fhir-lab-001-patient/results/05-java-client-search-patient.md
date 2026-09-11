# 05 — Java Client Search Patient

Observations from one Java execution against the independent FHIR Lab #001 server.

- Server: HAPI FHIR 8.12.0
- FHIR: R4 4.0.1
- Base URL: `http://localhost:18080/fhir`

The Patient data used in this laboratory is fictitious.

A separate manual search of the same identifier was later executed by the user with Postman/curl. That HTTP check is recorded under **Manual HTTP evidence**. It is not output of the Java client. The two observations are independent measurements of the same FHIR search.

## Objective

Execute a FHIR **search** from Java using the HAPI FHIR Generic Client, receive an R4 `Bundle`, and inspect a matching `Patient` inside that bundle.

The Java operation under test was:

```text
GET /fhir/Patient?identifier={system}|{value}
```

with:

- system: `https://example.org/fhir/identifier/patient`
- value: `LAB-001-0001`

This is not a read-by-id. The primary Java type is `org.hl7.fhir.r4.model.Bundle`.

## Environment

| Item | Value |
| --- | --- |
| Java | 21.0.2 |
| Spring Boot | 3.5.16 |
| HAPI FHIR | 8.12.0 |
| FHIR | R4 4.0.1 |
| HAPI FHIR server base URL | `http://localhost:18080/fhir` |
| Search parameter | `identifier` (`system\|value`) |

## Implementation

The existing module `experiments/fhir-lab-001-patient/java-client` was extended. No new Maven dependencies were added.

- The same `FhirContext.forR4()` and `IGenericClient` beans are reused.
- Identifier system and value come from `application.yml` (`fhir.identifier.system`, `fhir.identifier.value`).
- `fhir.operation: search` selects the search runner so this run does not also execute the previous read.
- The 8.12.0 API compiled as documented:

```text
client.search()
      .forResource(Patient.class)
      .where(Patient.IDENTIFIER.exactly().systemAndIdentifier(system, value))
      .returnBundle(Bundle.class)
      .execute()
```

`Patient.IDENTIFIER` is a `TokenClientParam` on `org.hl7.fhir.r4.model.Patient`. `exactly().systemAndIdentifier(String, String)` exists on `TokenClientParam.IMatches` in `hapi-fhir-base` 8.12.0. No fallback (`byUrl`) was required.

## Request Flow

```text
Spring Boot 3.5.16
        ↓
FhirContext.forR4()
        ↓
IGenericClient (base URL http://localhost:18080/fhir)
        ↓
search().forResource(Patient.class).where(identifier).returnBundle(Bundle.class).execute()
        ↓
GET /fhir/Patient?identifier={system}|{value}   ← FHIR search interaction (not printed by the app)
        ↓
HAPI FHIR Server 8.12.0
        ↓
org.hl7.fhir.r4.model.Bundle
        ↓
entry.resource = org.hl7.fhir.r4.model.Patient
```

The HTTP path is the FHIR search interaction performed by the Generic Client. The Java application did not print status codes or headers. Fields below were observed on the `Bundle` and `Patient` models after `execute()` returned.

## Observed

### Java evidence

From the Java run (`mvn -q spring-boot:run` with `fhir.operation=search`):

- Spring Boot 3.5.16 started with Java 21.0.2.
- HAPI logged `HAPI FHIR version 8.12.0`.
- HAPI logged `Creating new FHIR context for FHIR version [R4]`.
- The runner logged `Searching FHIR Patient by identifier https://example.org/fhir/identifier/patient|LAB-001-0001 at http://localhost:18080/fhir`.
- `execute()` returned `org.hl7.fhir.r4.model.Bundle`.
- The process printed:

```text
FHIR Patient search successfully
Bundle type: searchset
Total matches: 1
Entries: 1
Patient ID: 1000
Identifier: https://example.org/fhir/identifier/patient|LAB-001-0001
Name: Ana Demo
```

- The process ended with **exit code 0**.

The Java client did not print an HTTP status. It did not dump raw FHIR JSON as its primary result. An empty search was not observed on this run.

### Manual HTTP evidence

Independent check by the user (Postman/curl). Not printed by the Java client.

Request:

```text
curl --location "http://127.0.0.1:18080/fhir/Patient?identifier=https://example.org/fhir/identifier/patient%7CLAB-001-0001" \
--header "Accept: application/fhir+json"
```

Observed on that manual run (fields only; response body not copied in full):

- HTTP status: **200**
- `Bundle.type`: **searchset**
- `Bundle.total`: **1**
- `Bundle.entry` count: **1**
- `entry.fullUrl`: `http://127.0.0.1:18080/fhir/Patient/1000`
- `entry.search.mode`: **match**
- `Patient.id`: **1000**
- `Patient.identifier`: `https://example.org/fhir/identifier/patient|LAB-001-0001`
- `Patient.name`: Ana Demo

The Java client did not observe this HTTP **200**. The status comes only from this manual check. The Java run observed the same match through the `Bundle` / `Patient` models.

[results/03-search-patient.md](03-search-patient.md) is an earlier HTTP search of the same identifier, recorded before this Java client search existed. It is not a substitute for the Java evidence above, and it is not this manual check.

## Bundle and Patient Fields Read

Values printed by the Java application from R4 model getters. Not from a custom DTO.

| Field | Value | Source |
| --- | --- | --- |
| Bundle type | searchset | Bundle Java model |
| Total matches | 1 | Bundle Java model (`getTotal()`) |
| Entries | 1 | Bundle Java model (`getEntry().size()`) |
| Patient ID | 1000 | Patient Java model |
| Identifier | `https://example.org/fhir/identifier/patient\|LAB-001-0001` | Patient Java model |
| Name | Ana Demo | Patient Java model |

## Interpretation

This run shows, for this laboratory stack only:

- A HAPI FHIR Generic Client configured with `FhirContext.forR4()` can execute FHIR **search**.
- The fluent `search()` API returns `org.hl7.fhir.r4.model.Bundle`, not a bare `Patient`.
- That `Bundle` can be inspected as a `searchset` (`type`, `total`, entry count).
- A matching `Patient` can be read from `entry.resource` using the official R4 model.
- A custom DTO was not required for `Bundle` or `Patient`.

It does not show that every search parameter, every server, or paging behaves the same way.

## Error Handling

Observed on this successful run:

- No connection failure.
- No empty `searchset`.
- Process exit code **0**.

The application code also contains a basic handler for `FhirClientConnectionException` (server unavailable). An empty result would print `No Patient resources found` and is not mapped to `ResourceNotFoundException`. Those paths were not exercised in this happy-path run.

## Limitations

- Only **search** by one `identifier` (`system\|value`) was executed from Java.
- Pagination, `_count`, `_sort`, `_include`, `_revinclude`, chained search, and POST search were not executed.
- Authentication was not tested.
- The application did not inspect HTTP status or headers.
- The experiment does not evaluate interoperability across different FHIR implementations.
- `Bundle.total` and `entry.size()` happened to be `1` on this run; that equality is not claimed as a general FHIR rule.
- The Patient instance is fictitious.

## Reproducibility

Java client (from `experiments/fhir-lab-001-patient/java-client`, with `fhir.operation: search`):

```text
mvn -q spring-boot:run
```

To run the earlier read instead:

```text
mvn -q spring-boot:run -Dspring-boot.run.arguments=--fhir.operation=read
```

Manual HTTP check already executed by the user (separate from the Java client):

```text
curl --location "http://127.0.0.1:18080/fhir/Patient?identifier=https://example.org/fhir/identifier/patient%7CLAB-001-0001" \
--header "Accept: application/fhir+json"
```

Postman alternative (same search):

- Method: `GET`
- URL: `http://127.0.0.1:18080/fhir/Patient?identifier=https://example.org/fhir/identifier/patient|LAB-001-0001`
- Header: `Accept: application/fhir+json`

## References

- [HL7 FHIR R4 (v4.0.1)](https://hl7.org/fhir/R4/index.html)
- [HL7 FHIR R4 RESTful API — search](https://hl7.org/fhir/R4/http.html#search)
- [HL7 FHIR R4 Search](https://hl7.org/fhir/R4/search.html)
- [HL7 FHIR R4 Bundle](https://hl7.org/fhir/R4/bundle.html)
- [HL7 FHIR R4 Patient](https://hl7.org/fhir/R4/patient.html)
- [HAPI FHIR — Generic (Fluent) Client](https://hapifhir.io/hapi-fhir/docs/client/generic_client.html)
- [HAPI FHIR — Client Introduction](https://hapifhir.io/hapi-fhir/docs/client/introduction.html)
