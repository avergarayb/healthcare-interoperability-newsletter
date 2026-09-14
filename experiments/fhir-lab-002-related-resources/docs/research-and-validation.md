# Lab #002 — Research and Validation

Technical preparation for FHIR Lab #002. The `/metadata` read below was recorded before the HTTP write phase. Create / read / search evidence is in [../results/01-create-read.md](../results/01-create-read.md) and [../results/02-search.md](../results/02-search.md).

Statement kinds:

- **Official FHIR:** taken from HL7 FHIR R4 4.0.1.
- **Observed (this session):** produced by a call made while writing this file.
- **Prior Lab #001 observation:** recorded earlier; not a Lab #002 run.
- **Laboratory decision:** a choice for the later HTTP phase.
- **Not tested:** must be proven with HTTP later.

## 1. Objective of this research

Validate, before any POST:

- that the five resource types and the planned references exist in FHIR R4;
- that the planned elements are legal (including required Encounter/Observation fields);
- which codes the specification actually defines for Encounter and Observation;
- which search parameter *names* R4 defines for related queries;
- what the reused Lab #001 HAPI process (`http://localhost:18080/fhir`) declares in `/metadata`.

The design in [../README.md](../README.md) remains the scope contract. This file fills the open questions that can be answered from the specification and from a read-only metadata GET.

## 2. Official sources consulted

All R4 4.0.1:

| Source | URL | Used for |
| --- | --- | --- |
| FHIR R4 index | https://hl7.org/fhir/R4/index.html | Version line |
| Patient | https://hl7.org/fhir/R4/patient.html | Elements, cardinalities |
| Practitioner | https://hl7.org/fhir/R4/practitioner.html | Elements |
| Organization | https://hl7.org/fhir/R4/organization.html | Elements |
| Encounter | https://hl7.org/fhir/R4/encounter.html | Structure, search parameters |
| Encounter definitions | https://hl7.org/fhir/R4/encounter-definitions.html | `subject`, `participant.individual`, `serviceProvider` |
| Observation | https://hl7.org/fhir/R4/observation.html | Structure, search parameters |
| Observation definitions | https://hl7.org/fhir/R4/observation-definitions.html | `subject`, `encounter`, `value[x]` |
| References | https://hl7.org/fhir/R4/references.html | Relative `Reference.reference` |
| RESTful API | https://hl7.org/fhir/R4/http.html | create / read / search |
| Search | https://hl7.org/fhir/R4/search.html | Searchset Bundle, reference search |
| EncounterStatus | https://hl7.org/fhir/R4/valueset-encounter-status.html | `Encounter.status` codes |
| v3 ActEncounterCode | https://hl7.org/fhir/R4/v3/ActEncounterCode/vs.html | `Encounter.class` codes |
| ObservationStatus | https://hl7.org/fhir/R4/valueset-observation-status.html | `Observation.status` codes |
| Official example Observation (body temperature) | https://hl7.org/fhir/R4/observation-example-body-temperature.json | LOINC `8310-5`, UCUM `Cel` |

No secondary blogs were used to invent codes.

## 3. HTTP executed

**Laboratory decision:** Lab #002 uses the same HAPI already running for Lab #001:

- Base URL: `http://localhost:18080/fhir`
- Container: `hin-fhir-lab-001`
- No second server, no compose change, no new image

Instances created later must be **new** (`LAB-002-*`). Do not reuse `Patient/1000` or other Lab #001 resources.

| When | Method | URL | Result |
| --- | --- | --- | --- |
| First research attempt | `GET` | `http://localhost:18080/fhir/metadata` | Connection failed (server not reachable) |
| After the existing Docker HAPI was up | `GET` | `http://localhost:18080/fhir/metadata` | **200**, `Content-Type` FHIR JSON, body ~1.9 MB |

No POST, PUT, PATCH, DELETE. No search of Patient, Encounter, or Observation instances.

### Observed CapabilityStatement (second GET only)

| Field | Observed value |
| --- | --- |
| `resourceType` | CapabilityStatement |
| `fhirVersion` | **4.0.1** |
| Software | HAPI FHIR Server **8.12.0** |
| Implementation | HAPI FHIR R4 Server |

For each Lab #002 type, the server **declared** (among other interactions) `create`, `read`, and `search-type`:

