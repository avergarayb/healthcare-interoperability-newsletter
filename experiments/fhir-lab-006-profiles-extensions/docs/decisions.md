# Lab #006 — Technical decisions

Statement kinds:

- **Laboratory decision:** a choice for this experiment.
- **Observed:** recorded while running HAPI FHIR 8.12.0 on this JVM.
- **General principle:** a FHIR conformance idea. Not a claim that every healthcare system uses this profile.

## ADR-001 — A lab profile, not an official one

**Status:** Accepted.

**Decision:** Define `Lab006PatientProfile` at `https://example.org/fhir/StructureDefinition/lab-006-patient`.

Constraints, for this lab only:

- `Patient.identifier` minimum 1
- `Patient.name` minimum 1
- `Patient.birthDate` minimum 1

**Reasons:** The base `Patient` leaves those elements optional. Raising the minimum shows what a profile changes. Adding clinical rules would not make the mechanism clearer.

The `birthDate` differential sets the minimum and does not set a maximum. Base `Patient.birthDate` maximum `1` remains, so the lab cardinality is `1..1`.

**This is not** an official Peru profile, an HL7 national profile, a production Implementation Guide, or a recommendation for clinical use. The canonical URLs use `example.org`.

## ADR-002 — One extension, formally defined

**Status:** Accepted.

**Decision:** `PreferredContactChannel` is a `StructureDefinition` of kind `complex-type`, based on `Extension`. The profile slice references that URL. The value is `valueCode`, required binding to a local ValueSet whose CodeSystem contains `phone`, `email`, and `sms`.

**Reasons:** `Patient` has no single element for a preferred channel. A property added beside `resourceType` would not be an extension. The ValueSet is small enough to validate in memory with `InMemoryTerminologyServerValidationSupport`.

The channel is a technical example. It is not a recommended contact policy.

## ADR-003 — StructureDefinition files, no package registry

**Status:** Accepted.

**Decision:** Store the profile, extension, ValueSet, and CodeSystem as JSON on the classpath. Generate snapshots at startup with `SnapshotGeneratingValidationSupport`. Do not publish an NPM package and do not upload definitions to the HAPI server.

## ADR-004 — Instance validator, local only

**Status:** Accepted.

**Decision:** Use HAPI FHIR 8.12.0:

- `hapi-fhir-structures-r4`
- `hapi-fhir-validation`
- `hapi-fhir-validation-resources-r4`
- `hapi-fhir-caching-caffeine`

`FhirInstanceValidator` receives a `ValidationSupportChain` of `DefaultProfileValidationSupport`, `PrePopulatedValidationSupport`, `InMemoryTerminologyServerValidationSupport`, `CommonCodeSystemsTerminologyService`, and `SnapshotGeneratingValidationSupport`.

Every `Patient` is validated with `ValidationOptions.addProfile` set to the lab profile URL.

**Observed:** `ValidationSupportChain` in 8.12.0 throws `HAPI-2200` when no cache provider is on the classpath. `hapi-fhir-caching-caffeine` supplies that provider. It is not a business cache.

**Observed:** On this JVM the validator text was Spanish (`mínimo requerido`). The API translates known issues and does not return the validator sentence, class names, or `HAPI-` codes.

**Not done:** This process does not call `http://localhost:18080/fhir`. Schematron is not on the classpath; HAPI skips it.

`meta.profile` is a declaration. Validation still decides conformance. `setErrorForUnknownProfiles(false)` avoids turning an unrelated profile URL into the subject of this lab. The lab profile is selected by `ValidationOptions`, not by hoping `meta.profile` is enough.

## ADR-005 — Application contract stays free of HAPI types

**Status:** Accepted.

**Decision:** `ValidatedHealthcarePatient` has `id`, `firstName`, `lastName`, `birthDate`, and `preferredContactChannel`. `PatientMapper` is the only translator from the HAPI `Patient`.

If `Patient.id` is absent, `id` is omitted. The lab does not invent an id.

`identifier` is required by the profile and is not copied into the application body. The profile constraint and the application fields are different contracts.

## ADR-006 — One Spring Boot process

**Status:** Accepted.

**Decision:** Do not add a database, messaging, security, or a second service. Port 8088 avoids 8086 and 8087.

Later work can add an Implementation Guide, a terminology server, or other integration styles. None of that is started here.

## HTTP behavior

| Condition | HTTP | Body |
| --- | --- | --- |
| Profile accepts the Patient | 200 | `{ "valid": true, "patient": { ... } }` |
| Profile or resource-type failure | 422 | `{ "valid": false, "errors": [...] }` |
| Unreadable JSON | 400 | `{ "valid": false, "errors": [{ "code": "unreadable_payload", ... }] }` |
| Unexpected failure | 500 | `{ "error": "internal_error", "message": "An unexpected error occurred" }` |

## Closure review

**Status:** Accepted. No change to the StructureDefinitions or to the validator chain.

The effective chain, in order, is:

1. `DefaultProfileValidationSupport` — packaged R4 base definitions, including `Patient` and `Extension`.
2. `PrePopulatedValidationSupport` — this lab's profile snapshot, extension snapshot, ValueSet, and CodeSystem.
3. `InMemoryTerminologyServerValidationSupport` — required binding of `valueCode`. This module is what rejects `fax`.
4. `CommonCodeSystemsTerminologyService` — core coded elements used by the base `Patient`.
5. `SnapshotGeneratingValidationSupport` — already used to build snapshots before the validator runs; left on the chain for any further snapshot lookup.

Local definitions are consulted before the in-memory terminology service, so the lab ValueSet is visible when the binding is checked. Core definitions come first so the base `Patient` and `Extension` snapshots can be resolved. No request is sent to `localhost:18080`. The tests load definitions from the classpath.

`meta.profile` with the lab URL and a missing `birthDate` still returns HTTP 422. The declaration does not prove conformance.
