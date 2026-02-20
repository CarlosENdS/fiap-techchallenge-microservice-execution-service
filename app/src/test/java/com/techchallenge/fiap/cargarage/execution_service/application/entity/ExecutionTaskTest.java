package com.techchallenge.fiap.cargarage.execution_service.application.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.techchallenge.fiap.cargarage.execution_service.application.exception.InvalidDataException;

class ExecutionTaskTest {

    private final LocalDateTime now = LocalDateTime.now();

    private ExecutionTask createDefaultTask() {
        return ExecutionTask.builder()
                .id(1L)
                .serviceOrderId(100L)
                .customerId(200L)
                .vehicleId(300L)
                .vehicleLicensePlate("ABC1D23")
                .description("Test task")
                .status(ExecutionStatus.queued())
                .assignedTechnician("Technician")
                .notes("Some notes")
                .failureReason(null)
                .priority(1)
                .createdAt(now)
                .updatedAt(now)
                .startedAt(null)
                .completedAt(null)
                .build();
    }

    @Test
    void shouldCreateExecutionTaskSuccessfully() {
        ExecutionTask task = createDefaultTask();
        assertNotNull(task);
        assertEquals(1L, task.id());
        assertEquals(100L, task.serviceOrderId());
        assertEquals(200L, task.customerId());
        assertEquals(300L, task.vehicleId());
        assertEquals("ABC1D23", task.vehicleLicensePlate());
        assertEquals("Test task", task.description());
        assertEquals(ExecutionStatus.queued(), task.status());
        assertEquals("Technician", task.assignedTechnician());
        assertEquals("Some notes", task.notes());
        assertNull(task.failureReason());
        assertEquals(1, task.priority());
        assertEquals(now, task.createdAt());
    }

    @Test
    void shouldThrowWhenServiceOrderIdIsNull() {
        assertThrows(InvalidDataException.class, () -> ExecutionTask.builder()
                .serviceOrderId(null)
                .status(ExecutionStatus.queued())
                .createdAt(now)
                .build());
    }

    @Test
    void shouldCreateTaskWithId() {
        ExecutionTask task = createDefaultTask();
        ExecutionTask withNewId = task.withId(99L);
        assertEquals(99L, withNewId.id());
        assertEquals(task.serviceOrderId(), withNewId.serviceOrderId());
        assertEquals(task.status(), withNewId.status());
    }

    @Test
    void shouldUpdateStatusToInProgressSettingStartedAt() {
        ExecutionTask task = createDefaultTask();
        LocalDateTime updateTime = LocalDateTime.now();
        ExecutionTask updated = task.withStatusUpdated(
                ExecutionStatus.inProgress(), updateTime);

        assertEquals(ExecutionStatus.inProgress(), updated.status());
        assertEquals(updateTime, updated.startedAt());
        assertNull(updated.completedAt());
        assertEquals(updateTime, updated.updatedAt());
    }

    @Test
    void shouldUpdateStatusToCompletedSettingCompletedAt() {
        ExecutionTask task = createDefaultTask();
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        ExecutionTask inProgress = task.withStatusUpdated(
                ExecutionStatus.inProgress(), startTime);

        LocalDateTime completeTime = LocalDateTime.now();
        ExecutionTask completed = inProgress.withStatusUpdated(
                ExecutionStatus.completed(), completeTime);

        assertEquals(ExecutionStatus.completed(), completed.status());
        assertEquals(startTime, completed.startedAt());
        assertEquals(completeTime, completed.completedAt());
    }

    @Test
    void shouldUpdateStatusToFailedSettingCompletedAt() {
        ExecutionTask task = createDefaultTask();
        LocalDateTime failTime = LocalDateTime.now();
        ExecutionTask failed = task.withStatusUpdated(
                ExecutionStatus.failed(), failTime);

        assertEquals(ExecutionStatus.failed(), failed.status());
        assertEquals(failTime, failed.completedAt());
    }

    @Test
    void shouldNotOverrideExistingStartedAt() {
        LocalDateTime startTime = LocalDateTime.now().minusHours(2);
        ExecutionTask task = ExecutionTask.builder()
                .id(1L)
                .serviceOrderId(100L)
                .status(ExecutionStatus.inProgress())
                .createdAt(now)
                .startedAt(startTime)
                .build();

        LocalDateTime updateTime = LocalDateTime.now();
        ExecutionTask updated = task.withStatusUpdated(
                ExecutionStatus.inProgress(), updateTime);

        assertEquals(startTime, updated.startedAt());
    }

    @Test
    void shouldThrowWhenStatusUpdateWithNull() {
        ExecutionTask task = createDefaultTask();
        assertThrows(IllegalArgumentException.class,
                () -> task.withStatusUpdated(null, now));
    }

    @Test
    void shouldCreateWithFailure() {
        ExecutionTask task = createDefaultTask();
        LocalDateTime failTime = LocalDateTime.now();
        ExecutionTask failed = task.withFailure("Resource unavailable", failTime);

        assertEquals(ExecutionStatus.failed(), failed.status());
        assertEquals("Resource unavailable", failed.failureReason());
        assertEquals(failTime, failed.completedAt());
        assertEquals(failTime, failed.updatedAt());
    }

    @Test
    void shouldPreserveOtherFieldsOnWithId() {
        ExecutionTask task = createDefaultTask();
        ExecutionTask copy = task.withId(42L);
        assertEquals(42L, copy.id());
        assertEquals(task.serviceOrderId(), copy.serviceOrderId());
        assertEquals(task.description(), copy.description());
        assertEquals(task.status(), copy.status());
    }
}
