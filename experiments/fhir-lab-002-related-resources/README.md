# FHIR Lab #002 — Related Resources

Design and scope document for the second experiment in this repository.

This file is the design and scope contract. HTTP create / read / search evidence is in [results/01-create-read.md](results/01-create-read.md) and [results/02-search.md](results/02-search.md). No application code and no server or Compose change were added.

FHIR Lab #001 — Patient remains closed. This laboratory does not modify that experiment’s files, results, or Java client.

## 1. Objective

Understand how five FHIR R4 resources work together in a small clinical scene, and how their **references** express “who / where / in what encounter / what was observed”.

The central question is not “can we create another Patient?”. It is:

> How does FHIR link `Patient`, `Practitioner`, `Organization`, `Encounter`, and `Observation` so a backend can follow those links over REST?

This document does not claim that those links have already been created on a server.

## 2. Scope

**Laboratory decision:** one fictitious visit, five resource types, FHIR JSON, create / read / related search. No Java client in this design step.

| In scope | Out of scope in this design |
| --- | --- |
| Resource roles and references among the five types | Java / Spring Boot client |
| Planned HTTP create, read, and related search | Custom REST API |
| Fictitious demographic and encounter data | Authentication / SMART / OAuth |
| Distinguishing `id` from `identifier` (already learned in Lab #001) | Custom ImplementationGuides / US Core |
| Recording observations as FHIR `Observation` | Complex terminology services |
| | Transaction or batch `Bundle` |
| | Update, delete, PATCH, history |
| | AI, RAG, messaging, extra infrastructure |

Capability discovery (`GET /metadata`) was executed against the reused HAPI at `http://localhost:18080/fhir`. **Observed:** HTTP **200**. The five types declare `create`, `read`, and `search-type`. That metadata evidence is documented in [docs/research-and-validation.md](docs/research-and-validation.md). Declaration is not by itself a successful POST, instance GET, or search.

## 3. FHIR version

This experiment uses the same specification line as Lab #001:

- **FHIR R4**
- **FHIR version 4.0.1**

Resource examples, references, and search parameters should be checked against the FHIR R4 4.0.1 specification at implementation time. Do not assume R5 types or search behavior.

## 4. Resources in scope

| Resource | Official role (short) | Role in this laboratory |
| --- | --- | --- |
| `Patient` | Demographics / administrative “who” of the subject of care | The person receiving attention |
| `Practitioner` | A person involved in healthcare | The clinician who participates in the visit |
| `Organization` | A grouping of people or locations | The healthcare organization where the visit happens |
| `Encounter` | An interaction between a patient and healthcare provider(s) | The visit that ties subject, participant, and place |
| `Observation` | Measurements and simple assertions | One finding recorded about the patient in that visit |

Official definitions should be read from the R4 resource pages listed in **Sources**. This table is a laboratory reading, not a substitute for those pages.

Other clinical resources (`Condition`, `Procedure`, `MedicationRequest`, `DiagnosticReport`, and the rest) are out of scope.

## 5. Clinical scenario

Fictitious scene, not real care and not real personal data:

A patient attends a healthcare organization. A practitioner takes part in that encounter and records one observation about the patient.

Narrative used only to keep the five resources aligned:

```text
Patient          receives care
        ↓
Organization     is the care setting / provider organization
        ↓
Practitioner     participates in the visit
        ↓
Encounter        is the visit
        ↓
Observation      is one recorded finding about the Patient
                 (and may point back to the Encounter)
```

Exact display names, identifiers, encounter class, and observation code are **implementation choices**. They must stay fictitious. They are not observed results of this document.

Do not reuse Lab #001’s `Patient/1000` as a hidden dependency of this design. Whether a later run shares the Lab #001 HAPI process is an open infrastructure question (see section 13). If the same server is used, this laboratory should create **new** instances with **new** business identifiers so Lab #001 evidence stays distinguishable.

## 6. Resource responsibilities

What each resource is responsible for in this scene, and what it is not:

| Resource | Responsible for | Not responsible for (in this lab) |
| --- | --- | --- |
| `Patient` | Who is receiving care | The visit, the clinic identity, the clinical measurement |
| `Practitioner` | Who is acting as a health professional | The organization as a whole, the measurement itself |
| `Organization` | Which organization is involved | The patient’s identity, the observation value |
| `Encounter` | The context of the visit (subject, participant, service provider) | Standing in for the Patient or storing the measured value |
| `Observation` | The finding (code, value, subject, optional encounter) | Being the visit or the patient’s master record |

A backend that stores “visit + vital” as one custom DTO would hide these responsibilities. The laboratory keeps them as separate FHIR resources linked by references.

## 7. Relationships between resources

Planned reference graph (logical, not assigned ids):

```text
Organization
     ▲
     │ Encounter.serviceProvider
     │
Encounter ── Encounter.subject ──────────────► Patient
     │                                              ▲
     │ Encounter.participant.individual             │
     ▼                                              │
Practitioner                                        │
                                                    │
Observation ── Observation.subject ─────────────────┘
     │
     └── Observation.encounter ──► Encounter   (if the finding is visit-scoped)
```

**Official FHIR concept:** a `Reference` points at another resource instance, usually as `{ "reference": "ResourceType/id" }` once the target exists. Relative references of that form are the planned style.

**Laboratory decision:** create independent resources first (`Patient`, `Practitioner`, `Organization`), then `Encounter` (which needs those three ids), then `Observation` (which needs `Patient` and, if used, `Encounter`). That order avoids a transaction `Bundle`.

Optional later fields (not required for the first graph):

- `Observation.performer` → `Practitioner`
- `Encounter.participant.individual` is enough to show the practitioner on the visit

Do not invent reference paths that are not in the R4 definitions of these resources.

## 8. Planned HTTP operations

The list below is the original HTTP plan. At design time none of these calls had been executed. The later authorized run is documented in [results/01-create-read.md](results/01-create-read.md) and [results/02-search.md](results/02-search.md).

Format: FHIR JSON. **Laboratory decision:** reuse the existing Lab #001 HAPI process at `http://localhost:18080/fhir`. Do not add another container or compose file. Create **new** fictitious instances (`LAB-002-*`). Do not reuse `Patient/1000` or other Lab #001 resources.

| Step | Method | Endpoint (conceptual) | Purpose |
| --- | --- | --- | --- |
| A | `GET` | `/metadata` | Confirm declared support for the five types (create / read / search-type) |
| B | `POST` | `/Patient` | Create the subject |
| C | `POST` | `/Practitioner` | Create the clinician |
| D | `POST` | `/Organization` | Create the organization |
| E | `POST` | `/Encounter` | Create the visit referencing Patient, Practitioner, Organization |
| F | `POST` | `/Observation` | Create the finding referencing Patient and Encounter |
| G | `GET` | `/Patient/{id}`, `/Practitioner/{id}`, `/Organization/{id}`, `/Encounter/{id}`, `/Observation/{id}` | Read by server-assigned logical id |
| H | `GET` | search on related resources | Follow the graph without guessing ids from memory |

Planned related searches (exact parameters to confirm against R4 Search and the server’s CapabilityStatement):

```text
GET /Encounter?subject=Patient/{patientId}
GET /Observation?subject=Patient/{patientId}
GET /Observation?encounter=Encounter/{encounterId}
```

Expected search result shape, from Lab #001 and the specification: a `Bundle` with `type` = `searchset`, not a bare resource. That expectation is **not** an observation of Lab #002.

Out of this planned HTTP set: PUT, PATCH, DELETE, `_history`, `_include` / `_revinclude` (unless a later note explicitly adds one include as a learning extra), transaction/batch, POST search.

## 9. Expected resource references

After a successful later create sequence, the stored instances are **expected** to contain references of this form. Ids will be server-assigned; the tokens below are placeholders.

**Encounter (planned body ideas, not a live payload):**

- `subject.reference` → `Patient/{patientId}`
- `participant[].individual.reference` → `Practitioner/{practitionerId}`
- `serviceProvider.reference` → `Organization/{organizationId}`

**Observation (planned body ideas, not a live payload):**

- `subject.reference` → `Patient/{patientId}`
- `encounter.reference` → `Encounter/{encounterId}`

A later result note must record the real ids and the JSON actually returned. This section must not be copied into a results file as if it had been posted.

Minimal field sets (laboratory decision; confirm cardinalities in R4 at implementation):

| Resource | Planned elements (subset) |
| --- | --- |
| `Patient` | `identifier`, `name`, `gender` (and `birthDate` if useful) |
| `Practitioner` | `identifier`, `name` |
| `Organization` | `identifier`, `name` |
| `Encounter` | `status`, `class`, `subject`, `participant`, `serviceProvider` |
| `Observation` | `status`, `code`, `subject`, `encounter`, one `value[x]` |

`Encounter.class` and `Observation.code` were **selected in design** from [docs/research-and-validation.md](docs/research-and-validation.md) and drafted in [docs/proposed-payloads.md](docs/proposed-payloads.md). They were **sent** in the HTTP create phase. HAPI accepted Encounter and Observation with HTTP **201**. Observation/`1004` kept `valueQuantity.unit` = `Cel`. See [results/01-create-read.md](results/01-create-read.md).

## 10. What will not be implemented

Not in FHIR Lab #002 as designed here:

- Java, Spring Boot, or any new application module
- A project-owned REST API in front of FHIR
- OAuth2, SMART on FHIR, JWT, API Gateway
- Custom FHIR profiles or an ImplementationGuide
- ValueSet / CodeSystem maintenance, SNOMED distributions, terminology servers
- External HIS / LIS / EHR integration
- AI, LLM, RAG, agents
- Transaction or batch `Bundle`
- Kafka, RabbitMQ, Redis, Kubernetes
- Sharing or changing Lab #001 Java sources, results, or documented Patient instance as if they were this experiment

Lab #002 reuses the Lab #001 HAPI container (`hin-fhir-lab-001`, `http://localhost:18080/fhir`). This design does not change `experiments/fhir-lab-001-patient/infra/` or repository-root `infra/`.

## 11. Expected learning outcomes

After implementation and notes exist, the repository should be able to show:

1. Five small FHIR JSON examples that match the scenario, with server-assigned `id` values recorded.
2. That `Encounter` and `Observation` store **references**, not copies of the Patient name as their primary identity.
3. Request/response notes for create, read, and at least one related search, with Observed / Interpretation / Limitations separated.
4. That a related search returns a `searchset` `Bundle` (if that is what this server does — to be observed, not assumed in the result note).
5. Answers, from the specification and from the run, to:
   - Why is the visit not a field on `Patient`?
   - Why is the measurement not a field on `Encounter`?
   - What does `Reference` carry on the wire?
   - How do you create resources that depend on other ids without a transaction Bundle?
6. Confirmation that no item from section 10 was introduced.

If a point cannot be shown from the spec or from the run, it stays an open question.

## 12. Reproducibility considerations

- All person and organization data must remain fictitious.
- Writes must not be executed until an implementation note says so. This design executes none.
- HAPI Lab #001 used H2 **without a durable volume**. If Lab #002 reuses that process, instances disappear when the container is recreated. That is an operational risk, not a reason to add Postgres in this document.
- Logical ids (`1000`, or whatever the server assigns next) are not portable across resets. Business `identifier` values chosen for Lab #002 should be unique and documented (for example a `LAB-002-...` prefix).
- Java evidence and HTTP evidence, if both appear later, must stay separate — same rule as Lab #001.
- Result notes belong under `experiments/fhir-lab-002-related-resources/results/` when they exist. Do not append Lab #002 observations to Lab #001 result files.
- Do not treat Lab #001 `results/03` or `results/05` as proof that Encounter or Observation search works.

## 13. Decisions and remaining questions

### Decided or validated (not open)

| Topic | Status |
| --- | --- |
| FHIR server | Reuse `http://localhost:18080/fhir` (`hin-fhir-lab-001`). No second HAPI. |
| Lab #001 instances | Do not reuse `Patient/1000` or `LAB-001-*`. New `LAB-002-*` only. |
| `/metadata` | **Observed** (capability discovery only): HTTP 200. The five types **declare** `create`, `read`, `search-type`. See [docs/research-and-validation.md](docs/research-and-validation.md). This row is not create, read, or search evidence. |
| Encounter codes | Selected in design and accepted in the executed HTTP create operation: `status` = `finished`; `class` = `AMB` (`http://terminology.hl7.org/CodeSystem/v3-ActCode`). |
| Observation codes | Selected in design and accepted in the executed HTTP create operation: `status` = `final`; `code` = LOINC `8310-5`; `valueQuantity` + UCUM `Cel`. Observation/`1004` kept `unit` = `Cel`. |
| `Observation.performer` | Omitted on the first POST. |
| Declared search params | Encounter: `subject`, `patient`. Observation: `subject`, `patient`, `encounter`. Live searches observed: `identifier` (Patient / Practitioner / Organization), `Encounter?subject=`, `Observation?subject=`, `Observation?encounter=`. See [results/02-search.md](results/02-search.md). `patient=` alias **not** called. |
| `_include` | Out of the first HTTP set. Plain search only. |
| Transaction Bundle | Out of scope. Create in order: Patient, Practitioner, Organization → Encounter → Observation. |

Draft JSON for review: [docs/proposed-payloads.md](docs/proposed-payloads.md). Not results.

### HTTP phase (this run)

Answered by [results/01-create-read.md](results/01-create-read.md) and [results/02-search.md](results/02-search.md):

1. The five POSTs succeeded (HTTP **201**) after a first **400** caused by a UTF-8 BOM. Bodies used `unit` = `Cel`.
2. Assigned ids: Patient **1000**, Practitioner **1001**, Organization **1002**, Encounter **1003**, Observation **1004**. Patient `1000` is Lucia / `LAB-002-PATIENT-001`, not Lab #001 Ana / `LAB-001-0001`.
3. `Encounter?subject=Patient/1000`, `Observation?subject=Patient/1000`, and `Observation?encounter=Encounter/1003` each returned a `searchset` Bundle with `total` = 1.
4. Optional later: OperationOutcome when a reference id is missing. Not required for the happy path. Not tested.

## 14. Sources

Primary sources for this laboratory:

- [HL7 FHIR R4 (v4.0.1)](https://hl7.org/fhir/R4/index.html)
- [HL7 FHIR R4 Patient](https://hl7.org/fhir/R4/patient.html)
- [HL7 FHIR R4 Practitioner](https://hl7.org/fhir/R4/practitioner.html)
- [HL7 FHIR R4 Organization](https://hl7.org/fhir/R4/organization.html)
- [HL7 FHIR R4 Encounter](https://hl7.org/fhir/R4/encounter.html)
- [HL7 FHIR R4 Observation](https://hl7.org/fhir/R4/observation.html)
- [HL7 FHIR R4 References](https://hl7.org/fhir/R4/references.html)
- [HL7 FHIR R4 RESTful API](https://hl7.org/fhir/R4/http.html)
- [HL7 FHIR R4 Search](https://hl7.org/fhir/R4/search.html)
- [HAPI FHIR documentation](https://hapifhir.io/hapi-fhir/docs/)

Lab #001 is context only (Patient `id` vs `identifier`, searchset `Bundle`). It is not a result of Lab #002.

Secondary summaries should not replace these sources.
