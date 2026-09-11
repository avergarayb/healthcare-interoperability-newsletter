# FHIR Lab #001 — Java Client

Minimal Spring Boot / Java 21 consumer that reads one FHIR R4 `Patient` through the HAPI FHIR Generic Client.

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
GET /fhir/Patient/{id}
   ↓
org.hl7.fhir.r4.model.Patient
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
  patient:
    id: "1000"
```

- `fhir.server.base-url` — FHIR base, including `/fhir`
- `fhir.patient.id` — logical id to read (string, not assumed numeric)

No secrets are required. The laboratory server has no OAuth.

## Build

From this directory:

```text
mvn -DskipTests compile
```

## Run

```text
mvn spring-boot:run
```

The application starts, performs one FHIR read, prints fields from the `Patient` model, and exits.

## What it does

- Initializes one `FhirContext.forR4()`
- Creates a Generic / Fluent `IGenericClient`
- Reads the configured Patient id
- Prints `id`, `identifier`, `name`, `telecom`, `gender`, and `birthDate` from the FHIR model

## What it does not do

- create, update, delete, search, PATCH, history
- transactions or bundles
- OAuth2 / SMART on FHIR / JWT
- a REST API of its own
- custom Patient DTOs
- persistence
- Docker or server changes

## Relation to the laboratory

Design: [docs/java-client-read-design.md](../docs/java-client-read-design.md)

The parent experiment README is [../README.md](../README.md).
