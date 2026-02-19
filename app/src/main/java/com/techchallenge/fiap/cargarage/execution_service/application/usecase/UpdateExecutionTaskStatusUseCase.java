package com.techchallenge.fiap.cargarage.execution_service.application.usecase;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskStatusUpdateDto;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionStatus;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionTask;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.InvalidDataException;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.NotFoundException;
import com.techchallenge.fiap.cargarage.execution_service.application.gateway.ExecutionTaskGateway;
import com.techchallenge.fiap.cargarage.execution_service.infrastructure.messaging.ExecutionEventPublisher;

/**
 * Use case for updating Execution Task status.
 */
@RequiredArgsConstructor
public class UpdateExecutionTaskStatusUseCase {

    private final ExecutionTaskGateway gateway;
    private final ExecutionEventPublisher eventPublisher;

    /**
     * Updates the status of an execution task and publishes corresponding events.
     *
     * @param id        the execution task ID
     * @param statusDto the new status
     * @return the updated execution task
     */
    public ExecutionTask execute(Long id, ExecutionTaskStatusUpdateDto statusDto) {
        ExecutionTask existing = gateway.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Execution task not found with id: " + id));

        ExecutionStatus currentStatus = existing.status();
        ExecutionStatus newStatus = ExecutionStatus.of(statusDto.status());

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new InvalidDataException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        LocalDateTime now = LocalDateTime.now();
        ExecutionTask updated = existing.withStatusUpdated(newStatus, now);
        ExecutionTask saved = gateway.update(updated);

        publishStatusChangeEvent(saved, newStatus);

        return saved;
    }

    private void publishStatusChangeEvent(
            ExecutionTask task, ExecutionStatus status) {
        if (status.isInProgress()) {
            eventPublisher.publishExecutionStarted(task);
        } else if (status.isCompleted()) {
            eventPublisher.publishExecutionCompleted(task);
        } else if (status.isFailed()) {
            eventPublisher.publishExecutionFailed(task);
        }
    }
}
