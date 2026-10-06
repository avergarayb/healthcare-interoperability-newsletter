# FHIR Lab #006 — Profiles and extensions

Spring Boot application that validates a FHIR R4 `Patient` against a lab-specific profile and maps a conforming instance to an application contract.

Previous laboratory: interoperability architecture (Lab #005). Labs #001–#005 are not modified by this experiment.

This laboratory defines a lab-specific profile. It is not an official Peru profile, not an HL7 national profile, not a production Implementation Guide, and not a recommendation for clinical use.

## 1. What is a FHIR profile?

A profile constrains a base FHIR resource for one implementation context. It can raise cardinality, limit types, and state which extensions are allowed.

A profile is not a new JSON schema invented beside FHIR. FHIR represents a profile as a `StructureDefinition` with `kind=resource`, `derivation=constraint`, and `baseDefinition` pointing at the base resource.

## 2. What is a FHIR extension?

An extension carries information that the base resource does not already have, using the formal `extension` array and a canonical URL.

An extension is also a `StructureDefinition`, usually `kind=complex-type`, `type=Extension`, based on `http://hl7.org/fhir/StructureDefinition/Extension`.

Adding a custom property next to `resourceType` is not a FHIR extension. This laboratory reads `preferredContactChannel` only from:

```text
extension.url = https://example.org/fhir/StructureDefinition/preferred-contact-channel
extension.valueCode
```

## 3. StructureDefinition

`StructureDefinition` is the FHIR resource that defines both profiles and extensions. This lab stores four local definitions:

| File | Resource | URL |
| --- | --- | --- |
| `fhir/lab-006-patient-profile.json` | Profile | `https://example.org/fhir/StructureDefinition/lab-006-patient` |
| `fhir/preferred-contact-channel-extension.json` | Extension | `https://example.org/fhir/StructureDefinition/preferred-contact-channel` |
| `fhir/preferred-contact-channel-valueset.json` | ValueSet | `https://example.org/fhir/ValueSet/preferred-contact-channel` |
| `fhir/preferred-contact-channel-codesystem.json` | CodeSystem | `https://example.org/fhir/CodeSystem/preferred-contact-channel` |

There is no Implementation Guide package.

## 4. Base resource and this profile

Base FHIR `Patient` leaves `identifier`, `name`, and `birthDate` optional (`0..*`, `0..*`, `0..1`).

`Lab006PatientProfile` constrains that base resource for this lab only:

| Element | Lab cardinality |
| --- | --- |
| `Patient.identifier` | `1..*` |
| `Patient.name` | `1..*` |
| `Patient.birthDate` | `1..1` |
| `preferredContactChannel` extension | `0..1` |

The `birthDate` differential sets `min` to 1 and does not set `max`. Base `Patient.birthDate` already has maximum `1`, so that maximum stays. These minimums are not universal FHIR requirements. The extension slice uses discriminator `url` and `rules: open`.

## 5. Why this extension exists

`Patient` has telecom, but it does not have a single coded “preferred contact channel”. This lab adds that fact with an extension whose `valueCode` is bound, strength `required`, to the local ValueSet `phone | email | sms`.

The profile slice `preferredContactChannel` points at that extension definition. The slice rules are `open`: other extensions are not forbidden by this small profile.

## 6. Architecture

```text
POST /api/fhir/patients/validate          this process, port 8088
        |
        v
PatientValidationController               application JSON only
        |
        v
PatientValidationService
        |
        v
FhirProfileValidationService              parse + HAPI instance validator
        |
        +-- local StructureDefinition / ValueSet / CodeSystem
        |
        v
PatientMapper
        |
        v
ValidatedHealthcarePatient
```

| Layer | Class | Responsibility |
| --- | --- | --- |
| API | `PatientValidationController` | `POST /api/fhir/patients/validate`. No HAPI types. |
| Application | `PatientValidationService` | Orders parse, profile validation, and mapping. |
| FHIR validation | `FhirProfileValidationService` | Parses JSON. Runs the instance validator against the lab profile URL. |
| Mapping | `PatientMapper` | Reads a HAPI `Patient` and returns `ValidatedHealthcarePatient`. |
| Definitions | `FhirDefinitionLoader` | Loads the JSON files under `src/main/resources/fhir/`. |

`ValidatedHealthcarePatient` does not reference HAPI classes.

Validation runs inside this process. This lab does not call `http://localhost:18080/fhir` and does not add a `RestClient`.

## 7. Request flow

1. The caller posts a FHIR JSON body.
2. The parser builds a HAPI resource. Unreadable JSON becomes HTTP 400. The response does not include the parser exception text.
3. A resource whose type is not `Patient` becomes HTTP 422 `Payload is not a Patient`.
4. A `Patient` is validated with `ValidationOptions.addProfile` set to the lab profile URL. That call is what checks conformance.
5. `meta.profile` may declare the same URL. The declaration does not make the instance valid. A declared profile with a missing `birthDate` still fails.
6. Errors from the validator are translated to stable application messages. The public body does not include the raw validator text.
7. A successful instance is mapped. `id` is copied only when `Patient.id` is present. This lab does not generate an id. `preferredContactChannel` is copied only from the formal extension.

## 8. Application contract

Success, HTTP 200:

```json
{
  "valid": true,
  "patient": {
    "id": "1000",
    "firstName": "Lucia",
    "lastName": "Example",
    "birthDate": "1988-03-20",
    "preferredContactChannel": "email"
  }
}
```

`firstName` is the first `given` of the first `HumanName`. `lastName` is `family` of that same name. `preferredContactChannel` is `valueCode` on the extension URL above. Those three choices are mapping decisions of this lab, not universal FHIR rules. If `Patient.id` is absent, `id` is omitted. The lab does not invent an id. The body has no `resourceType`, `meta`, or `identifier`.

Profile failure, HTTP 422:

```json
{
  "valid": false,
  "errors": [
    {
      "code": "profile_validation_error",
      "message": "Patient.identifier is required"
    }
  ]
}
```

Unreadable JSON, HTTP 400, uses the same `valid` / `errors` shape with code `unreadable_payload`.

Unexpected failures, HTTP 500:

```json
{"error":"internal_error","message":"An unexpected error occurred"}
```

## 9. Example valid Patient

```json
{
  "resourceType": "Patient",
  "id": "1000",
  "meta": {
    "profile": [
      "https://example.org/fhir/StructureDefinition/lab-006-patient"
    ]
  },
  "identifier": [
    {
      "system": "https://example.org/patient-id",
      "value": "LAB-006-001"
    }
  ],
  "name": [
    {
      "family": "Example",
      "given": ["Lucia"]
    }
  ],
  "birthDate": "1988-03-20",
  "extension": [
    {
      "url": "https://example.org/fhir/StructureDefinition/preferred-contact-channel",
      "valueCode": "email"
    }
  ]
}
```

The same constraints pass when `meta.profile` is absent, because the validator is invoked with the profile URL explicitly.

## 10. Example invalid Patient

A `Patient` without `identifier` is HTTP 422:

```json
{"valid":false,"errors":[{"code":"profile_validation_error","message":"Patient.identifier is required"}]}
```

Also rejected:

| Input | Application message |
| --- | --- |
| No `name` | `Patient.name is required` |
| No `birthDate` | `Patient.birthDate is required` |
| `valueCode` `fax` | `preferredContactChannel must be phone, email, or sms` |
| `valueString` on that extension URL | `preferredContactChannel extension is not valid` |
| `resourceType` `Observation` | `Payload is not a Patient` |
| Body `not-json` | `Request body is not readable FHIR JSON` (HTTP 400) |

## 11. How validation works

HAPI FHIR **8.12.0** instance validator:

1. `DefaultProfileValidationSupport` loads the packaged R4 core definitions from the classpath. No network call.
2. `PrePopulatedValidationSupport` holds the lab StructureDefinitions, ValueSet, and CodeSystem.
3. `SnapshotGeneratingValidationSupport` builds snapshots from the differentials before validation.
4. `InMemoryTerminologyServerValidationSupport` checks the required binding of `valueCode`.
5. `CommonCodeSystemsTerminologyService` is on the chain for core coded elements.
6. `ValidationSupportChain` in this HAPI version requires a cache provider, so `hapi-fhir-caching-caffeine` is on the classpath.
7. `FhirInstanceValidator` validates the instance against `https://example.org/fhir/StructureDefinition/lab-006-patient`.

On this JVM, HAPI emitted Spanish validator text (`mínimo requerido`). The API does not return that text. `ValidationMessageTranslator` maps known issues to the English messages above. Any unrecognized error becomes `Patient does not conform to Lab006PatientProfile`.

Schematron is not on the classpath. HAPI logs that it will not run schematron. This lab uses the instance validator.

## 12. Build and test

From `experiments/fhir-lab-006-profiles-extensions`:

```text
mvn clean test
mvn spring-boot:run
```

Java 21. Maven. No wrapper. Spring Boot **3.5.16**. Port **8088**.

`mvn clean test` at closure, 2026-10-05:

```text
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-10-05T19:02:56-05:00
```

Tests load definitions, translate validator messages, map a Patient, exercise the controller (200, 422, 400, 500 without internal text), and run the instance validator locally for the cases in section 10. They do not call a FHIR server.

## 13. Manual tests

```text
curl.exe -sS -D - -H "Content-Type: application/fhir+json" --data-binary "@valid.json" http://localhost:8088/api/fhir/patients/validate
```

**Observed:** HTTP **200**

```json
{"valid":true,"patient":{"id":"1000","firstName":"Lucia","lastName":"Example","birthDate":"1988-03-20","preferredContactChannel":"email"}}
```

Closure run on port 8088, 2026-10-05:

| Request | HTTP | Body |
| --- | --- | --- |
| Valid Patient, `valueCode` `email` | 200 | `{"valid":true,"patient":{"id":"1000","firstName":"Lucia","lastName":"Example","birthDate":"1988-03-20","preferredContactChannel":"email"}}` |
| Missing `identifier` | 422 | `Patient.identifier is required` |
| `meta.profile` set to the lab profile, `birthDate` absent | 422 | `Patient.birthDate is required` |
| `valueCode` `fax` | 422 | `preferredContactChannel must be phone, email, or sms` |
| `Observation` | 422 | `Payload is not a Patient` |
| Body `not-json` | 400 | `unreadable_payload` / `Request body is not readable FHIR JSON` |

None of those bodies contained `HAPI-`, `ca.uhn`, a stack trace, or a Java exception class name.

`fax` is rejected by the instance validator. The extension binds `valueCode` with strength `required` to the local ValueSet. `InMemoryTerminologyServerValidationSupport` reports that `fax` is not in that ValueSet. `PatientMapper` does not keep a code list.

## 14. Difficulties

| Issue | What happened |
| --- | --- |
| `ValidationSupportContext` package | In HAPI 8.12.0 the class is `ca.uhn.fhir.context.support.ValidationSupportContext`. |
| Cache provider | `ValidationSupportChain` throws `HAPI-2200` unless `hapi-fhir-caching-caffeine` is present. |
| Validator language | This JVM received Spanish issue text. The public API keeps stable English messages and does not echo the validator sentence. |

## 15. Limitations

This laboratory does not implement:

- an Implementation Guide or a national Patient profile
- HL7 v2
- a terminology server, SNOMED CT, or LOINC
- SMART on FHIR or OAuth
- a database
- microservices, Kafka, RabbitMQ, or events
- AI

It also does not:

- validate every element of `Patient`
- close the extension slice (`rules` is `open`)
- parse `OperationOutcome` from an external server
- call the HAPI server on port 18080
- persist the validated patient
- treat `meta.profile` as proof of conformance

The layering is one Spring Boot process. It is not a microservice deployment.

## 16. Status

**Lab #006 is closed.** The closure review did not change the profile, the extension, or the validator chain. The path remains `POST /api/fhir/patients/validate` → local profile validation → `ValidatedHealthcarePatient`.

## Coordinates

| Field | Value |
| --- | --- |
| Group | `com.healthcare.interoperability` |
| Artifact | `lab-006-profiles-extensions` |
| Base package | `com.healthcare.interoperability.profiles` |
| Local endpoint | `POST /api/fhir/patients/validate` |
| Port | 8088 |
| HAPI FHIR | 8.12.0 |

Decision record: [docs/decisions.md](docs/decisions.md).
