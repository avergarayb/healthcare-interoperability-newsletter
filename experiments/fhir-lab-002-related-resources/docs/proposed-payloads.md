# Lab #002 — Proposed create payloads

**Proposal used by the HTTP phase.** Live create / read / search evidence is in [../results/01-create-read.md](../results/01-create-read.md) and [../results/02-search.md](../results/02-search.md). The JSON below is the body that was planned; placeholders were replaced with server-assigned ids before POST.

- Server (planned): `http://localhost:18080/fhir`
- Identifier system: `https://example.org/fhir/identifier/lab-002`
- Data are fictitious. Do not reuse Lab #001 `Patient/1000`.

Placeholder tokens (replace after each create, never send these strings as real ids):

| Token | Replace with |
| --- | --- |
| `{{hapi-patient-id}}` | `id` returned by `POST /Patient` |
| `{{hapi-practitioner-id}}` | `id` returned by `POST /Practitioner` |
| `{{hapi-organization-id}}` | `id` returned by `POST /Organization` |
| `{{hapi-encounter-id}}` | `id` returned by `POST /Encounter` |

Create order: Patient → Practitioner → Organization → Encounter → Observation.

No `id` or `meta` is sent. No `Observation.performer`.

## 1. Patient — `POST /Patient`

```json
{
  "resourceType": "Patient",
  "identifier": [
    {
      "system": "https://example.org/fhir/identifier/lab-002",
      "value": "LAB-002-PATIENT-001"
    }
  ],
  "name": [
    {
      "family": "Example",
      "given": ["Lucia"]
    }
  ],
  "gender": "female",
  "birthDate": "1988-03-20"
}
```

## 2. Practitioner — `POST /Practitioner`

```json
{
  "resourceType": "Practitioner",
  "identifier": [
    {
      "system": "https://example.org/fhir/identifier/lab-002",
      "value": "LAB-002-PRACTITIONER-001"
    }
  ],
  "name": [
    {
      "family": "Example",
      "given": ["Carlos"]
    }
  ]
}
```

## 3. Organization — `POST /Organization`

```json
{
  "resourceType": "Organization",
  "identifier": [
    {
      "system": "https://example.org/fhir/identifier/lab-002",
      "value": "LAB-002-ORGANIZATION-001"
    }
  ],
  "name": "Example Community Clinic"
}
```

## 4. Encounter — `POST /Encounter`

Do not POST until the three ids above exist. Substitute the placeholders.

```json
{
  "resourceType": "Encounter",
  "status": "finished",
  "class": {
    "system": "http://terminology.hl7.org/CodeSystem/v3-ActCode",
    "code": "AMB",
    "display": "ambulatory"
  },
  "subject": {
    "reference": "Patient/{{hapi-patient-id}}"
  },
  "participant": [
    {
      "individual": {
        "reference": "Practitioner/{{hapi-practitioner-id}}"
      }
    }
  ],
  "serviceProvider": {
    "reference": "Organization/{{hapi-organization-id}}"
  }
}
```

## 5. Observation — `POST /Observation`

Do not POST until Patient and Encounter ids exist. No `performer`.

```json
{
  "resourceType": "Observation",
  "status": "final",
  "code": {
    "coding": [
      {
        "system": "http://loinc.org",
        "code": "8310-5",
        "display": "Body temperature"
      }
    ],
    "text": "Body temperature"
  },
  "subject": {
    "reference": "Patient/{{hapi-patient-id}}"
  },
  "encounter": {
    "reference": "Encounter/{{hapi-encounter-id}}"
  },
  "valueQuantity": {
    "value": 36.8,
    "unit": "Cel",
    "system": "http://unitsofmeasure.org",
    "code": "Cel"
  }
}
```

## Status of this file

| Item | Kind |
| --- | --- |
| JSON bodies | Proposal / laboratory decision (posted in the HTTP phase) |
| Codes and systems | From R4 / official example (see research note) |
| POST / instance GET / search | **Observed** — see `results/01-create-read.md` and `results/02-search.md` |
| Server-assigned ids (this run) | Patient `1000`, Practitioner `1001`, Organization `1002`, Encounter `1003`, Observation `1004` |
