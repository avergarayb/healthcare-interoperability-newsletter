# 02 — Identifier and related-resource search

Observations from the same authorized HTTP execution as [01-create-read.md](01-create-read.md).

- Server: HAPI FHIR 8.12.0
- FHIR: R4 4.0.1
- Base URL: `http://localhost:18080/fhir`

Instances present at search time:

| Resource | `id` | `identifier.value` |
| --- | --- | --- |
| Patient | 1000 | `LAB-002-PATIENT-001` |
| Practitioner | 1001 | `LAB-002-PRACTITIONER-001` |
| Organization | 1002 | `LAB-002-ORGANIZATION-001` |
| Encounter | 1003 | (none) |
| Observation | 1004 | (none) |

No `_include`, `_revinclude`, unfiltered type search, `patient=` alias, PUT, PATCH, or DELETE was used.

## Objective

Search only the created instances: by `identifier` (`system|value`) on Patient, Practitioner, and Organization; by declared reference parameters `Encounter.subject`, `Observation.subject`, and `Observation.encounter`.

## Search 1 — Patient by identifier

### Request

- Method: `GET`
- URL: `http://localhost:18080/fhir/Patient?identifier=https://example.org/fhir/identifier/lab-002|LAB-002-PATIENT-001`
- Parameter: `identifier` = `{system}|{value}`
- Header: `Accept: application/fhir+json`

`|` was URL-encoded as `%7C`. The server echoed the encoded form in `Bundle.link.self`:

```text
http://localhost:18080/fhir/Patient?identifier=https%3A%2F%2Fexample.org%2Ffhir%2Fidentifier%2Flab-002%7CLAB-002-PATIENT-001
```

### Observed

- HTTP status: **200**
- `Content-Type`: `application/fhir+json;charset=UTF-8`
- `resourceType`: **Bundle**
- `Bundle.type`: **searchset**
- `Bundle.total`: **1**
- `entry[0].fullUrl`: `http://localhost:18080/fhir/Patient/1000`
- `entry[0].resource.id`: **1000**
- `entry[0].resource.identifier.value`: **LAB-002-PATIENT-001**
- `entry[0].search.mode`: **match**

```json
{
  "resourceType": "Bundle",
  "id": "edef2c9c-e676-4d3f-ad35-b16d7e6ffcfa",
  "meta": {
    "lastUpdated": "2026-09-14T01:54:32.023+00:00"
  },
  "type": "searchset",
  "total": 1,
  "link": [ {
    "relation": "self",
    "url": "http://localhost:18080/fhir/Patient?identifier=https%3A%2F%2Fexample.org%2Ffhir%2Fidentifier%2Flab-002%7CLAB-002-PATIENT-001"
  } ],
  "entry": [ {
    "fullUrl": "http://localhost:18080/fhir/Patient/1000",
    "resource": {
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
    },
    "search": {
      "mode": "match"
    }
  } ]
}
```

## Search 2 — Practitioner by identifier

### Request

- Method: `GET`
- URL: `http://localhost:18080/fhir/Practitioner?identifier=https://example.org/fhir/identifier/lab-002|LAB-002-PRACTITIONER-001`
- Parameter: `identifier` = `{system}|{value}`
- Header: `Accept: application/fhir+json`

`Bundle.link.self`:

```text
http://localhost:18080/fhir/Practitioner?identifier=https%3A%2F%2Fexample.org%2Ffhir%2Fidentifier%2Flab-002%7CLAB-002-PRACTITIONER-001
```

### Observed

- HTTP status: **200**
- `Bundle.type`: **searchset**
- `Bundle.total`: **1**
- Match: **Practitioner/1001**, `identifier.value` = `LAB-002-PRACTITIONER-001`
- `entry[0].search.mode`: **match**

