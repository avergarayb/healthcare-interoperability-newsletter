# Java Client — Read Patient Design

Design only. This document does not implement code, change Docker, or add experimental claims beyond the Patient already recorded in this laboratory.

Statement kinds used below:

- **Official documentation:** taken from HL7 FHIR R4, HAPI FHIR, or Spring Boot docs.
- **Laboratory decision:** a choice for this experiment.
- **Technical recommendation:** justified preference, not an observed result.

## 1. Objective

Connect the FHIR `Patient` knowledge already gained in this laboratory to a Java / Spring Boot backend by reading an existing resource through a FHIR client.

The first Java operation is:

```text
GET /fhir/Patient/1000
```

against the independent lab server at `http://localhost:18080/fhir`.

This step does **not** create, search, update, or delete a Patient from Java. It does not add authentication.

## 2. Current Laboratory Context

Already implemented, tested, and documented:

| Item | Value |
| --- | --- |
| FHIR | R4 4.0.1 |
| Server | HAPI FHIR Server 8.12.0 (`hapiproject/hapi:v8.12.0-1`) |
| Base URL | `http://localhost:18080/fhir` |
| Existing instance | `Patient/1000` |
| Identifier | `https://example.org/fhir/identifier/patient` / `LAB-001-0001` |

Evidence is only in:

- [results/01-metadata.md](../results/01-metadata.md)
- [results/02-create-read-patient.md](../results/02-create-read-patient.md)
- [results/03-search-patient.md](../results/03-search-patient.md)
- [docs/patient-resource-anatomy.md](patient-resource-anatomy.md)

**Observed (prior run):** curl `GET /fhir/Patient/1000` returned HTTP 200 and the fictitious Patient. This design assumes that instance is still present when the Java client is later implemented. H2 has no durable volume; if the container was recreated, `Patient/1000` may no longer exist. That is an operational precondition, not a new test.

The other repository `healthcare-ai-interoperability-lab` remains out of scope.

## 3. Technical Decisions

| Decision | Choice | Kind |
| --- | --- | --- |
| Language / runtime | Java 21 | Laboratory decision (already in the lab architecture) |
| Application framework | Spring Boot 3.5.x (pin latest 3.5 patch at implementation; 3.5.16 listed as current 3.5 line in official docs at design time) | Laboratory decision |
| FHIR library | HAPI FHIR Generic (Fluent) Client + R4 structures, version **8.12.0** | Laboratory decision |
| First interaction | FHIR **read**: `GET /Patient/{id}` | Laboratory decision |
| Target id | `1000` (existing lab instance) | Observed previously |
| HTTP style | FHIR client, not a hand-written DTO over WebClient | Technical recommendation |
| Configuration | External `application.yml` | Laboratory decision |
| Secrets | None | Laboratory decision |

The compatibility that matters for this laboratory is this concrete stack:

```text
Java 21
   ↓
Spring Boot 3.5.16
   ↓
HAPI FHIR 8.12.0
   ↓
FHIR R4 4.0.1
```

Spring Boot 4.1.1 existed as a newer Boot line at design time. This laboratory does not use it. Staying on 3.5.16 keeps the client on Spring Boot 3.x and avoids adding Spring Framework 7 as an extra variable next to HAPI FHIR 8.12.0.

## 4. Technology Versions

