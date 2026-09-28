package com.healthcare.interoperability.integration.exception;

public class IntegrationException extends RuntimeException {

    private final boolean unavailable;

    private IntegrationException(String message, boolean unavailable, Throwable cause) {
        super(message, cause);
        this.unavailable = unavailable;
    }

    public static IntegrationException unavailable(Throwable cause) {
        return new IntegrationException("FHIR server is not reachable", true, cause);
    }

    public static IntegrationException upstreamError(Throwable cause) {
        return new IntegrationException("FHIR server returned an unexpected error", false, cause);
    }

    public boolean isUnavailable() {
        return unavailable;
    }
}