```json
{
  "resourceType": "Bundle",
  "id": "32b6965e-80ed-450b-9896-c2dcd2164f47",
  "meta": {
    "lastUpdated": "2026-09-14T01:54:32.257+00:00"
  },
  "type": "searchset",
  "total": 1,
  "link": [ {
    "relation": "self",
    "url": "http://localhost:18080/fhir/Practitioner?identifier=https%3A%2F%2Fexample.org%2Ffhir%2Fidentifier%2Flab-002%7CLAB-002-PRACTITIONER-001"
  } ],
  "entry": [ {
    "fullUrl": "http://localhost:18080/fhir/Practitioner/1001",
    "resource": {
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
    },
    "search": {
      "mode": "match"
    }
  } ]
}
```

## Search 3 — Organization by identifier

### Request

- Method: `GET`
- URL: `http://localhost:18080/fhir/Organization?identifier=https://example.org/fhir/identifier/lab-002|LAB-002-ORGANIZATION-001`
- Parameter: `identifier` = `{system}|{value}`
- Header: `Accept: application/fhir+json`

`Bundle.link.self`:

```text
http://localhost:18080/fhir/Organization?identifier=https%3A%2F%2Fexample.org%2Ffhir%2Fidentifier%2Flab-002%7CLAB-002-ORGANIZATION-001
```

### Observed

- HTTP status: **200**
- `Bundle.type`: **searchset**
- `Bundle.total`: **1**
- Match: **Organization/1002**, `identifier.value` = `LAB-002-ORGANIZATION-001`
- `entry[0].search.mode`: **match**

```json
{
  "resourceType": "Bundle",
  "id": "574cc54c-5071-43ea-b3c2-ab17c652e188",
  "meta": {
    "lastUpdated": "2026-09-14T01:54:32.321+00:00"
  },
  "type": "searchset",
  "total": 1,
  "link": [ {
    "relation": "self",
    "url": "http://localhost:18080/fhir/Organization?identifier=https%3A%2F%2Fexample.org%2Ffhir%2Fidentifier%2Flab-002%7CLAB-002-ORGANIZATION-001"
  } ],
  "entry": [ {
    "fullUrl": "http://localhost:18080/fhir/Organization/1002",
    "resource": {
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
    },
    "search": {
      "mode": "match"
    }
  } ]
}
```

## Search 4 — Encounter by subject

### Request

- Method: `GET`
- URL: `http://localhost:18080/fhir/Encounter?subject=Patient/1000`
- Parameter: `subject` = `Patient/1000` (declared on `/metadata` as Encounter search parameter `subject`)
- Header: `Accept: application/fhir+json`

`Bundle.link.self`:

```text
http://localhost:18080/fhir/Encounter?subject=Patient%2F1000
```

### Observed

- HTTP status: **200**
- `Bundle.type`: **searchset**
- `Bundle.total`: **1**
- Match: **Encounter/1003**
- `entry[0].resource.subject.reference`: `Patient/1000`
- `entry[0].search.mode`: **match**

```json
{
  "resourceType": "Bundle",
  "id": "10444adc-8d92-4036-bf88-e342214aa766",
  "meta": {
    "lastUpdated": "2026-09-14T01:54:32.383+00:00"
  },
  "type": "searchset",
  "total": 1,
  "link": [ {
    "relation": "self",
    "url": "http://localhost:18080/fhir/Encounter?subject=Patient%2F1000"
  } ],
  "entry": [ {
    "fullUrl": "http://localhost:18080/fhir/Encounter/1003",
    "resource": {
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
    },
    "search": {
      "mode": "match"
    }
  } ]
}
```

## Search 5 — Observation by subject

### Request

- Method: `GET`
- URL: `http://localhost:18080/fhir/Observation?subject=Patient/1000`
- Parameter: `subject` = `Patient/1000` (declared on `/metadata` as Observation search parameter `subject`)
- Header: `Accept: application/fhir+json`

`Bundle.link.self`:

```text
http://localhost:18080/fhir/Observation?subject=Patient%2F1000
```

