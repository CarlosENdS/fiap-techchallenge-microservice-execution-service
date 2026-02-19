package com.techchallenge.fiap.cargarage.execution_service.application.dto;

import java.time.LocalDateTime;

import lombok.Builder;

/**
 * DTO for Execution Task responses.
 */
@Builder
public record ExecutionTaskDto(
        Long id,
        Long serviceOrderId,
        Long customerId,
        Long vehicleId,
        String vehicleLicensePlate,
        String description,
        String status,
        String assignedTechnician,
        String notes,
        String failureReason,
        Integer priority,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt) {
}
