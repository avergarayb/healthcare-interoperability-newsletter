# 02 — Create and read Patient

Observations from one execution against the independent FHIR Lab #001 server.

- Server: HAPI FHIR 8.12.0
- FHIR: R4 4.0.1
- Base URL: `http://localhost:18080/fhir`

The Patient data used in this laboratory is fictitious.

## Objective

Create a FHIR R4 `Patient` with a small field set, observe what the server adds on create, and read the same instance by the server-assigned `id`.

The request body did not include `id` or `meta`.

## Patient sent

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

## POST /fhir/Patient

### Request

- Method: `POST`
- Endpoint: `/fhir/Patient`
- Full URL: `http://localhost:18080/fhir/Patient`
- Headers:
  - `Content-Type: application/fhir+json`
  - `Accept: application/fhir+json`

### Observed

- HTTP status: **201**
- `Content-Type`: `application/fhir+json;charset=UTF-8`
- `Location`: `http://127.0.0.1:18080/fhir/Patient/1000/_history/1`
- `ETag`: `W/"1"`
- Assigned `id`: **1000**
- `meta.versionId`: **1**
- `meta.lastUpdated`: `2026-09-10T02:52:57.464+00:00`
- `identifier.value` remained **LAB-001-0001**
- `identifier.system` remained `https://example.org/fhir/identifier/patient`
- `name`, `telecom`, `gender`, and `birthDate` were returned as sent

Response body:

```json
{
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
}
```

## GET /fhir/Patient/1000

### Request

- Method: `GET`
- Endpoint: `/fhir/Patient/1000`
- Full URL: `http://localhost:18080/fhir/Patient/1000`
- Header: `Accept: application/fhir+json`

### Observed

- HTTP status: **200**
- `ETag`: `W/"1"`
- Body identical to the POST response
- `id`: **1000**
- `identifier.value`: **LAB-001-0001**
- Same `name`, `telecom`, `gender`, and `birthDate`

## Observed difference: `id` and `identifier`

In this execution:

| Field | Value |
| --- | --- |
| `resource.id` | `1000` |
| `identifier.value` | `LAB-001-0001` |

These were different values.

- `id` was not in the request. The server assigned `1000` and used it in `Location` and in the later GET path.
- `identifier.value` was sent by the client and returned unchanged.

## Interpretation

In this execution, create and read worked as a pair: POST produced a stored instance, and GET by the assigned `id` returned that same instance.

The server added resource envelope fields (`id`, `meta`) that the client did not send. It did not replace the submitted `identifier`.

From this execution alone, `id` and `identifier.value` had different roles: one was the instance key used in `/Patient/{id}`, the other was a value supplied in the resource body and kept as submitted.

## Limitations

This note does not claim that HAPI always assigns numeric ids, that the next create would receive `1001`, or that every FHIR server assigns ids in this way.

This note does not claim that `identifier` is always a business identifier in every FHIR deployment. It only records that, in this execution, `identifier.value` was client-supplied and distinct from `id`.

This note does not test update, delete, duplicate identifiers, or persistence after the container is recreated.