| Component | Proposed version | Why | Source |
| --- | --- | --- | --- |
| Java | 21 | Required runtime for this laboratory, used together with Spring Boot 3.5.16 and HAPI FHIR 8.12.0. | Laboratory decision |
| Maven | 3.6.3 or later | Official Spring Boot build requirement. | [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html) |
| Spring Boot | **3.5.16** (or newer 3.5.x patch if one exists at implementation) | Current official 3.5 stable line listed next to 4.1.1. Compatible with Java 21. Keeps the lab on Spring Boot 3.x. | [Spring Boot reference version list](https://docs.spring.io/spring-boot/reference/) |
| HAPI FHIR client / structures | **8.12.0** | Same release as the running server. FHIR R4 structures are selected via `hapi-fhir-structures-r4` and `FhirContext.forR4()`. | Server image; [Maven Central `hapi-fhir-client` 8.12.0](https://repo1.maven.org/maven2/ca/uhn/hapi/fhir/hapi-fhir-client/8.12.0/); [Downloading and Importing](https://hapifhir.io/hapi-fhir/docs/getting_started/downloading_and_importing.html) |
| FHIR | R4 4.0.1 | Already proven on this server via `/metadata`. | [results/01-metadata.md](../results/01-metadata.md); [HL7 FHIR R4](https://hl7.org/fhir/R4/index.html) |

Do not use a SNAPSHOT HAPI build. Do not use `latest` Docker or unmatched client/server HAPI versions unless a later note records a deliberate upgrade.

## 5. FHIR Client Choice

### Official HAPI client types

HAPI documents two REST clients ([Client Introduction](https://hapifhir.io/hapi-fhir/docs/client/introduction.html)):

1. **Generic (Fluent) client** — start here; flexible fluent API.
2. **Annotation Client** — static bindings; more setup; not needed for one read.

**Laboratory decision:** Generic (Fluent) client.

### Why a FHIR client instead of WebClient / RestClient

| | A. WebClient / RestClient + own DTO | B. HAPI FHIR Client + `Patient` model |
| --- | --- | --- |
| What you learn | HTTP and JSON mapping | FHIR resource, types, and REST interaction |
| Response type | Custom DTO or `JsonNode` | `org.hl7.fhir.r4.model.Patient` |
| `identifier` / `name` | Easy to flatten incorrectly | Official collections and datatypes |
| FHIR JSON rules | Easy to ignore (`resourceType`, repeating arrays) | Handled by HAPI parsers |
| Effort for one GET | Slightly less | Slightly more (context + client) |
| Fit for this lab | Proves HTTP works | Proves FHIR consumption |

**Technical recommendation:** option B.

The laboratory goal is interoperability, not “make a GET return a string”. A DTO that copies `firstName` / `lastName` would hide `HumanName` and `Identifier`. HAPI’s R4 `Patient` is the same model described in [patient-resource-anatomy.md](patient-resource-anatomy.md).

## 6. Architecture

Deliberately small. No extra services.

```text
Spring Boot application (Java 21)
        |
        | HAPI FHIR Generic Client
        v
HTTP  GET /fhir/Patient/1000
        |
        v
HAPI FHIR Server 8.12.0
        |
        v
FHIR R4 Patient (id = 1000)
```

The Java process is a **consumer**. It does not replace the server. It does not share Docker network, volumes, or ports with `healthcare-ai-interoperability-lab`.

## 7. Read Patient Flow

**Official FHIR concept:** read is `GET [base]/[type]/[id]` ([HTTP read](https://hl7.org/fhir/R4/http.html#read)).

**Official HAPI pattern** ([Generic Client — Read/VRead](https://hapifhir.io/hapi-fhir/docs/client/generic_client.html)):

```text
FhirContext.forR4()
  → newRestfulGenericClient(baseUrl)
  → client.read().resource(Patient.class).withId("1000").execute()
  → org.hl7.fhir.r4.model.Patient
```

Proposed runtime flow:

```text
Application startup
        ↓
Load configuration (base URL, patient id)
        ↓
Create FhirContext for R4 (once; expensive, thread-safe)
        ↓
Create IGenericClient (inexpensive; reusable)
        ↓
Read Patient/1000
        ↓
Patient Java model
        ↓
Access id, identifier, name, telecom, gender, birthDate
        ↓
Console / log evidence
```

**Official documentation:** keep one `FhirContext` for the application lifetime. Clients are cheap and thread-safe.

vread (`withIdAndVersion`) is out of scope for the first implementation.

## 8. Maven Dependencies

No `pom.xml` is created in this step.

| Dependency | Version | Purpose | Declared? |
| --- | --- | --- | --- |
| `org.springframework.boot:spring-boot-starter-parent` | 3.5.16 (parent POM) | Dependency management, Java 21 compiler defaults | Yes (parent) |
| `org.springframework.boot:spring-boot-starter` | managed by parent | Application runtime, SLF4J/Logback (HAPI requires SLF4J) | Yes |
| `ca.uhn.hapi.fhir:hapi-fhir-structures-r4` | 8.12.0 | R4 `Patient` and datatypes | Yes — official minimum with a structures JAR |
| `ca.uhn.hapi.fhir:hapi-fhir-client` | 8.12.0 | Generic REST client | Yes |
| `ca.uhn.hapi.fhir:hapi-fhir-base` | 8.12.0 | Core (`FhirContext`, parsers) | **Transitive** via client and structures |
| `org.apache.httpcomponents:httpclient` | managed by HAPI | Default HTTP transport | **Transitive** via `hapi-fhir-client` |

Not declared for this first step:

- `hapi-fhir-validation-resources-r4` — validation is out of scope
- `spring-boot-starter-web` — no HTTP API to expose
- `spring-boot-starter-webflux` / WebClient — not the chosen approach
- Annotation client extra modules
- Testcontainers / Spring Boot test starters — optional later; not required to design the read

## 9. Configuration

Externalize values. Do not hardcode the server URL or Patient id in Java source.

Proposed keys (names are a laboratory decision; values are the current lab):

```yaml
fhir:
  server:
    base-url: http://localhost:18080/fhir
  patient:
    id: "1000"
```

| Property | Meaning |
| --- | --- |
| `fhir.server.base-url` | FHIR base, including `/fhir` |
| `fhir.patient.id` | Logical id for the read |

FHIR version is selected in code by `FhirContext.forR4()`, not by a free-text version string that could silently disagree with the structures JAR. A comment or a non-binding `fhir.version: R4` property may be added for documentation only.

No secrets, tokens, or passwords. The lab HAPI has no OAuth.

## 10. HAPI FHIR Client Components

Verified against official HAPI client documentation. Do not invent APIs.

| Component | Use in this experiment | Source |
| --- | --- | --- |
| `FhirContext` | `FhirContext.forR4()`; factory for client and parsers | [Introduction](https://hapifhir.io/hapi-fhir/docs/getting_started/introduction.html), [Generic Client](https://hapifhir.io/hapi-fhir/docs/client/generic_client.html) |
| `IGenericClient` | `ctx.newRestfulGenericClient(serverBase)` | Same |
| `read().resource(Patient.class).withId(...).execute()` | FHIR read | [Read/VRead](https://hapifhir.io/hapi-fhir/docs/client/generic_client.html) |
| `org.hl7.fhir.r4.model.Patient` | R4 Java model | `hapi-fhir-structures-r4` |
| `IParser` (optional) | `ctx.newJsonParser()` if evidence is printed as FHIR JSON | [Introduction](https://hapifhir.io/hapi-fhir/docs/getting_started/introduction.html) |

Not required for the first read:

| Component | Why not |
| --- | --- |
| Annotation Client | Official docs: extra work; Generic client is the starting point |
| `IClientExecutable` | Fluent internals; official examples call `execute()` and do not require declaring this type |
| Custom `ApacheRestfulClientFactory` / OkHttp | Default HTTP provider is enough |
| Search / create / update / delete fluent APIs | Out of scope |

Accessors to study on the returned `Patient` (model API; exact method names to be confirmed against the 8.12.0 R4 classes at implementation):

- resource id (`getIdElement()` / id part)
- `identifier` collection
- `name` / `HumanName` (`family`, `given`)
- `telecom` / `ContactPoint`
- `gender`
- `birthDate`

Implementation must use the real getters from `org.hl7.fhir.r4.model.Patient`. This design does not invent method signatures.

## 11. Expected Behavior

When implemented, the application should:

1. Start with Java 21 and the configured base URL.
2. Initialize one R4 `FhirContext` and an `IGenericClient`.
3. Read `Patient/1000`.
4. Receive an `org.hl7.fhir.r4.model.Patient`, not a raw JSON string as the primary result.
5. Print or log enough fields to show it is the laboratory instance.

Evidence to compare with [02-create-read-patient.md](../results/02-create-read-patient.md):

| Field | Expected from prior observation |
| --- | --- |
| resource id | `1000` |
| `identifier.system` | `https://example.org/fhir/identifier/patient` |
| `identifier.value` | `LAB-001-0001` |
| `name.family` | `Demo` |
| `name.given` | `Ana` |
| `telecom` | phone `+51999999999`, use `mobile` |
| `gender` | `female` |
| `birthDate` | `1990-05-15` |

If those values differ, document that as a new observation (for example, the H2 store was reset). Do not treat this table as a live test result of the Java client; the Java client has not been run yet.

## 12. Error Scenarios

Study these during implementation. Do not implement handlers in this step.

| Scenario | FHIR / HTTP | What to study |
| --- | --- | --- |
| Patient found | Official read returns 200 and the resource | Happy path |
| Patient missing | Official read returns 404 | HAPI `ResourceNotFoundException` — “Represents an HTTP 404 Resource Not Found response” ([Javadoc](https://hapifhir.io/hapi-fhir/apidocs/hapi-fhir-base/ca/uhn/fhir/rest/server/exceptions/ResourceNotFoundException.html)); extends `BaseServerResponseException` |
| Server down / wrong host | No FHIR response | Connection / I/O failure from the HTTP provider; confirm the exact HAPI exception at implementation |
| Payload not a Patient / wrong version | Parse or type mismatch | Client configured with `forR4()` against a non-R4 body |

Do not add retry, circuit breakers, or OAuth error flows.

## 13. Learning Objectives

- FHIR resource
- `Patient`
- resource `id` versus `identifier`
- FHIR JSON (via the client parser, not a hand-built DTO)
- FHIR client (HAPI Generic Client)
- FHIR R4 (`FhirContext.forR4()`)
- RESTful **read**

## 14. Explicit Non-Goals

Not in this experiment:

- OAuth2, SMART on FHIR, JWT
- API Gateway
- Kafka, RabbitMQ, Redis
- Kubernetes
- RAG, LLM, AI agents
- FHIR subscriptions
- terminology services
- advanced profiles / US Core
- transactions / batches
- create / search / update / delete from Java
- Spring Web controllers
- sharing infrastructure with the other laboratory

## 15. Project Structure Proposal

**Laboratory decision:** a Maven module under the experiment, not at the repository root.

```text
experiments/fhir-lab-001-patient/
├── README.md                 # existing lab README (update later with a link only)
├── docs/
│   ├── patient-resource-anatomy.md
│   └── java-client-read-design.md   # this file
├── results/                  # existing HTTP notes stay here
├── infra/
│   └── docker-compose.yml
└── java-client/              # new module (implementation phase)
    ├── README.md
    ├── pom.xml
    └── src/main/
        ├── java/...          # Spring Boot app + FHIR client bean
        └── resources/
            └── application.yml
```

Why this layout:

- Keeps the newsletter repo as a workspace of experiments, not a single root Maven app.
- Sits next to `docs/`, `results/`, and `infra/` for the same lab.
- Avoids a multi-module parent until a second Java module exists.
- Implementation notes for the Java run go in `results/04-java-client-read-patient.md` so all lab evidence stays in one folder.

Gradle is not chosen: Maven is enough and matches common Spring Boot + HAPI examples.

## 16. Implementation Plan

Later, after this design is approved:

1. Create `java-client/` with Spring Boot 3.5.x parent and Java 21.
2. Add `hapi-fhir-structures-r4` and `hapi-fhir-client` 8.12.0.
3. Add `application.yml` with base URL and patient id.
4. Define a `FhirContext` bean (`forR4()`) and an `IGenericClient` bean.
5. On startup (for example `ApplicationRunner` / `CommandLineRunner`), perform the read.
6. Log id, identifier, name, telecom, gender, birthDate.
7. Optionally log FHIR JSON via `IParser`.
8. Manually exercise the missing-id case and record the exception type.
9. Write `results/04-java-client-read-patient.md`.
10. Add a short link from the experiment README (implementation phase only).

Do not change `docker-compose.yml`. Do not touch the other laboratory.

## 17. Documentation Plan

After implementation, add:

| File | Role |
| --- | --- |
| `java-client/README.md` | How to build and run the client |
| `results/04-java-client-read-patient.md` | Observed / Interpretation / Limitations of the Java read |
| Experiment `README.md` | One new link under Experimental results |

Do not rewrite `01`–`03` result notes. Do not claim the Java client worked until that run is recorded.

## 18. Official References

- [HL7 FHIR R4 (v4.0.1)](https://hl7.org/fhir/R4/index.html)
- [HL7 FHIR R4 RESTful API — read](https://hl7.org/fhir/R4/http.html#read)
- [HL7 FHIR R4 Patient](https://hl7.org/fhir/R4/patient.html)
- [HAPI FHIR — Introduction / FhirContext](https://hapifhir.io/hapi-fhir/docs/getting_started/introduction.html)
- [HAPI FHIR — Downloading and Importing](https://hapifhir.io/hapi-fhir/docs/getting_started/downloading_and_importing.html)
- [HAPI FHIR — Client Introduction](https://hapifhir.io/hapi-fhir/docs/client/introduction.html)
- [HAPI FHIR — Generic (Fluent) Client](https://hapifhir.io/hapi-fhir/docs/client/generic_client.html)
- [HAPI FHIR — ResourceNotFoundException](https://hapifhir.io/hapi-fhir/apidocs/hapi-fhir-base/ca/uhn/fhir/rest/server/exceptions/ResourceNotFoundException.html)
- [Maven Central — hapi-fhir-client 8.12.0](https://repo1.maven.org/maven2/ca/uhn/hapi/fhir/hapi-fhir-client/8.12.0/)
- [Spring Boot project](https://spring.io/projects/spring-boot)
- [Spring Boot reference (version list)](https://docs.spring.io/spring-boot/reference/)
- [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
