package com.techchallenge.fiap.cargarage.execution_service.application.entity;

import java.util.Objects;

import com.techchallenge.fiap.cargarage.execution_service.application.enums.ExecutionStatusEnum;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.InvalidDataException;
import lombok.EqualsAndHashCode;

/**
 * Value object that represents execution task status as a canonical String.
 */
@EqualsAndHashCode
public final class ExecutionStatus {

    private final String status;

    private ExecutionStatus(String status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    /**
     * Create an ExecutionStatus from a string.
     */
    public static ExecutionStatus of(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be null or blank");
        }
        String normalized = status.trim().toUpperCase();
        ExecutionStatusEnum enumVal = ExecutionStatusEnum.fromString(normalized);
        if (enumVal == null) {
            throw new InvalidDataException("Invalid execution status: " + status);
        }
        return new ExecutionStatus(enumVal.name());
    }

    public static ExecutionStatus queued() {
        return new ExecutionStatus(ExecutionStatusEnum.QUEUED.name());
    }

    public static ExecutionStatus inProgress() {
        return new ExecutionStatus(ExecutionStatusEnum.IN_PROGRESS.name());
    }

    public static ExecutionStatus completed() {
        return new ExecutionStatus(ExecutionStatusEnum.COMPLETED.name());
    }

    public static ExecutionStatus failed() {
        return new ExecutionStatus(ExecutionStatusEnum.FAILED.name());
    }

    public boolean isQueued() {
        return ExecutionStatusEnum.QUEUED.name().equals(this.status);
    }

    public boolean isInProgress() {
        return ExecutionStatusEnum.IN_PROGRESS.name().equals(this.status);
    }

    public boolean isCompleted() {
        return ExecutionStatusEnum.COMPLETED.name().equals(this.status);
    }

    public boolean isFailed() {
        return ExecutionStatusEnum.FAILED.name().equals(this.status);
    }

    public String value() {
        return status;
    }

    /**
     * Returns true if a transition from this status to the target is allowed.
     */
    public boolean canTransitionTo(ExecutionStatus target) {
        if (target == null) {
            return false;
        }
        ExecutionStatusEnum currentEnum = ExecutionStatusEnum.fromString(this.status);
        ExecutionStatusEnum targetEnum = ExecutionStatusEnum.fromString(target.status);
        if (currentEnum == null || targetEnum == null) {
            return false;
        }
        switch (currentEnum) {
            case QUEUED:
                return targetEnum == ExecutionStatusEnum.IN_PROGRESS
                        || targetEnum == ExecutionStatusEnum.FAILED;
            case IN_PROGRESS:
                return targetEnum == ExecutionStatusEnum.COMPLETED
                        || targetEnum == ExecutionStatusEnum.FAILED;
            case COMPLETED:
                return false;
            case FAILED:
                return false;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return status;
    }
}
