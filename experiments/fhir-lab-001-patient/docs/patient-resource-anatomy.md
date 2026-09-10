# Patient Resource Anatomy

A practical reading of the FHIR R4 `Patient` resource, based on the instance already created and documented in FHIR Lab #001.

This document distinguishes three kinds of statements:

- **Observed:** recorded in the laboratory result notes.
- **Interpretation:** meaning drawn from those observations, limited to that run.
- **General FHIR concept:** taken from the FHIR R4 4.0.1 specification.

## 1. Purpose

This document explains the structure of a FHIR R4 4.0.1 `Patient` resource from a backend point of view, and relates that structure to the experiment already executed against HAPI FHIR 8.12.0 at `http://localhost:18080/fhir`.

The goal is technical learning. The document does not attempt to cover every element, extension, or implementation profile of `Patient`.

The executed operations used as evidence are only those already recorded in:

- [results/01-metadata.md](../results/01-metadata.md)
- [results/02-create-read-patient.md](../results/02-create-read-patient.md)
- [results/03-search-patient.md](../results/03-search-patient.md)

## 2. Patient Resource

The following JSON is the `Patient` sent in the laboratory create request. It is fictitious and is not real patient data.

```json
{
  "resourceType": "Patient",
  "identifier": [
    {
      "system": "https://example.org/fhir/identifier/patient",
      "value": "LAB-001-0001"
    }
  ],
  "name": [
    {
      "family": "Demo",
      "given": ["Ana"]
    }
  ],
  "telecom": [
    {
      "system": "phone",
      "value": "+51999999999",
      "use": "mobile"
    }
  ],
  "gender": "female",
  "birthDate": "1990-05-15"
}
```

**Observed:** this body was posted without `id` or `meta`. The server later returned the same demographic fields and added `id` and `meta`. See [02-create-read-patient.md](../results/02-create-read-patient.md).

## 3. resourceType

**General FHIR concept.** In the FHIR JSON representation, every resource instance carries a `resourceType` property. That property names the resource type. For this instance the value is `Patient`.

FHIR JSON is not an arbitrary object graph. The parser uses `resourceType` to decide which resource definition applies. Officially, the resource type is not the name of a root JSON object; it is carried as this property.

The REST type endpoint follows the same name. Creating a `Patient` is `POST /Patient`. Reading one is `GET /Patient/{id}`. The type in the URL and the `resourceType` in the body refer to the same resource definition.

**Interpretation:** a backend cannot treat a FHIR payload as “just JSON with some fields”. The payload is an instance of a named FHIR resource, serialized according to the FHIR JSON rules.

## 4. Resource id vs identifier

This distinction is the main observation of the create/read run.

| Kind | Path | Value in this execution |
| --- | --- | --- |
| Resource logical id | `Patient.id` | `1000` |
| Identifier value | `Patient.identifier[0].value` | `LAB-001-0001` |

**Observed** ([02-create-read-patient.md](../results/02-create-read-patient.md)):

- The request did not include `id`.
- After `POST /Patient`, HAPI FHIR assigned `id` = `1000`.
- `Location` referred to `Patient/1000/_history/1`.
- `GET /Patient/1000` returned that instance.
- `identifier.value` remained `LAB-001-0001`.

**General FHIR concept.** `id` is defined on `Resource`. It is the logical id of that resource instance on the server that assigned it. It is used in the instance URL `/Patient/{id}`.

`identifier` is an element of `Patient`. Each item is an `Identifier` datatype. It is part of the resource content, not the server-assigned instance key used in the path.

**Interpretation, limited to this run:** HAPI assigned the instance `id`. The client supplied `identifier`. They were different values and were used differently: `1000` in the read path, `LAB-001-0001` in the resource body and in identifier search.

This document does not claim that every FHIR server assigns numeric ids, or that the next create on this server would receive `1001`. It also does not claim that `identifier` is always a “business identifier” in every deployment. It only records the FHIR model and what this execution showed.

## 5. Identifier

**General FHIR concept.** `Patient.identifier` is a repeating element. In JSON it is an array, even when a given instance contains only one item. Official FHIR R4 Patient cardinality for `identifier` is `0..*`.

Each item is the complex datatype `Identifier`. Common parts used in this laboratory:

