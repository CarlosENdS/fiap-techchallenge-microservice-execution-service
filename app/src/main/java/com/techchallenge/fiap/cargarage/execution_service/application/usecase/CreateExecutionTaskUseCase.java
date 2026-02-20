package com.techchallenge.fiap.cargarage.execution_service.application.usecase;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskRequestDto;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionStatus;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionTask;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.BusinessException;
import com.techchallenge.fiap.cargarage.execution_service.application.gateway.ExecutionTaskGateway;

/**
 * Use case for creating a new Execution Task (adding to the execution queue).
 */
@RequiredArgsConstructor
public class CreateExecutionTaskUseCase {

    private final ExecutionTaskGateway gateway;

    /**
     * Executes the use case to create a new execution task.
     *
     * @param requestDto the execution task request data
     * @return the created execution task
     */
    public ExecutionTask execute(ExecutionTaskRequestDto requestDto) {
        // Check if an execution task already exists for this service order
        gateway.findByServiceOrderId(requestDto.serviceOrderId())
                .ifPresent(existing -> {
                    if (!existing.status().isFailed()) {
                        throw new BusinessException(
                                "Execution task already exists for service order: "
                                        + requestDto.serviceOrderId());
                    }
                });

        LocalDateTime now = LocalDateTime.now();
        ExecutionTask task = ExecutionTask.builder()
                .id(null)
                .serviceOrderId(requestDto.serviceOrderId())
                .customerId(requestDto.customerId())
                .vehicleId(requestDto.vehicleId())
                .vehicleLicensePlate(requestDto.vehicleLicensePlate())
                .description(requestDto.description())
                .status(ExecutionStatus.queued())
                .assignedTechnician(requestDto.assignedTechnician())
                .notes(null)
                .failureReason(null)
                .priority(requestDto.priority() != null ? requestDto.priority() : 0)
                .createdAt(now)
                .updatedAt(now)
                .startedAt(null)
                .completedAt(null)
                .build();

        return gateway.insert(task);
    }
}
