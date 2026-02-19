package com.techchallenge.fiap.cargarage.execution_service.application.presenter;

import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskStatusDto;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionTask;

/**
 * Presenter class for converting between ExecutionTask domain models and DTOs.
 */
public final class ExecutionTaskPresenter {

    private ExecutionTaskPresenter() {
    }

    /**
     * Converts an ExecutionTask domain model to a response DTO.
     */
    public static ExecutionTaskDto toResponseDto(ExecutionTask model) {
        if (model == null) {
            return null;
        }
        return ExecutionTaskDto.builder()
                .id(model.id())
                .serviceOrderId(model.serviceOrderId())
                .customerId(model.customerId())
                .vehicleId(model.vehicleId())
                .vehicleLicensePlate(model.vehicleLicensePlate())
                .description(model.description())
                .status(model.status() != null ? model.status().value() : null)
                .assignedTechnician(model.assignedTechnician())
                .notes(model.notes())
                .failureReason(model.failureReason())
                .priority(model.priority())
                .createdAt(model.createdAt())
                .updatedAt(model.updatedAt())
                .startedAt(model.startedAt())
                .completedAt(model.completedAt())
                .build();
    }

    /**
     * Converts an ExecutionTask domain model to a status DTO.
     */
    public static ExecutionTaskStatusDto toStatusDto(ExecutionTask model) {
        if (model == null) {
            return null;
        }
        return ExecutionTaskStatusDto.builder()
                .status(model.status() != null ? model.status().value() : null)
                .build();
    }
}
