package com.techchallenge.fiap.cargarage.execution_service.infrastructure.messaging;

import java.time.LocalDateTime;

import lombok.Builder;

/**
 * DTO for Execution events sent to messaging system.
 */
@Builder
public record ExecutionEventDto(
        String eventId,
        String eventType,
        Long executionTaskId,
        Long serviceOrderId,
        Long customerId,
        Long vehicleId,
        String vehicleLicensePlate,
        String status,
        String failureReason,
        LocalDateTime timestamp) {
}