- `system` — namespace that gives meaning to `value` (FHIR type `uri`)
- `value` — the identifier string itself (FHIR type `string`)

`system` and `value` are read together. The same `value` can mean different things under different `system` values. Official search for a token parameter such as `identifier` uses the form `system|value`.

**Observed** ([03-search-patient.md](../results/03-search-patient.md)):

```text
GET /Patient?identifier=https://example.org/fhir/identifier/patient|LAB-001-0001
```

returned a Bundle `searchset` with `total` = 1. The match was Patient `1000`.

A second search used only the value:

```text
GET /Patient?identifier=LAB-001-0001
```

On this server, and with this data set, that search also returned HTTP 200 and the same Patient `1000`.

**Limitation:** that second result does not prove that omitting `system` is equivalent to `system|value` in general. A case with the same `value` under two different `system` values was not created and was not tested.

## 6. HumanName

**General FHIR concept.** `Patient.name` is a repeating element. Official FHIR R4 Patient cardinality is `0..*`. Each item is a `HumanName`, not a pair of first-name / last-name strings.

`HumanName` can carry several parts. This laboratory used:

- `family` — family name
- `given` — given name(s); `given` itself is repeating

A person may have more than one `HumanName` (for example official and usual). A single `HumanName` may have more than one `given`. The laboratory instance used one name with one family and one given value. That is a valid subset, not the whole model.

Related instance fragment:

```json
"name": [
  {
    "family": "Demo",
    "given": ["Ana"]
  }
]
```

**Observed:** the create request sent that `name`, and both POST and GET returned it unchanged.

Reducing FHIR names to `firstName` / `lastName` columns loses cardinality and the `HumanName` structure.

## 7. ContactPoint

**General FHIR concept.** `Patient.telecom` is a repeating element. Official FHIR R4 Patient cardinality is `0..*`. Each item is a `ContactPoint`.

Parts used in this laboratory:

- `system` — kind of contact (`phone` in the example)
- `value` — the contact value
- `use` — context of use (`mobile` in the example)

The same model can represent phone, email, and other contact kinds. No additional contact types were created in this laboratory.

Related instance fragment:

```json
"telecom": [
  {
    "system": "phone",
    "value": "+51999999999",
    "use": "mobile"
  }
]
```

**Observed:** this telecom was sent and returned unchanged. It is fictitious.

## 8. gender

**General FHIR concept.** `Patient.gender` is a `code` with a required binding to AdministrativeGender in FHIR R4: `male` | `female` | `other` | `unknown`. Official Patient cardinality is `0..1`.

It is not free text. The laboratory sent `"gender": "female"`, which is one of those codes.

FHIR uses terminologies (`CodeSystem`, `ValueSet`) to define and bind such codes. Those concepts are only noted here. They will be studied later.

**Observed:** the create/read responses kept `gender` as `female`.

## 9. birthDate

**General FHIR concept.** `Patient.birthDate` uses the FHIR primitive type `date`. Official Patient cardinality is `0..1`.

In JSON, many FHIR primitives appear as strings, but they are not an unconstrained `String`. `date` has FHIR rules for format and precision. FHIR defines its own type system (for example `date`, `dateTime`, `code`, `uri`, `id`).

**Observed:** the laboratory sent `"birthDate": "1990-05-15"` and received the same value back.

## 10. FHIR Data Types Involved

