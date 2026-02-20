package com.techchallenge.fiap.cargarage.execution_service.application.exception;

/**
 * Exception thrown when a resource is not found.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
