package com.techchallenge.fiap.cargarage.execution_service.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * DTO for updating execution task status.
 */
@Builder
public record ExecutionTaskStatusUpdateDto(
        @NotBlank(message = "status is required") String status) {
}
