# 01 — Create and read related resources

Observations from one authorized HTTP execution against the reused Lab #001 HAPI process.

- Server: HAPI FHIR 8.12.0
- FHIR: R4 4.0.1
- Base URL: `http://localhost:18080/fhir`
- Container: `hin-fhir-lab-001` (no second server, no Docker / Compose change)

All person and organization data in this laboratory are fictitious.

No PUT, PATCH, DELETE, `_include`, or transaction `Bundle` was used.

## Objective

Create five new Lab #002 instances in dependency order, record the server-assigned `id` values, substitute those ids into `Encounter` and `Observation`, and read each instance by logical id.

## Assigned ids (this execution)

| Resource | Business `identifier.value` | Server `id` |
| --- | --- | --- |
| Patient | `LAB-002-PATIENT-001` | **1000** |
| Practitioner | `LAB-002-PRACTITIONER-001` | **1001** |
| Organization | `LAB-002-ORGANIZATION-001` | **1002** |
| Encounter | (none sent) | **1003** |
| Observation | (none sent) | **1004** |

Shared `identifier.system`: `https://example.org/fhir/identifier/lab-002`.

The Patient created here is **Lucia / Example**, identifier `LAB-002-PATIENT-001`. It is not Lab #001’s Ana / Demo (`LAB-001-0001`). HAPI assigned logical id `1000` because the H2 store had no prior instances at create time. The numeric coincidence with Lab #001’s documented `Patient/1000` is observed; the resource body is a different instance.

## First POST attempt (failed)

The first three POSTs used JSON files written with a UTF-8 BOM (`Set-Content -Encoding utf8` on Windows).

| Method | URL | HTTP |
| --- | --- | --- |
| `POST` | `http://localhost:18080/fhir/Patient` | **400** |
| `POST` | `http://localhost:18080/fhir/Practitioner` | **400** |
| `POST` | `http://localhost:18080/fhir/Organization` | **400** |

Observed `OperationOutcome` (same diagnostic on each of the three):

```json
{
  "resourceType": "OperationOutcome",
  "issue": [ {
    "severity": "error",
    "code": "processing",
    "diagnostics": "HAPI-0450: Failed to parse request body as JSON resource. Error was: HAPI-1861: Failed to parse JSON encoded FHIR content: HAPI-1859: Content does not appear to be FHIR JSON, first non-whitespace character was: '﻿' (must be '{')"
  } ]
}
```

No instance was created by those three calls. The retry below used the same JSON text written as UTF-8 **without** BOM.

## POST /fhir/Patient

### Request

- Method: `POST`
- URL: `http://localhost:18080/fhir/Patient`
- Headers: `Content-Type: application/fhir+json`, `Accept: application/fhir+json`

Body sent (no `id`, no `meta`):

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

### Observed

- HTTP status: **201**
- `Location`: `http://localhost:18080/fhir/Patient/1000/_history/1`
- `ETag`: `W/"1"`
- Assigned `id`: **1000**
- `meta.versionId`: **1**
- `meta.lastUpdated`: `2026-09-14T01:53:38.669+00:00`

Response body:

```json
{
  "resourceType": "Patient",
  "id": "1000",
  "meta": {
    "versionId": "1",
    "lastUpdated": "2026-09-14T01:53:38.669+00:00"
  },
  "identifier": [ {
    "system": "https://example.org/fhir/identifier/lab-002",
    "value": "LAB-002-PATIENT-001"
  } ],
  "name": [ {
    "family": "Example",
    "given": [ "Lucia" ]
  } ],
  "gender": "female",
  "birthDate": "1988-03-20"
}
```

## POST /fhir/Practitioner

### Request

- Method: `POST`
- URL: `http://localhost:18080/fhir/Practitioner`
- Headers: `Content-Type: application/fhir+json`, `Accept: application/fhir+json`

Body sent (no `id`, no `meta`):

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

### Observed

- HTTP status: **201**
- `Location`: `http://localhost:18080/fhir/Practitioner/1001/_history/1`
- `ETag`: `W/"1"`
- Assigned `id`: **1001**
- `meta.lastUpdated`: `2026-09-14T01:53:38.913+00:00`

Response body:

```json
{
  "resourceType": "Practitioner",
  "id": "1001",
  "meta": {
    "versionId": "1",
    "lastUpdated": "2026-09-14T01:53:38.913+00:00"
  },
  "identifier": [ {
    "system": "https://example.org/fhir/identifier/lab-002",
    "value": "LAB-002-PRACTITIONER-001"
  } ],
  "name": [ {
    "family": "Example",
    "given": [ "Carlos" ]
  } ]
}
```

## POST /fhir/Organization

### Request

- Method: `POST`
- URL: `http://localhost:18080/fhir/Organization`
- Headers: `Content-Type: application/fhir+json`, `Accept: application/fhir+json`

Body sent (no `id`, no `meta`):

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

### Observed

- HTTP status: **201**
- `Location`: `http://localhost:18080/fhir/Organization/1002/_history/1`
- `ETag`: `W/"1"`
- Assigned `id`: **1002**
- `meta.lastUpdated`: `2026-09-14T01:53:38.959+00:00`

Response body:

```json
{
  "resourceType": "Organization",
  "id": "1002",
  "meta": {
    "versionId": "1",
    "lastUpdated": "2026-09-14T01:53:38.959+00:00"
  },
  "identifier": [ {
    "system": "https://example.org/fhir/identifier/lab-002",
    "value": "LAB-002-ORGANIZATION-001"
  } ],
  "name": "Example Community Clinic"
}
```

## POST /fhir/Encounter

Placeholders were replaced with the three ids above. The body was not posted until those ids existed.

