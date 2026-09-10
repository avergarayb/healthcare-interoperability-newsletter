# 01 — GET /metadata

Observations from one execution against the independent FHIR Lab #001 server.

- Server: HAPI FHIR 8.12.0
- FHIR: R4 4.0.1
- Base URL: `http://localhost:18080/fhir`

All notes below describe that execution only.

## Objective

Inspect the capabilities declared by the FHIR server before creating, reading, or searching a `Patient`.

`GET /metadata` returns a CapabilityStatement. That resource is the server's description of what it says it supports.

## Request

- Method: `GET`
- Endpoint: `/fhir/metadata`
- Full URL: `http://localhost:18080/fhir/metadata`
- Header: `Accept: application/fhir+json`

## Observed

- HTTP status: **200**
- `Content-Type`: `application/fhir+json`
- `resourceType`: **CapabilityStatement**
- Software name: HAPI FHIR Server
- Software version: **8.12.0**
- `fhirVersion`: **4.0.1**
- Implementation description: HAPI FHIR R4 Server

`Patient` appeared in `rest.resource` and declared at least these interactions:

- `create`
- `read`
- `search-type`

Representative fragment (not the full CapabilityStatement):

```json
{
  "resourceType": "CapabilityStatement",
  "software": {
    "name": "HAPI FHIR Server",
    "version": "8.12.0"
  },
  "fhirVersion": "4.0.1",
  "rest": [
    {
      "mode": "server",
      "resource": [
        {
          "type": "Patient",
          "interaction": [
            { "code": "search-type" },
            { "code": "read" },
            { "code": "create" }
          ]
        }
      ]
    }
  ]
}
```

The server also declared other Patient interactions (`update`, `delete`, `patch`, `vread`, history). Those were present in the CapabilityStatement. They were not exercised in this laboratory step.

## Interpretation

`GET /metadata` is a discovery call. In this execution it confirmed, before any `Patient` write, that the server advertised FHIR R4 4.0.1 and that `Patient` was listed with `create`, `read`, and `search-type`.

That is useful because later POST / GET / search can be compared against what the server claimed to support.

A declared capability is not the same as a proven operation. The CapabilityStatement is what the server published. The later result notes record what actually happened when those operations were called.

## Limitations

This note does not prove that create, read, or search succeed. Those calls are documented in the later result files.

This note also does not claim that every HAPI FHIR server, or every FHIR server, publishes the same CapabilityStatement.
