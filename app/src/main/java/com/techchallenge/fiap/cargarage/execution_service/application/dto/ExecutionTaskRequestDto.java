package com.techchallenge.fiap.cargarage.execution_service.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * DTO for creating a new execution task request.
 */
@Builder
public record ExecutionTaskRequestDto(
        @NotNull(message = "serviceOrderId is required") Long serviceOrderId,
        Long customerId,
        Long vehicleId,
        String vehicleLicensePlate,
        String description,
        String assignedTechnician,
        Integer priority) {
}
