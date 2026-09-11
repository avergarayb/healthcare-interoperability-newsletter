# FHIR Lab #001 — Patient

Design and scope document for the first experiment in this repository.

This file describes what the experiment will study and what it will not include. It does not implement the experiment. No application code, server, or infrastructure is created in this step.

## 1. Experiment title

**FHIR Lab #001 — Patient**

## 2. Objective

The objective of this experiment is to understand how a FHIR `Patient` resource is represented and exchanged through a FHIR REST API, and how a Java / Spring Boot application can later consume that resource.

The experiment will treat FHIR as a contract: a `Patient` instance is created, read, and searched as JSON over HTTP. A later step may add a small Spring Boot client that calls the same API. That client is in scope as a planned integration, not as an implementation in this document.

## 3. FHIR version

This experiment uses:

- **FHIR R4**
- **FHIR version 4.0.1**

All resource examples, search behavior, and HTTP interactions in this laboratory should be checked against the FHIR R4 4.0.1 specification.

## 4. Resource

**Resource:** `Patient`

According to the official FHIR R4 specification, `Patient` covers demographics and other administrative information about an individual or animal receiving care or other health-related services. The resource is focused on the "who" information needed for administrative, financial, and logistic procedures.

`Patient` does **not** represent the full clinical history of a person. Conditions, observations, encounters, medications, and similar clinical data are modeled as other FHIR resources that may later reference a `Patient`. This experiment stays on `Patient` only.

A patient receiving care at more than one organization may have more than one `Patient` record. That fact is noted here; it is not implemented in this laboratory.

## 5. Initial scope

This laboratory is limited to the following interactions and formats:

| Item | In scope |
| --- | --- |
| Capability discovery | `GET /metadata` |
| Create a Patient | `POST /Patient` |
| Read a Patient by logical id | `GET /Patient/{id}` |
| Search a Patient by identifier | `GET /Patient?identifier=...` |
| Resource format | FHIR JSON |
| Later application integration | a Java / Spring Boot consumer of the same API |

Out of this initial HTTP set: updates, deletes, history, bundles, XML, and other search parameters.

`GET /metadata` is included so the experiment can inspect what the server declares it supports before any `Patient` interaction.

## 6. Patient fields for the experiment

The experiment will use only a small subset of `Patient`:

| Field | FHIR cardinality (R4) | Role in this experiment |
| --- | --- | --- |
| `identifier` | `0..*` | Business identifier for the patient, distinct from the server-assigned resource `id` |
| `name` | `0..*` | Name associated with the patient |
| `telecom` | `0..*` | Contact detail for the individual |
| `gender` | `0..1` | Administrative gender (`male` \| `female` \| `other` \| `unknown`) |
| `birthDate` | `0..1` | Date of birth |

Other `Patient` elements (`address`, `contact`, `communication`, `managingOrganization`, `link`, and the rest of the resource) are intentionally omitted for now.

The experiment will also observe standard resource-level elements that appear in FHIR JSON, especially `resourceType` and `id`, because they are required to understand how a resource instance is identified on the wire. Those elements are not extra clinical fields; they belong to the FHIR resource envelope.

## 7. Architecture

Conceptual architecture for this laboratory:

```text
Spring Boot / Java 21
        |
        | HTTP / FHIR REST
        v
HAPI FHIR Server
        |
        v
FHIR R4 Patient
```

How to read this diagram:

- **Spring Boot / Java 21** is the planned consuming application. It is not created in this document.
- **HTTP / FHIR REST** is the contract between the application and the server (`/metadata`, `POST /Patient`, `GET /Patient/{id}`, `GET /Patient?identifier=...`).
- **HAPI FHIR Server** is the FHIR server implementation that will expose the REST API in a later implementation step.
- **FHIR R4 Patient** is the resource stored and returned by the server.

This is a conceptual sketch for one experiment. It is not a production architecture and it does not introduce additional services.

## 8. Explicit non-goals

The following are out of scope for FHIR Lab #001 — Patient:

- Kafka
- RabbitMQ
- Redis
- Kubernetes
- API Gateway
- OAuth2 / Keycloak
- RAG
- LLM
- AI agents
- vector database
- additional microservices

This laboratory is a first look at one resource and a small REST surface. Broader integration, security, messaging, and AI topics belong to later experiments, if they are defined.

## 9. Questions to investigate

The implementation and notes for this laboratory should try to answer:

- What is a FHIR Resource?
- What is the difference between `id` and `identifier`?
- Why are `name` and `telecom` collections?
- What does `resourceType` represent?
- How does FHIR Search work?
- How is a `Patient` represented in FHIR JSON?
- How does a Java application consume a FHIR resource?
- What does HAPI FHIR provide?
- What is the role of the FHIR server versus the consuming application?

Answers should be written from official sources and from observations produced by this experiment. Unsupported claims should be avoided.

## 10. Expected outcomes

Success for this laboratory is a set of verifiable results, not a claim of expertise.

After the experiment is implemented and documented, this repository should contain:

1. A documented FHIR JSON example of `Patient` that uses only the fields listed in section 6.
2. Recorded request and response notes for:
   - `GET /metadata`
   - `POST /Patient`
   - `GET /Patient/{id}`
   - `GET /Patient?identifier=...`
3. Written answers to the questions in section 9, distinguished as facts from the specification, observations from the experiment, or open questions.
4. A short note on how a Java / Spring Boot application would call the same endpoints, without requiring that client to exist before the FHIR interactions are understood.
5. Confirmation that no item from the non-goals list was introduced.

If an outcome cannot be demonstrated from the specification or from the experiment, it should be left as an open question.

## 11. Sources

Primary sources for this laboratory:

- [HL7 FHIR R4 (v4.0.1) specification](https://hl7.org/fhir/R4/index.html)
- [HL7 FHIR R4 Patient](https://hl7.org/fhir/R4/patient.html)
- [HL7 FHIR R4 Resource](https://hl7.org/fhir/R4/resource.html)
- [HL7 FHIR R4 RESTful API](https://hl7.org/fhir/R4/http.html)
- [HL7 FHIR R4 Search](https://hl7.org/fhir/R4/search.html)
- [HL7 FHIR R4 JSON](https://hl7.org/fhir/R4/json.html)
- [HAPI FHIR project](https://hapifhir.io/)
- [HAPI FHIR documentation](https://hapifhir.io/hapi-fhir/docs/)
- [HAPI FHIR JPA Server introduction](https://hapifhir.io/hapi-fhir/docs/server_jpa/introduction.html)

Secondary summaries should not replace these sources.

## Experimental results

Initial operations were executed against HAPI FHIR 8.12.0 / FHIR R4 4.0.1 at `http://localhost:18080/fhir`.

Notes from that run:

- [results/01-metadata.md](results/01-metadata.md)
- [results/02-create-read-patient.md](results/02-create-read-patient.md)
- [results/03-search-patient.md](results/03-search-patient.md)
- [results/04-java-client-read-patient.md](results/04-java-client-read-patient.md)
- [results/05-java-client-search-patient.md](results/05-java-client-search-patient.md)
