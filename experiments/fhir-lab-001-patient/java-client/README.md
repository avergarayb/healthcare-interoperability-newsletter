# FHIR Lab #001 — Java Client

Minimal Spring Boot / Java 21 consumer that reads or searches a FHIR R4 `Patient` through the HAPI FHIR Generic Client.

This module belongs to [FHIR Lab #001 — Patient](../README.md). It does not replace the HAPI server.

## Objective

Demonstrate:

```text
Java 21
   ↓
Spring Boot 3.5.16
   ↓
HAPI FHIR Client 8.12.0
   ↓
FHIR R4 context
   ↓
read  → GET /fhir/Patient/{id}     → org.hl7.fhir.r4.model.Patient
search → GET /fhir/Patient?identifier=... → org.hl7.fhir.r4.model.Bundle
```

## Requirements

- Java 21
- Maven 3.6.3 or later
- The laboratory HAPI FHIR Server running at the configured base URL

The expected laboratory instance is `Patient/1000` on `http://localhost:18080/fhir`. That instance must already exist. This client does not create it.

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
fhir:
  server:
    base-url: http://localhost:18080/fhir
  operation: search
  patient:
    id: "1000"
  identifier:
    system: https://example.org/fhir/identifier/patient
    value: LAB-001-0001
```

- `fhir.server.base-url` — FHIR base, including `/fhir`
- `fhir.operation` — `read` or `search` (only one interaction runs)
- `fhir.patient.id` — logical id for read (string)
- `fhir.identifier.system` / `fhir.identifier.value` — token used for search

No secrets are required. The laboratory server has no OAuth.

## Build

From this directory:

```text
mvn -DskipTests compile
```

## Run

Search (default in `application.yml`):

```text
mvn spring-boot:run
```

Read:

```text
mvn spring-boot:run -Dspring-boot.run.arguments=--fhir.operation=read
```

The application starts, performs the selected FHIR interaction, prints model fields, and exits.

## What it does

- Initializes one `FhirContext.forR4()`
- Creates a Generic / Fluent `IGenericClient`
- **read:** `Patient` by logical id
- **search:** `Patient` by `identifier` (`system|value`), then inspects the `Bundle` (`type`, `total`, entries) and any enclosed `Patient` (`id`, `identifier`, `name`)

## What it does not do

- create, update, delete, PATCH, history
- pagination, `_count`, `_sort`, `_include`, `_revinclude`
- chained or composite search
- POST search
- transactions
- OAuth2 / SMART on FHIR / JWT
- a REST API of its own
- custom Patient or Bundle DTOs
- persistence
- Docker or server changes

## Relation to the laboratory

Design:

- [docs/java-client-read-design.md](../docs/java-client-read-design.md)
- [docs/java-client-search-design.md](../docs/java-client-search-design.md)

The parent experiment README is [../README.md](../README.md).
