package com.healthcare.interoperability.eventproducer.exception;

public class BrokerUnavailableException extends RuntimeException {

    public BrokerUnavailableException(Throwable cause) {
        super("Event broker is not reachable", cause);
    }
}
