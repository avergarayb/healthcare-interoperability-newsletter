package com.healthcare.interoperability.fhirintegration.exception;

public class FhirClientException extends RuntimeException {

    public enum Kind {
        UPSTREAM_HTTP,
        UNAVAILABLE,
        UNREADABLE
    }

    private final Kind kind;

    private FhirClientException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    private FhirClientException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public static FhirClientException upstreamHttp(int status) {
        return new FhirClientException(Kind.UPSTREAM_HTTP, "FHIR server returned HTTP " + status);
    }

    public static FhirClientException unavailable(Throwable cause) {
        return new FhirClientException(Kind.UNAVAILABLE, "FHIR server is not reachable", cause);
    }

    public static FhirClientException unreadable(Throwable cause) {
        return new FhirClientException(Kind.UNREADABLE, "FHIR response could not be read", cause);
    }

    public static FhirClientException unreadable() {
        return new FhirClientException(Kind.UNREADABLE, "FHIR response could not be read");
    }
}
