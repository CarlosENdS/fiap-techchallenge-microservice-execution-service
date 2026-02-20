package com.techchallenge.fiap.cargarage.execution_service.application.exception;

/**
 * Exception thrown when input data is invalid.
 */
public class InvalidDataException extends RuntimeException {
    public InvalidDataException(String message) {
        super(message);
    }
}