| Type | create | read | search-type | Declared search params of interest |
| --- | --- | --- | --- | --- |
| Patient | yes | yes | yes | `identifier` |
| Practitioner | yes | yes | yes | `identifier` |
| Organization | yes | yes | yes | `identifier` |
| Encounter | yes | yes | yes | `subject` (reference), `patient` (reference), `identifier` |
| Observation | yes | yes | yes | `subject`, `patient`, `encounter` (all reference), `identifier` |

Official search-parameter definitions echoed by HAPI:

- Encounter.`subject` → `http://hl7.org/fhir/SearchParameter/Encounter-subject`
- Encounter.`patient` → `http://hl7.org/fhir/SearchParameter/clinical-patient`
- Observation.`subject` → `http://hl7.org/fhir/SearchParameter/Observation-subject`
- Observation.`patient` → `http://hl7.org/fhir/SearchParameter/clinical-patient`
- Observation.`encounter` → `http://hl7.org/fhir/SearchParameter/clinical-encounter`

The server also declared `update`, `delete`, `patch`, `vread`, and history on these types. Those interactions remain **out of scope** and were **not** called.

A declared capability is **not** by itself a successful create, read, or related search. Those writes/searches were later executed and recorded in `results/`.

## 4. Validation of each resource

### 4.1 Patient — Official FHIR

- Definition: administrative / demographic “who” of the subject of care.
- Planned elements are all **0..*** or **0..1**: `identifier`, `name`, `gender` (AdministrativeGender, Required when present), `birthDate`.
- No 1..1 clinical field is required to have a valid Patient instance.
- Search `identifier` (token) is official; already used in Lab #001. Not re-tested here.

**Laboratory decision:** keep the same small subset as Lab #001, with **new** identifiers.

### 4.2 Practitioner — Official FHIR

- A person involved in the provision of healthcare.
- `identifier` 0..*, `name` 0..* (`HumanName`).
- No 1..1 field required for a minimal instance.

**Laboratory decision:** `identifier` + `name` only.

### 4.3 Organization — Official FHIR

- A grouping of people or organizations.
- `identifier` 0..*, `name` 0..1 (string).
- Elements are marked with an invariant (`I`): an organization is expected to have a name and/or identifier.

**Laboratory decision:** send both `identifier` and `name`.

### 4.4 Encounter — Official FHIR

Required for any instance:

| Element | Cardinality | Type | Binding |
| --- | --- | --- | --- |
| `status` | **1..1** | `code` | EncounterStatus **Required** |
| `class` | **1..1** | `Coding` | v3 ActEncounterCode **Extensible** |

Planned optional elements (legal, not required):

| Element | Cardinality | Type |
| --- | --- | --- |
| `subject` | 0..1 | `Reference(Patient \| Group)` |
| `participant` | 0..* | Backbone; `individual` is `Reference(Practitioner \| PractitionerRole \| RelatedPerson)` |
| `serviceProvider` | 0..1 | `Reference(Organization)` |

These reference targets match the Lab #002 graph. `participant.individual` may point at Practitioner. That is official, not invented.

### 4.5 Observation — Official FHIR

Required:

| Element | Cardinality | Type | Binding |
| --- | --- | --- | --- |
| `status` | **1..1** | `code` | ObservationStatus **Required** |
| `code` | **1..1** | `CodeableConcept` | LOINC Codes **Example** |

Planned optional elements:

| Element | Cardinality | Type |
| --- | --- | --- |
| `subject` | 0..1 | `Reference(Patient \| Group \| Device \| Location)` |
| `encounter` | 0..1 | `Reference(Encounter)` |
| `value[x]` | 0..1 | Quantity, CodeableConcept, string, boolean, integer, Range, Ratio, SampledData, time, dateTime, Period |

**Official FHIR:** Observation is for measurements and point-in-time assertions, not for diagnoses (`Condition`) or full lab reports (`DiagnosticReport`).

`Observation.performer` exists and may reference Practitioner. **Laboratory decision:** omit it on the first POST; the practitioner is already on `Encounter.participant`.

### 4.6 Reference — Official FHIR

A `Reference` typically uses `reference` as a relative URL `ResourceType/id` once the target has a logical id. That is why Encounter and Observation must be created **after** the referenced instances exist, unless a transaction Bundle is used. This laboratory still excludes transaction Bundles.

## 5. Minimum elements selected

**Laboratory decision** (aligned with the design README and with R4 cardinalities):