### Request

- Method: `POST`
- URL: `http://localhost:18080/fhir/Encounter`
- Headers: `Content-Type: application/fhir+json`, `Accept: application/fhir+json`

Body sent:

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
    "reference": "Patient/1000"
  },
  "participant": [
    {
      "individual": {
        "reference": "Practitioner/1001"
      }
    }
  ],
  "serviceProvider": {
    "reference": "Organization/1002"
  }
}
```

### Observed

- HTTP status: **201**
- `Location`: `http://localhost:18080/fhir/Encounter/1003/_history/1`
- `ETag`: `W/"1"`
- Assigned `id`: **1003**
- `meta.lastUpdated`: `2026-09-14T01:53:56.728+00:00`
- Stored references: `Patient/1000`, `Practitioner/1001`, `Organization/1002`

Response body:

```json
{
  "resourceType": "Encounter",
  "id": "1003",
  "meta": {
    "versionId": "1",
    "lastUpdated": "2026-09-14T01:53:56.728+00:00"
  },
  "status": "finished",
  "class": {
    "system": "http://terminology.hl7.org/CodeSystem/v3-ActCode",
    "code": "AMB",
    "display": "ambulatory"
  },
  "subject": {
    "reference": "Patient/1000"
  },
  "participant": [ {
    "individual": {
      "reference": "Practitioner/1001"
    }
  } ],
  "serviceProvider": {
    "reference": "Organization/1002"
  }
}
```

## POST /fhir/Observation

Posted after Encounter `1003` existed. No `performer`. `valueQuantity.unit` and `valueQuantity.code` were both `Cel`.

### Request

- Method: `POST`
- URL: `http://localhost:18080/fhir/Observation`
- Headers: `Content-Type: application/fhir+json`, `Accept: application/fhir+json`

Body sent:

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
    "reference": "Patient/1000"
  },
  "encounter": {
    "reference": "Encounter/1003"
  },
  "valueQuantity": {
    "value": 36.8,
    "unit": "Cel",
    "system": "http://unitsofmeasure.org",
    "code": "Cel"
  }
}
```

### Observed

- HTTP status: **201**
- `Location`: `http://localhost:18080/fhir/Observation/1004/_history/1`
- `ETag`: `W/"1"`
- Assigned `id`: **1004**
- `meta.lastUpdated`: `2026-09-14T01:54:01.364+00:00`
- Stored references: `Patient/1000`, `Encounter/1003`
- `valueQuantity.unit`: **Cel** (accepted; not rewritten)

Response body:

```json
{
  "resourceType": "Observation",
  "id": "1004",
  "meta": {
    "versionId": "1",
    "lastUpdated": "2026-09-14T01:54:01.364+00:00"
  },
  "status": "final",
  "code": {
    "coding": [ {
      "system": "http://loinc.org",
      "code": "8310-5",
      "display": "Body temperature"
    } ],
    "text": "Body temperature"
  },
  "subject": {
    "reference": "Patient/1000"
  },
  "encounter": {
    "reference": "Encounter/1003"
  },
  "valueQuantity": {
    "value": 36.8,
    "unit": "Cel",
    "system": "http://unitsofmeasure.org",
    "code": "Cel"
  }
}
```

## Instance GET (only these five)

| Method | URL | HTTP | `id` | `ETag` |
| --- | --- | --- | --- | --- |
| `GET` | `http://localhost:18080/fhir/Patient/1000` | **200** | 1000 | `W/"1"` |
| `GET` | `http://localhost:18080/fhir/Practitioner/1001` | **200** | 1001 | `W/"1"` |
| `GET` | `http://localhost:18080/fhir/Organization/1002` | **200** | 1002 | `W/"1"` |
| `GET` | `http://localhost:18080/fhir/Encounter/1003` | **200** | 1003 | `W/"1"` |
| `GET` | `http://localhost:18080/fhir/Observation/1004` | **200** | 1004 | `W/"1"` |

Header on each GET: `Accept: application/fhir+json`.  
`Content-Type` on each response: `application/fhir+json;charset=UTF-8`.

Each GET body was the same resource as the corresponding POST response (`id`, `meta`, identifiers, names, references, and Observation `valueQuantity` including `unit` = `Cel`).

## Observed differences

| Item | Observed |
| --- | --- |
| Client-supplied `id` | None. Server assigned `1000`–`1004`. |
| First POST without BOM-free UTF-8 | HTTP **400**, parse error on the BOM character. |
| Successful POST | HTTP **201**, `Location` includes `/_history/1`. |
| `Location` host | `localhost` (Lab #001 notes recorded `127.0.0.1` on a different execution). |
| Patient logical id `1000` | Assigned to the **new** Lab #002 Patient after an empty H2 store. Not a read of Lab #001 Ana / Demo. |
| Encounter / Observation identity | Stored as `Reference.reference` strings (`Patient/1000`, `Practitioner/1001`, `Organization/1002`, `Encounter/1003`), not copies of names. |

## Interpretation

In this execution, create-in-order without a transaction Bundle worked: the three independent resources were created first, then Encounter referenced those ids, then Observation referenced Patient and Encounter.

Instance GET by the assigned `id` returned the stored resource. The server added `id` and `meta`; it did not replace submitted identifiers, codes, or `valueQuantity.unit` = `Cel`.

## Limitations

This note does not claim that HAPI always starts ids at `1000`, that `1000` will remain this Lab #002 Patient after a container recreate, or that every FHIR server accepts these codes.

This note does not test a missing reference target, update, delete, or persistence after the H2 process is recreated.

Related searches are in [02-search.md](02-search.md).
