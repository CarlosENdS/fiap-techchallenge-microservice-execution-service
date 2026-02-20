package com.techchallenge.fiap.cargarage.execution_service.infrastructure.messaging;

import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionTask;

/**
 * Interface for publishing Execution events to messaging system.
 * Part of the Saga pattern implementation.
 */
public interface ExecutionEventPublisher {

    /**
     * Publishes event when execution is started.
     */
    void publishExecutionStarted(ExecutionTask task);

    /**
     * Publishes event when execution is completed.
     * Also notifies OS Service via execution-completed-queue.
     */
    void publishExecutionCompleted(ExecutionTask task);

    /**
     * Publishes event when execution fails (Saga compensation).
     */
    void publishExecutionFailed(ExecutionTask task);
}