| Resource | Elements to send | Required by R4? |
| --- | --- | --- |
| Patient | `identifier`, `name`, `gender`, `birthDate` | No; all optional |
| Practitioner | `identifier`, `name` | No |
| Organization | `identifier`, `name` | Invariant: name and/or identifier |
| Encounter | `status`, `class`, `subject`, `participant.individual`, `serviceProvider` | Only `status` and `class` are 1..1 |
| Observation | `status`, `code`, `subject`, `encounter`, `valueQuantity` | Only `status` and `code` are 1..1 |

Business identifiers (**laboratory decision**, not observed on a server):

| Resource | `identifier.value` |
| --- | --- |
| Patient | `LAB-002-PATIENT-001` |
| Practitioner | `LAB-002-PRACTITIONER-001` |
| Organization | `LAB-002-ORGANIZATION-001` |

Shared `identifier.system` (**laboratory decision**): `https://example.org/fhir/identifier/lab-002`

Do **not** reuse Lab #001 `Patient/1000` or `LAB-001-0001`.

## 6. Proposed FHIR codes and justification

These are **proposals for the later HTTP phase**, taken from official R4 value sets or the official body-temperature example. They are **not** Observed on HAPI in this session.

### Encounter.status

- **Code:** `finished`
- **System (code type):** `http://hl7.org/fhir/encounter-status`
- **On the wire:** FHIR `code` is sent as the string `finished` (Required binding).
- **Meaning (official):** “The Encounter has ended.”
- **Why:** the fictitious visit is complete; avoids `in-progress` implying a live admission workflow.

### Encounter.class

- **Code:** `AMB`
- **System:** `http://terminology.hl7.org/CodeSystem/v3-ActCode`
- **Display:** ambulatory
- **Meaning (official):** care in a facility on a non-resident / outpatient-style basis; patient is not assigned a bed.
- **Why:** matches a simple clinic visit; code is in the official ActEncounterCode expansion used by `Encounter.class`.

### Observation.status

- **Code:** `final`
- **System:** `http://hl7.org/fhir/observation-status`
- **On the wire:** string `final`.
- **Meaning (official):** the observation is complete; no further actions needed for this result.
- **Why:** same status as the official body-temperature example.

### Observation.code

- **System:** `http://loinc.org`
- **Code:** `8310-5`
- **Display:** Body temperature
- **Source:** official example `observation-example-body-temperature.json` (FHIR R4).
- **Why:** LOINC is the example binding for `Observation.code`; this code is published by HL7 in that example, not invented here.

`Observation.category` = `vital-signs` appears in that same example (`http://terminology.hl7.org/CodeSystem/observation-category`). It is **0..*** and not in the Lab #002 minimum set. Optional later; not required for a valid Observation.

### Observation.value[x]

- **Choice:** `valueQuantity` (official allowed type).
- **Example shape (official example):** `value` 36.5, `system` `http://unitsofmeasure.org`, `code` `Cel`.
- **Laboratory decision for a later POST:** a fictitious quantity such as `36.8` `Cel` (same unit system/code as the official example). The numeric value is laboratory fiction; the unit coding is official UCUM as used by HL7’s example.

### Administrative gender (Patient)

- **Code:** `female` from AdministrativeGender (`http://hl7.org/fhir/administrative-gender`), Required when `gender` is sent. Same approach as Lab #001.

## 7. Relationship model

**Official FHIR** reference paths (targets as specified):

```text
Encounter.subject                → Patient | Group
Encounter.participant.individual → Practitioner | PractitionerRole | RelatedPerson
Encounter.serviceProvider        → Organization
Observation.subject              → Patient | Group | Device | Location
Observation.encounter            → Encounter
```

**Laboratory decision** (narrower than the spec):

```text
Encounter.subject.reference                = Patient/{patientId}
Encounter.participant[0].individual.reference = Practitioner/{practitionerId}
Encounter.serviceProvider.reference        = Organization/{organizationId}
Observation.subject.reference              = Patient/{patientId}
Observation.encounter.reference            = Encounter/{encounterId}
```

`{…Id}` tokens are placeholders until a later POST returns server-assigned logical ids.

Create order remains:

```text
Patient, Practitioner, Organization
        ↓
Encounter
        ↓
Observation
```

## 8. Fictitious scenario

Not real people, not real care, not Lab #001’s Ana Demo.

| Role | Fictitious value |
| --- | --- |
| Patient name | Lucia / Example |
| Patient gender | female |
| Patient birthDate | 1988-03-20 |
| Patient identifier | `LAB-002-PATIENT-001` |
| Practitioner name | Carlos / Example |
| Practitioner identifier | `LAB-002-PRACTITIONER-001` |
| Organization name | Example Community Clinic |
| Organization identifier | `LAB-002-ORGANIZATION-001` |
| Encounter | finished ambulatory visit at that clinic |
| Observation | body temperature 36.8 °C (LOINC 8310-5) recorded for that visit |

