package com.techchallenge.fiap.cargarage.execution_service.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionStatus;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionTask;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.InvalidDataException;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.NotFoundException;
import com.techchallenge.fiap.cargarage.execution_service.application.gateway.ExecutionTaskGateway;
import com.techchallenge.fiap.cargarage.execution_service.infrastructure.messaging.ExecutionEventPublisher;

@ExtendWith(MockitoExtension.class)
class FailExecutionTaskUseCaseTest {

    @Mock
    private ExecutionTaskGateway gateway;

    @Mock
    private ExecutionEventPublisher eventPublisher;

    private FailExecutionTaskUseCase useCase;

    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        useCase = new FailExecutionTaskUseCase(gateway, eventPublisher);
    }

    private ExecutionTask createTask(Long id, Long serviceOrderId,
            ExecutionStatus status) {
        return ExecutionTask.builder()
                .id(id)
                .serviceOrderId(serviceOrderId)
                .status(status)
                .createdAt(now)
                .build();
    }

    @Test
    void shouldFailQueuedTask() {
        ExecutionTask task = createTask(1L, 100L, ExecutionStatus.queued());
        when(gateway.findById(1L)).thenReturn(Optional.of(task));
        when(gateway.update(any(ExecutionTask.class))).thenAnswer(
                inv -> inv.getArgument(0));

        ExecutionTask result = useCase.execute(1L, "Resource unavailable");

        assertNotNull(result);
        assertEquals(ExecutionStatus.failed(), result.status());
        assertEquals("Resource unavailable", result.failureReason());
        verify(eventPublisher).publishExecutionFailed(any());
    }

    @Test
    void shouldFailInProgressTask() {
        ExecutionTask task = createTask(1L, 100L, ExecutionStatus.inProgress());
        when(gateway.findById(1L)).thenReturn(Optional.of(task));
        when(gateway.update(any(ExecutionTask.class))).thenAnswer(
                inv -> inv.getArgument(0));

        ExecutionTask result = useCase.execute(1L, "Equipment failure");

        assertEquals(ExecutionStatus.failed(), result.status());
        assertEquals("Equipment failure", result.failureReason());
    }

    @Test
    void shouldUseDefaultReasonWhenNull() {
        ExecutionTask task = createTask(1L, 100L, ExecutionStatus.queued());
        when(gateway.findById(1L)).thenReturn(Optional.of(task));
        when(gateway.update(any(ExecutionTask.class))).thenAnswer(
                inv -> inv.getArgument(0));

        ExecutionTask result = useCase.execute(1L, null);

        assertEquals("Execution failed", result.failureReason());
    }

    @Test
    void shouldThrowWhenTaskNotFound() {
        when(gateway.findById(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> useCase.execute(999L, "reason"));
    }

    @Test
    void shouldThrowWhenTaskAlreadyCompleted() {
        ExecutionTask task = createTask(1L, 100L, ExecutionStatus.completed());
        when(gateway.findById(1L)).thenReturn(Optional.of(task));

        assertThrows(InvalidDataException.class,
                () -> useCase.execute(1L, "reason"));
    }

    @Test
    void shouldThrowWhenTaskAlreadyFailed() {
        ExecutionTask task = createTask(1L, 100L, ExecutionStatus.failed());
        when(gateway.findById(1L)).thenReturn(Optional.of(task));

        assertThrows(InvalidDataException.class,
                () -> useCase.execute(1L, "reason"));
    }

    // executeByServiceOrderId tests

    @Test
    void shouldFailTaskByServiceOrderId() {
        ExecutionTask task = createTask(1L, 100L, ExecutionStatus.queued());
        when(gateway.findByServiceOrderId(100L))
                .thenReturn(Optional.of(task));
        when(gateway.update(any(ExecutionTask.class))).thenAnswer(
                inv -> inv.getArgument(0));

        ExecutionTask result = useCase.executeByServiceOrderId(
                100L, "Payment failed");

        assertEquals(ExecutionStatus.failed(), result.status());
        assertEquals("Payment failed", result.failureReason());
        verify(eventPublisher).publishExecutionFailed(any());
    }

    @Test
    void shouldThrowWhenServiceOrderNotFound() {
        when(gateway.findByServiceOrderId(999L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> useCase.executeByServiceOrderId(999L, "reason"));
    }

    @Test
    void shouldThrowWhenServiceOrderTaskAlreadyFailed() {
        ExecutionTask task = createTask(1L, 100L, ExecutionStatus.failed());
        when(gateway.findByServiceOrderId(100L))
                .thenReturn(Optional.of(task));

        assertThrows(InvalidDataException.class,
                () -> useCase.executeByServiceOrderId(100L, "reason"));
    }

    @Test
    void shouldUseDefaultReasonByServiceOrderIdWhenNull() {
        ExecutionTask task = createTask(1L, 100L, ExecutionStatus.inProgress());
        when(gateway.findByServiceOrderId(100L))
                .thenReturn(Optional.of(task));
        when(gateway.update(any(ExecutionTask.class))).thenAnswer(
                inv -> inv.getArgument(0));

        ExecutionTask result = useCase.executeByServiceOrderId(100L, null);

        assertEquals("Execution failed", result.failureReason());
    }
}