### Observed

- HTTP status: **200**
- `Bundle.type`: **searchset**
- `Bundle.total`: **1**
- Match: **Observation/1004**
- `entry[0].resource.subject.reference`: `Patient/1000`
- `entry[0].search.mode`: **match**

```json
{
  "resourceType": "Bundle",
  "id": "46f6b3b4-fa5a-4a0c-99c8-854ef5b755b1",
  "meta": {
    "lastUpdated": "2026-09-14T01:54:32.445+00:00"
  },
  "type": "searchset",
  "total": 1,
  "link": [ {
    "relation": "self",
    "url": "http://localhost:18080/fhir/Observation?subject=Patient%2F1000"
  } ],
  "entry": [ {
    "fullUrl": "http://localhost:18080/fhir/Observation/1004",
    "resource": {
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
    },
    "search": {
      "mode": "match"
    }
  } ]
}
```

## Search 6 — Observation by encounter

### Request

- Method: `GET`
- URL: `http://localhost:18080/fhir/Observation?encounter=Encounter/1003`
- Parameter: `encounter` = `Encounter/1003` (declared on `/metadata` as Observation search parameter `encounter`)
- Header: `Accept: application/fhir+json`

`Bundle.link.self`:

```text
http://localhost:18080/fhir/Observation?encounter=Encounter%2F1003
```

### Observed

- HTTP status: **200**
- `Bundle.type`: **searchset**
- `Bundle.total`: **1**
- Match: **Observation/1004**
- `entry[0].resource.encounter.reference`: `Encounter/1003`
- `entry[0].search.mode`: **match**

```json
{
  "resourceType": "Bundle",
  "id": "6c682cf3-6333-4949-8ec8-5e4a28a13740",
  "meta": {
    "lastUpdated": "2026-09-14T01:54:32.511+00:00"
  },
  "type": "searchset",
  "total": 1,
  "link": [ {
    "relation": "self",
    "url": "http://localhost:18080/fhir/Observation?encounter=Encounter%2F1003"
  } ],
  "entry": [ {
    "fullUrl": "http://localhost:18080/fhir/Observation/1004",
    "resource": {
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
    },
    "search": {
      "mode": "match"
    }
  } ]
}
```

## Parameters used (this execution only)

| Search | Parameter name | Value sent |
| --- | --- | --- |
| Patient | `identifier` | `https://example.org/fhir/identifier/lab-002\|LAB-002-PATIENT-001` |
| Practitioner | `identifier` | `https://example.org/fhir/identifier/lab-002\|LAB-002-PRACTITIONER-001` |
| Organization | `identifier` | `https://example.org/fhir/identifier/lab-002\|LAB-002-ORGANIZATION-001` |
| Encounter | `subject` | `Patient/1000` |
| Observation | `subject` | `Patient/1000` |
| Observation | `encounter` | `Encounter/1003` |

Not called: `Encounter?patient=`, `Observation?patient=`, `Encounter?practitioner=`, `Encounter?service-provider=`, value-only identifier, empty-result searches.

## Interpretation

In this execution, each search returned a `Bundle` of type `searchset`, not a bare resource. Each of the six queries returned HTTP 200 and `total` = 1, and the single `entry` was the Lab #002 instance created in `01`.

`Encounter?subject=Patient/1000` found Encounter `1003`. `Observation?subject=Patient/1000` and `Observation?encounter=Encounter/1003` both found Observation `1004`. That shows these three declared reference parameters returned the stored graph on this server, with this data set, at this moment.

It does not prove the `patient` alias, include/revinclude, or behavior with more than one matching Encounter or Observation.

## Limitations

These searches do not test:

- `patient=` as an alias of `subject`
- an empty result (`total` = 0)
- two Encounters or Observations for the same Patient
- identifier uniqueness
- `_include` / `_revinclude`
- behavior after the container is recreated
