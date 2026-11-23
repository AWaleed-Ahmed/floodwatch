package com.floodwatch.integration;

/** Thrown when an external API is down, slow or returns data we cannot trust. */
public class ExternalDataException extends RuntimeException {
    public ExternalDataException(String message) { super(message); }
    public ExternalDataException(String message, Throwable cause) { super(message, cause); }
}
