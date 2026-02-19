package com.techchallenge.fiap.cargarage.execution_service.application.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import com.techchallenge.fiap.cargarage.execution_service.application.exception.InvalidDataException;
import lombok.Builder;

/**
 * Domain entity representing an Execution Task.
 * Each execution task corresponds to a service order that has been approved
 * and paid for, ready for execution in the workshop.
 */
public record ExecutionTask(
        Long id,
        Long serviceOrderId,
        Long customerId,
        Long vehicleId,
        String vehicleLicensePlate,
        String description,
        ExecutionStatus status,
        String assignedTechnician,
        String notes,
        String failureReason,
        Integer priority,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt) {

    public ExecutionTask {
        if (Objects.isNull(serviceOrderId)) {
            throw new InvalidDataException(
                    "Invalid ExecutionTask: serviceOrderId must not be null");
        }
    }

    @Builder(builderMethodName = "builder")
    public static ExecutionTask buildExecutionTask(
            Long id,
            Long serviceOrderId,
            Long customerId,
            Long vehicleId,
            String vehicleLicensePlate,
            String description,
            ExecutionStatus status,
            String assignedTechnician,
            String notes,
            String failureReason,
            Integer priority,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime startedAt,
            LocalDateTime completedAt) {
        return new ExecutionTask(
                id, serviceOrderId, customerId, vehicleId,
                vehicleLicensePlate, description, status,
                assignedTechnician, notes, failureReason,
                priority, createdAt, updatedAt, startedAt, completedAt);
    }

    public ExecutionTask withId(Long id) {
        return new ExecutionTask(
                id, this.serviceOrderId, this.customerId, this.vehicleId,
                this.vehicleLicensePlate, this.description, this.status,
                this.assignedTechnician, this.notes, this.failureReason,
                this.priority, this.createdAt, this.updatedAt,
                this.startedAt, this.completedAt);
    }

    /**
     * Return a new ExecutionTask with status changed and timestamps adjusted.
     */
    public ExecutionTask withStatusUpdated(ExecutionStatus newStatus, LocalDateTime now) {
        if (newStatus == null) {
            throw new IllegalArgumentException("newStatus must not be null");
        }

        LocalDateTime started = this.startedAt;
        LocalDateTime completed = this.completedAt;

        if (newStatus.isInProgress() && started == null) {
            started = now;
        }
        if ((newStatus.isCompleted() || newStatus.isFailed()) && completed == null) {
            completed = now;
        }

        return new ExecutionTask(
                this.id, this.serviceOrderId, this.customerId, this.vehicleId,
                this.vehicleLicensePlate, this.description, newStatus,
                this.assignedTechnician, this.notes, this.failureReason,
                this.priority, this.createdAt, now, started, completed);
    }

    /**
     * Return a new ExecutionTask with failure reason set.
     */
    public ExecutionTask withFailure(String reason, LocalDateTime now) {
        return new ExecutionTask(
                this.id, this.serviceOrderId, this.customerId, this.vehicleId,
                this.vehicleLicensePlate, this.description, ExecutionStatus.failed(),
                this.assignedTechnician, this.notes, reason,
                this.priority, this.createdAt, now, this.startedAt, now);
    }
}
