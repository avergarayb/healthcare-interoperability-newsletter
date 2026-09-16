# FHIR Lab #003 — Spring FHIR Client

Spring Boot scaffold that prepares later consumption of the laboratory HAPI FHIR server. This first phase creates the Maven project and starts an empty web application. It does **not** call FHIR.

Lab #001 and Lab #002 remain closed. This laboratory does not modify those experiments, their results, Java client, or infrastructure.

## 1. Objective

Create a Java 21 / Spring Boot 3.5.16 Maven project that can later consume FHIR R4 resources from the reused HAPI at `http://localhost:18080/fhir`.

The future target instances (created in Lab #002; logical ids are not portable across an H2 reset) are:

| Resource | Logical id (Lab #002 run) |
| --- | --- |
| Patient | `1000` |
| Practitioner | `1001` |
| Organization | `1002` |
| Encounter | `1003` |
| Observation | `1004` |

This phase only prepares the application shell. It does not prove that those instances still exist, and it does not read them.

## 2. Scope of this first phase

| In scope | Out of scope |
| --- | --- |
| Manual Maven / Spring Boot project (no Spring Initializr ZIP) | RestClient, WebClient, HAPI Generic Client |
| `spring-boot-starter-web` and `spring-boot-starter-test` | Controllers, FHIR DTOs, HTTP calls |
| `application.yml` with port `8086` and `fhir.base-url` | Search, Bundle handling |
| Context-load test and a startable application | OAuth2 / SMART on FHIR |
| Laboratory README | Database, RabbitMQ, Docker, AI |

## 3. Technologies and versions

| Item | Version / value |
| --- | --- |
| Language | Java 21 |
| Build | Maven (no wrapper in this phase) |
| Spring Boot | **3.5.16** (not 4.x) |
| Web | `spring-boot-starter-web` |
| Test | `spring-boot-starter-test` (scope `test`) |
| JSON | Jackson, via Spring Web (no extra Jackson dependency) |
| FHIR release (context only) | R4, as used in Lab #001 / #002 |

## 4. Dependencies added

Declared in `pom.xml`:

- `org.springframework.boot:spring-boot-starter-web`
- `org.springframework.boot:spring-boot-starter-test` (`scope` = `test`)

Parent: `spring-boot-starter-parent` **3.5.16**.

Not added: HAPI FHIR client libraries, Spring Security, Spring Data, messaging starters.

## 5. Server port

```yaml
server:
  port: 8086
```

The application listens on **8086** so it does not collide with HAPI (`18080`) or a default Spring Boot `8080`.

## 6. HAPI FHIR base URL

```yaml
fhir:
  base-url: http://localhost:18080/fhir
```

This property is recorded for the next phase. This phase does not bind it to a client and does not send HTTP to HAPI.

The HAPI process is the Lab #001 container (`hin-fhir-lab-001`). This laboratory does not start, change, or replace that server.

## 7. Excluded functionality

Not implemented in this phase:

- RestClient / WebClient / a class named `FhirClient`
- `PatientController` or any REST API of this application
- FHIR DTOs
- HTTP calls or tests against HAPI
- FHIR search or `Bundle` processing
- OAuth2 / SMART on FHIR / JWT
- Database, RabbitMQ, Docker, Compose
- AI / LLM
- PUT, PATCH, DELETE, transaction Bundle

## 8. Next step

Configure `RestClient` (or an equivalent Spring HTTP client) and **GET** `Patient/1000` from `fhir.base-url`. That work is a later task. It is not done here.

## Requirements

- Java 21
- Maven 3.6.3 or later

HAPI does not need to be running for this phase’s `mvn clean test` or for starting the empty application.

## Build and test

From this directory:

```text
mvn clean test
```

On Windows, the same command. There is no Maven Wrapper (`mvnw` / `mvnw.cmd`) in this project.

## Run

```text
mvn spring-boot:run
```

Expected: Spring Boot starts and Tomcat listens on **8086**. There is no application endpoint yet; a request to `/` is not part of this phase’s contract.

## Coordinates

| Field | Value |
| --- | --- |
| Group | `com.healthcare.interoperability` |
| Artifact | `lab-003-spring-fhir-client` |
| Name | `lab-003-spring-fhir-client` |
| Base package | `com.healthcare.interoperability.fhirclient` |
| Main class | `com.healthcare.interoperability.fhirclient.FhirClientApplication` |

## Relation to other laboratories

- Lab #001: Patient HTTP + Java HAPI client. Closed. Do not modify.
- Lab #002: related resources (Patient, Practitioner, Organization, Encounter, Observation). Closed. Do not modify.
- Lab #003: new Spring Web application that will later consume that HAPI. This folder only.
