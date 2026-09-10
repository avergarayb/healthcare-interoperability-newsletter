# 03 — Search Patient by identifier

Observations from one execution against the independent FHIR Lab #001 server.

- Server: HAPI FHIR 8.12.0
- FHIR: R4 4.0.1
- Base URL: `http://localhost:18080/fhir`

The Patient data used in this laboratory is fictitious. At the time of these searches, the server held the Patient created in `02-create-read-patient.md` (`id` = `1000`, `identifier.value` = `LAB-001-0001`).

## Objective

Search for that Patient using the FHIR Search parameter `identifier`, first as `system|value` and then as value only.

## Search 1 — `system|value`

### Request

- Method: `GET`
- Endpoint: `/fhir/Patient?identifier={system}|{value}`
- system: `https://example.org/fhir/identifier/patient`
- value: `LAB-001-0001`
- Header: `Accept: application/fhir+json`

The `|` character was URL-encoded as `%7C` on the wire. The server echoed the encoded form in `Bundle.link.self`:

```text
http://127.0.0.1:18080/fhir/Patient?identifier=https%3A%2F%2Fexample.org%2Ffhir%2Fidentifier%2Fpatient%7CLAB-001-0001
```

### Observed

- HTTP status: **200**
- `Content-Type`: `application/fhir+json;charset=UTF-8`
- `resourceType`: **Bundle**
- `Bundle.type`: **searchset**
- `Bundle.total`: **1**
- `entry[0].fullUrl`: `http://127.0.0.1:18080/fhir/Patient/1000`
- `entry[0].resource.resourceType`: **Patient**
- `entry[0].resource.id`: **1000**
- `entry[0].resource.identifier.value`: **LAB-001-0001**
- `entry[0].search.mode`: **match**

Response body:

```json
{
  "resourceType": "Bundle",
  "id": "6fc5bb86-46b4-4ae9-98b3-edfebfdcb672",
  "meta": {
    "lastUpdated": "2026-09-10T03:01:06.004+00:00"
  },
  "type": "searchset",
  "total": 1,
  "link": [ {
    "relation": "self",
    "url": "http://127.0.0.1:18080/fhir/Patient?identifier=https%3A%2F%2Fexample.org%2Ffhir%2Fidentifier%2Fpatient%7CLAB-001-0001"
  } ],
  "entry": [ {
    "fullUrl": "http://127.0.0.1:18080/fhir/Patient/1000",
    "resource": {
      "resourceType": "Patient",
      "id": "1000",
      "meta": {
        "versionId": "1",
        "lastUpdated": "2026-09-10T02:52:57.464+00:00"
      },
      "identifier": [ {
        "system": "https://example.org/fhir/identifier/patient",
        "value": "LAB-001-0001"
      } ],
      "name": [ {
        "family": "Demo",
        "given": [ "Ana" ]
      } ],
      "telecom": [ {
        "system": "phone",
        "value": "+51999999999",
        "use": "mobile"
      } ],
      "gender": "female",
      "birthDate": "1990-05-15"
    },
    "search": {
      "mode": "match"
    }
  } ]
}
```

## Search 2 — value only

### Request

- Method: `GET`
- Endpoint: `/fhir/Patient?identifier=LAB-001-0001`
- Header: `Accept: application/fhir+json`

`Bundle.link.self` echoed:

```text
http://127.0.0.1:18080/fhir/Patient?identifier=LAB-001-0001
```

### Observed

- HTTP status: **200**
- `resourceType`: **Bundle**
- `Bundle.type`: **searchset**
- `Bundle.total`: **1**
- `entry[0].resource.id`: **1000**
- `entry[0].resource.identifier.value`: **LAB-001-0001**
- `entry[0].search.mode`: **match**

The Patient inside the Bundle was the same instance as in search 1. The Bundle `id` values of the two searches were different.

Response body:

```json
{
  "resourceType": "Bundle",
  "id": "bdf07613-6132-4fda-92fc-344efc59ee5a",
  "meta": {
    "lastUpdated": "2026-09-10T03:01:09.208+00:00"
  },
  "type": "searchset",
  "total": 1,
  "link": [ {
    "relation": "self",
    "url": "http://127.0.0.1:18080/fhir/Patient?identifier=LAB-001-0001"
  } ],
  "entry": [ {
    "fullUrl": "http://127.0.0.1:18080/fhir/Patient/1000",
    "resource": {
      "resourceType": "Patient",
      "id": "1000",
      "meta": {
        "versionId": "1",
        "lastUpdated": "2026-09-10T02:52:57.464+00:00"
      },
      "identifier": [ {
        "system": "https://example.org/fhir/identifier/patient",
        "value": "LAB-001-0001"
      } ],
      "name": [ {
        "family": "Demo",
        "given": [ "Ana" ]
      } ],
      "telecom": [ {
        "system": "phone",
        "value": "+51999999999",
        "use": "mobile"
      } ],
      "gender": "female",
      "birthDate": "1990-05-15"
    },
    "search": {
      "mode": "match"
    }
  } ]
}
```

## Interpretation

In this execution, FHIR Search did not return a bare `Patient`. It returned a Bundle of type `searchset` that contained the matching Patient in `entry`.

Search by `system|value` found Patient `1000`, the same instance created earlier.

Search by `identifier=LAB-001-0001` (no system) also returned HTTP 200 and the same Patient **on this server and with this data set**. At that moment only one Patient with that value existed.

That is not enough to conclude that omitting `system` is always equivalent to sending `system|value`.

If two Patients shared the same `identifier.value` under different `identifier.system` values, a value-only search might return one match, several matches, or a different match. That case was not created and was not tested.

## Limitations

These searches do not test:

- an empty result (`total = 0`)
- two Patients with the same value in different systems
- identifier uniqueness
- other search parameters
- update or delete
- behavior after the container is recreated
