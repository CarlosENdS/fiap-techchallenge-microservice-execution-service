package com.techchallenge.fiap.cargarage.execution_service.application.dto;

import lombok.Builder;

/**
 * DTO for execution task status.
 */
@Builder
public record ExecutionTaskStatusDto(String status) {
}
