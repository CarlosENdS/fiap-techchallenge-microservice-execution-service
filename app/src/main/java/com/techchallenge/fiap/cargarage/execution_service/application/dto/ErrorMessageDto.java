package com.techchallenge.fiap.cargarage.execution_service.application.dto;

import java.time.LocalDateTime;

import lombok.Builder;

/**
 * DTO for error messages returned by the API.
 */
@Builder
public record ErrorMessageDto(
        String error,
        String message,
        int status,
        String path,
        LocalDateTime timestamp) {
}
