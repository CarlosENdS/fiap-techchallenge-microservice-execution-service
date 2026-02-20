package com.techchallenge.fiap.cargarage.execution_service.application.usecase;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionTask;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.InvalidDataException;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.NotFoundException;
import com.techchallenge.fiap.cargarage.execution_service.application.gateway.ExecutionTaskGateway;
import com.techchallenge.fiap.cargarage.execution_service.infrastructure.messaging.ExecutionEventPublisher;

/**
 * Use case for failing an Execution Task (Saga compensation).
 */
@RequiredArgsConstructor
public class FailExecutionTaskUseCase {

    private final ExecutionTaskGateway gateway;
    private final ExecutionEventPublisher eventPublisher;

    /**
     * Marks an execution task as failed with a reason.
     *
     * @param id     the execution task ID
     * @param reason the failure reason
     * @return the failed execution task
     */
    public ExecutionTask execute(Long id, String reason) {
        ExecutionTask existing = gateway.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Execution task not found with id: " + id));

        if (existing.status().isCompleted() || existing.status().isFailed()) {
            throw new InvalidDataException(
                    "Cannot fail execution task in status: " + existing.status());
        }

        ExecutionTask failed = existing.withFailure(
                reason != null ? reason : "Execution failed",
                LocalDateTime.now());
        ExecutionTask saved = gateway.update(failed);

        eventPublisher.publishExecutionFailed(saved);

        return saved;
    }

    /**
     * Marks an execution task as failed by service order ID.
     *
     * @param serviceOrderId the service order ID
     * @param reason         the failure reason
     * @return the failed execution task
     */
    public ExecutionTask executeByServiceOrderId(Long serviceOrderId, String reason) {
        ExecutionTask existing = gateway.findByServiceOrderId(serviceOrderId)
                .orElseThrow(() -> new NotFoundException(
                        "Execution task not found for service order: " + serviceOrderId));

        if (existing.status().isCompleted() || existing.status().isFailed()) {
            throw new InvalidDataException(
                    "Cannot fail execution task in status: " + existing.status());
        }

        ExecutionTask failed = existing.withFailure(
                reason != null ? reason : "Execution failed",
                LocalDateTime.now());
        ExecutionTask saved = gateway.update(failed);

        eventPublisher.publishExecutionFailed(saved);

        return saved;
    }
}
