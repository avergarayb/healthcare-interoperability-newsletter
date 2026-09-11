# 04 — Java Client Read Patient

Observations from one Java execution against the independent FHIR Lab #001 server.

- Server: HAPI FHIR 8.12.0
- FHIR: R4 4.0.1
- Base URL: `http://localhost:18080/fhir`

The Patient data used in this laboratory is fictitious.

A separate manual GET of the same instance was performed by the user in Postman. That HTTP check is recorded below as independent evidence, not as output of the Java client.

## Objective

Consume an existing FHIR R4 `Patient` from Java using the HAPI FHIR Generic Client, and obtain the official R4 `Patient` model (`org.hl7.fhir.r4.model.Patient`) rather than a custom DTO.

The Java operation under test was a FHIR **read** of `Patient/1000`.

## Environment

| Item | Value |
| --- | --- |
| Java | 21.0.2 |
| Spring Boot | 3.5.16 |
| HAPI FHIR | 8.12.0 |
| FHIR | R4 4.0.1 |
| HAPI FHIR server base URL | `http://localhost:18080/fhir` |
| Patient ID | `1000` |

## Implementation

The module `experiments/fhir-lab-001-patient/java-client` is a minimal Spring Boot application.

- One `FhirContext` is created with `FhirContext.forR4()`.
- A Generic / Fluent client is created with `newRestfulGenericClient(baseUrl)`.
- Base URL and Patient id come from `application.yml` (`fhir.server.base-url`, `fhir.patient.id`).
- On startup, an `ApplicationRunner` calls:

```text
client.read()
      .resource(Patient.class)
      .withId(patientId)
      .execute()
```

`patientId` is the configured string `1000`. The return type is `org.hl7.fhir.r4.model.Patient`.

## Request Flow

```text
Spring Boot 3.5.16
        ↓
FhirContext.forR4()
        ↓
IGenericClient (base URL http://localhost:18080/fhir)
        ↓
read().resource(Patient.class).withId("1000").execute()
        ↓
GET /fhir/Patient/1000   ← FHIR read interaction (not printed by the app)
        ↓
HAPI FHIR Server 8.12.0
        ↓
org.hl7.fhir.r4.model.Patient
```

The HTTP path is the FHIR **read** interaction performed by the Generic Client. The Java application did not print status codes or headers. The fields below were observed on the `Patient` Java model after `execute()` returned.

## Observed

From the Java run (`mvn -q spring-boot:run`):

- Spring Boot 3.5.16 started with Java 21.0.2.
- HAPI logged `HAPI FHIR version 8.12.0`.
- HAPI logged `Creating new FHIR context for FHIR version [R4]`.
- The runner logged `Reading FHIR Patient 1000 from http://localhost:18080/fhir`.
- `execute()` returned `org.hl7.fhir.r4.model.Patient`.
- The process printed the fields listed in **Patient Fields Read**.
- The process ended with **exit code 0**.

The Java client did not print an HTTP status. It did not dump raw FHIR JSON as its primary result.

Independent manual check (user, Postman; not Java client output):

- Request: `GET http://127.0.0.1:18080/fhir/Patient/1000`
- Header: `Accept: application/fhir+json`
- Result: **HTTP 200 OK**
- Body: FHIR `Patient` with the same `id`, identifier, name, telecom, gender, and `birthDate` as the Java model fields above

Those two observations agree on the resource content. They are not the same measurement.

## Patient Fields Read

Values printed by the Java application from R4 model getters (`getIdElement().getIdPart()`, `getIdentifier()`, `getName()`, `getTelecom()`, `getGender()`, `getBirthDate()`). Not from a custom Patient DTO.

| Field | Value | Source |
| --- | --- | --- |
| Resource ID | 1000 | Patient Java model |
| Identifier | `https://example.org/fhir/identifier/patient\|LAB-001-0001` | Patient Java model |
| Name | Ana Demo | Patient Java model |
| Telecom | phone +51999999999 (mobile) | Patient Java model |
| Gender | female | Patient Java model |
| Birth Date | 1990-05-15 | Patient Java model |

## Interpretation

This run shows, for this laboratory stack only:

- A HAPI FHIR Generic Client configured with `FhirContext.forR4()` can talk to this R4 server.
- The fluent `read()` API performs the FHIR read of `Patient/1000`.
- HAPI represents the result as `org.hl7.fhir.r4.model.Patient`.
- A Java backend can read structured `Patient` elements (`id`, `identifier`, `name`, `telecom`, `gender`, `birthDate`) from that model.
- A custom DTO was not required to represent `Patient`.

It does not show that every FHIR server, every resource type, or every interaction behaves the same way.

## Error Handling

Observed on this successful run:

- No `ResourceNotFoundException`.
- No connection failure.
- Process exit code **0**.

The application code also contains basic handlers for `ResourceNotFoundException` (Patient missing) and `FhirClientConnectionException` (server unavailable). Those paths were not exercised in this happy-path run.

## Limitations

- Only **read** was executed from Java.
- Only `Patient/1000` was read.
- Search, create, update, and delete were not executed from Java.
- Authentication was not tested.
- The application did not inspect HTTP status or headers.
- The experiment does not evaluate interoperability across different FHIR implementations.
- The Patient instance is fictitious.

## Reproducibility

Java client (from `experiments/fhir-lab-001-patient/java-client`):

```text
mvn -q spring-boot:run
```

Manual HTTP check (separate from the Java client):

```text
curl -i -H "Accept: application/fhir+json" "http://127.0.0.1:18080/fhir/Patient/1000"
```

Postman alternative (same manual check):

- Method: `GET`
- URL: `http://127.0.0.1:18080/fhir/Patient/1000`
- Header: `Accept: application/fhir+json`

Expected for the manual HTTP check when the instance exists: **200** and a FHIR `Patient`.

## References

- [HL7 FHIR R4 (v4.0.1)](https://hl7.org/fhir/R4/index.html)
- [HL7 FHIR R4 RESTful API — read](https://hl7.org/fhir/R4/http.html#read)
- [HL7 FHIR R4 Patient](https://hl7.org/fhir/R4/patient.html)
- [HAPI FHIR — Introduction / FhirContext](https://hapifhir.io/hapi-fhir/docs/getting_started/introduction.html)
- [HAPI FHIR — Client Introduction](https://hapifhir.io/hapi-fhir/docs/client/introduction.html)
- [HAPI FHIR — Generic (Fluent) Client](https://hapifhir.io/hapi-fhir/docs/client/generic_client.html)
- [Maven Central — hapi-fhir-client 8.12.0](https://repo1.maven.org/maven2/ca/uhn/hapi/fhir/hapi-fhir-client/8.12.0/)