| Patient / JSON element | FHIR type / representation |
| --- | --- |
| `resourceType` | JSON property that names the resource type (see [JSON representation of Resources](https://hl7.org/fhir/R4/json.html)) |
| `id` | `id` (defined on `Resource`) |
| `identifier` | `Identifier` (`0..*`) |
| `name` | `HumanName` (`0..*`) |
| `telecom` | `ContactPoint` (`0..*`) |
| `gender` | `code` (AdministrativeGender, `0..1`) |
| `birthDate` | `date` (`0..1`) |

`resourceType` is listed because it appears on the wire in FHIR JSON. It is not a `Patient`-specific element in the Resource / DomainResource element tree.

Cardinalities in this table come from the official FHIR R4 4.0.1 `Patient` and `Resource` definitions, not from a measurement performed by this laboratory.

## 11. Collections and Cardinality

**General FHIR concept.** In FHIR JSON, an element that may repeat is serialized as an array, including when a particular instance has only one item.

From the official FHIR R4 Patient definition:

- `identifier` is repeatable (`0..*`)
- `name` is repeatable (`0..*`)
- `telecom` is repeatable (`0..*`)

The laboratory instance used one identifier, one name, and one telecom. That does not change the model: another instance may carry more than one of each.

Implementation profiles may tighten these cardinalities (for example require at least one identifier, or allow only one official name). Base FHIR and a profile are not the same constraint set.

## 12. Base FHIR vs Implementation Profiles

**General FHIR concept.**

```text
FHIR R4
  → Patient resource (base definition)
    → Implementation Guide / Profile (StructureDefinition)
      → additional constraints for a specific context
```

The base `Patient` definition describes the resource as published by HL7. An Implementation Guide can add a profile: required elements, fixed systems, value set bindings, or forbidden fields.

A resource may be valid against the base `Patient` and still fail a given profile. Validity against the base specification is not the same as conformance to a jurisdictional or project profile.

This laboratory used a small, fictitious base `Patient`. It did not apply a specific Implementation Guide. US Core and similar profiles are out of scope here.

## 13. Relation to the Executed Experiment

No new calls were made for this document. The mapping below uses only the existing result notes.

| Operation | Result note | Anatomy observed |
| --- | --- | --- |
| `GET /metadata` | [01-metadata.md](../results/01-metadata.md) | The server declared FHIR R4 4.0.1 and listed `Patient` with `create`, `read`, and `search-type`. |
| `POST /Patient` | [02-create-read-patient.md](../results/02-create-read-patient.md) | The client sent `resourceType`, `identifier`, `name`, `telecom`, `gender`, and `birthDate`. The server assigned `id` and `meta`. |
| `GET /Patient/{id}` | [02-create-read-patient.md](../results/02-create-read-patient.md) | The instance was retrieved by resource `id` (`1000`), not by `identifier.value`. |
| `GET /Patient?identifier=...` | [03-search-patient.md](../results/03-search-patient.md) | Search used the `Identifier` pair `system\|value` and returned a Bundle `searchset` containing Patient `1000`. A value-only search also matched that instance in this data set. |

## 14. Backend Perspective

From a backend point of view, this structure matters because:

- FHIR resources have a published structure, not an ad hoc JSON shape.
- Several elements are complex datatypes (`Identifier`, `HumanName`, `ContactPoint`), not scalar columns.
- Several elements are collections.
- Instance identity (`id`) and resource-level identifiers (`identifier`) are different constructs.
- Primitive values still belong to FHIR types (`date`, `code`, `id`, `uri`).
- Interoperability is exchange of resources that conform to that model, not exchange of arbitrary JSON.

This section does not describe a Spring Boot client. That client is still out of scope.

## 15. Scope and Limitations

This document:

- focuses only on `Patient`
- is based on FHIR R4 4.0.1
- uses the fictitious laboratory example
- does not cover every `Patient` element
- does not study implementation profiles in depth
- does not study terminologies in depth
- does not implement a Java client
- does not add experimental claims beyond the existing result notes

## 16. Official References

- [HL7 FHIR R4 (v4.0.1)](https://hl7.org/fhir/R4/index.html)
- [Resource Patient](https://hl7.org/fhir/R4/patient.html)
- [Resource](https://hl7.org/fhir/R4/resource.html)
- [JSON Representation of Resources](https://hl7.org/fhir/R4/json.html)
- [FHIR Data Types](https://hl7.org/fhir/R4/datatypes.html)
- [Identifier](https://hl7.org/fhir/R4/datatypes.html#Identifier)
- [HumanName](https://hl7.org/fhir/R4/datatypes.html#HumanName)
- [ContactPoint](https://hl7.org/fhir/R4/datatypes.html#ContactPoint)
- [date](https://hl7.org/fhir/R4/datatypes.html#date)
- [code](https://hl7.org/fhir/R4/datatypes.html#code)
- [id](https://hl7.org/fhir/R4/datatypes.html#id)
- [AdministrativeGender](https://hl7.org/fhir/R4/valueset-administrative-gender.html)
- [Profiling FHIR](https://hl7.org/fhir/R4/profiling.html)
- [RESTful API](https://hl7.org/fhir/R4/http.html)
- [Search](https://hl7.org/fhir/R4/search.html)
