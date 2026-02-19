package com.techchallenge.fiap.cargarage.execution_service.application.enums;

/**
 * Enum representing the possible statuses of an Execution Task.
 * Workflow: QUEUED -> IN_PROGRESS -> COMPLETED
 * FAILED can be triggered from QUEUED or IN_PROGRESS (Saga compensation).
 */
public enum ExecutionStatusEnum {
    /** Task is queued and waiting for execution. */
    QUEUED,
    /** Task is currently being executed. */
    IN_PROGRESS,
    /** Task has been completed successfully. */
    COMPLETED,
    /** Task has failed (Saga compensation). */
    FAILED;

    /**
     * Parses a string to an ExecutionStatusEnum.
     *
     * @param status the string representation of the status
     * @return the corresponding enum value, or null if not found
     */
    public static ExecutionStatusEnum fromString(String status) {
        for (ExecutionStatusEnum s : ExecutionStatusEnum.values()) {
            if (s.name().equalsIgnoreCase(status)) {
                return s;
            }
        }
        return null;
    }
}
