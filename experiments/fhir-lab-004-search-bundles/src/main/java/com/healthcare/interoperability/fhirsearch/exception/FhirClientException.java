package com.healthcare.interoperability.fhirsearch.exception;

public class FhirClientException extends RuntimeException {

    private final Integer statusCode;

    private FhirClientException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public static FhirClientException httpError(int statusCode) {
        return new FhirClientException("FHIR server returned HTTP " + statusCode, statusCode, null);
    }

    public static FhirClientException unavailable(Throwable cause) {
        return new FhirClientException("FHIR server is not reachable", null, cause);
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public boolean isUnavailable() {
        return statusCode == null;
    }
}