Narrative: Lucia Example attends Example Community Clinic. Practitioner Carlos Example participates. One body-temperature Observation is recorded and linked to the Encounter.

Logical ids are **unknown** until create runs. Do not assume `1000` or any other number.

## 9. Search parameters validated against R4 (not against HAPI)

**Official FHIR** search parameter names (Encounter and Observation resource pages):

| Planned call | Official parameter | Type | Expression |
| --- | --- | --- | --- |
| `GET /Encounter?subject=Patient/{id}` | `subject` | reference | `Encounter.subject` (Patient, Group) |
| `GET /Encounter?patient={id}` | `patient` | reference | `Encounter.subject` where target is Patient |
| `GET /Observation?subject=Patient/{id}` | `subject` | reference | `Observation.subject` |
| `GET /Observation?patient={id}` | `patient` | reference | `Observation.subject` where target is Patient |
| `GET /Observation?encounter=Encounter/{id}` | `encounter` | reference | `Observation.encounter` |

The names planned in the Lab #002 README (`Encounter?subject=`, `Observation?subject=`, `Observation?encounter=`) are **official R4 names**.

Official alternatives: `patient` is a common-parameter alias when the subject is a Patient. Either form is specification-legal. **Laboratory decision:** prefer the explicit `subject=Patient/{id}` and `encounter=Encounter/{id}` forms first, because they match the reference graph. Fall back to `patient=` only if a later CapabilityStatement / search test shows `subject` is undeclared or fails.

This HAPI 8.12.0 instance **declares** `subject` / `patient` on Encounter and `subject` / `patient` / `encounter` on Observation. Live searches later observed: `Encounter?subject=`, `Observation?subject=`, `Observation?encounter=` (see `results/02-search.md`). The `patient=` alias was not called.

Related official searches **not** in the first Lab #002 plan: `Encounter?practitioner=`, `Encounter?service-provider=`. They exist in R4 and may be added later.

## 10. Capabilities observed on HAPI

**Observed:** `GET /fhir/metadata` → **200** on `http://localhost:18080/fhir` (same Lab #001 container). See section 3.

HTTP create, instance GET, identifier search, and the three planned reference searches are recorded in `results/`. Not tested: `patient=` alias, missing-reference OperationOutcome, PUT/PATCH/DELETE.

## 11. Risks and limitations

- Lab #001 HAPI uses H2 without a volume. If that process is reused later, data (including Lab #001 `Patient/1000`) can disappear on recreate.
- Sharing one server mixes two laboratories’ instances. Distinct `LAB-002-*` identifiers reduce confusion; they do not isolate storage.
- Required Encounter `status`/`class` and Observation `status`/`code` will cause create failures if omitted. The proposed codes still need a live POST to prove HAPI accepts them (especially `Encounter.class` as a Coding).
- Extensible binding on `Encounter.class` allows other codes; this lab should stay on the official `AMB` coding above.
- Example binding on `Observation.code` does not force LOINC; the lab still chooses the official example code to stay inspectable.
- Search parameter *names* being official does not prove HAPI implements them.
- This file is not a result note. Do not copy proposed JSON into `results/` as if it had been posted.

## 12. HTTP experimental phase

Executed under the authorized scope. Evidence:

- [../results/01-create-read.md](../results/01-create-read.md)
- [../results/02-search.md](../results/02-search.md)

Still excluded: Java, transaction Bundle, PUT/PATCH/DELETE, OAuth, AI. No commit or push until the evidence is reviewed.

## 13. Summary table

| Topic | Kind |
| --- | --- |
| Five resources and reference paths | Official FHIR |
| Encounter `status`/`class` and Observation `status`/`code` required | Official FHIR |
| Proposed codes (`finished`, `AMB`, `final`, LOINC `8310-5`, UCUM `Cel`) | Official FHIR + laboratory choice of which official code to use |
| Search parameter names `subject`, `encounter`, `patient` | Official FHIR |
| HAPI declares those types / params | **Observed** in `/metadata` |
| Creates, reads, related searches | **Observed** in `results/` (this run) |
| New Lab #002 identifiers and names | Laboratory decision |
| Do not reuse `Patient/1000` | Laboratory decision (approved) |
