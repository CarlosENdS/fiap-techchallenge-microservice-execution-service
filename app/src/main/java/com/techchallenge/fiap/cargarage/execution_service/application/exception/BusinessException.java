package com.techchallenge.fiap.cargarage.execution_service.application.exception;

/**
 * Exception thrown when business validation fails.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
