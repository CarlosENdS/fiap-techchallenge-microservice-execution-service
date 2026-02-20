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

import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskStatusUpdateDto;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionStatus;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionTask;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.InvalidDataException;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.NotFoundException;
import com.techchallenge.fiap.cargarage.execution_service.application.gateway.ExecutionTaskGateway;
import com.techchallenge.fiap.cargarage.execution_service.infrastructure.messaging.ExecutionEventPublisher;

@ExtendWith(MockitoExtension.class)
class UpdateExecutionTaskStatusUseCaseTest {

    @Mock
    private ExecutionTaskGateway gateway;

    @Mock
    private ExecutionEventPublisher eventPublisher;

    private UpdateExecutionTaskStatusUseCase useCase;

    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        useCase = new UpdateExecutionTaskStatusUseCase(gateway, eventPublisher);
    }

    private ExecutionTask createTask(ExecutionStatus status) {
        return ExecutionTask.builder()
                .id(1L)
                .serviceOrderId(100L)
                .customerId(200L)
                .vehicleId(300L)
                .status(status)
                .createdAt(now)
                .build();
    }

    @Test
    void shouldUpdateFromQueuedToInProgress() {
        ExecutionTask existing = createTask(ExecutionStatus.queued());
        when(gateway.findById(1L)).thenReturn(Optional.of(existing));
        when(gateway.update(any(ExecutionTask.class))).thenAnswer(
                inv -> inv.getArgument(0));

        ExecutionTaskStatusUpdateDto dto = ExecutionTaskStatusUpdateDto.builder()
                .status("IN_PROGRESS").build();

        ExecutionTask result = useCase.execute(1L, dto);

        assertNotNull(result);
        assertEquals(ExecutionStatus.inProgress(), result.status());
        verify(eventPublisher).publishExecutionStarted(any());
    }

    @Test
    void shouldUpdateFromInProgressToCompleted() {
        ExecutionTask existing = createTask(ExecutionStatus.inProgress());
        when(gateway.findById(1L)).thenReturn(Optional.of(existing));
        when(gateway.update(any(ExecutionTask.class))).thenAnswer(
                inv -> inv.getArgument(0));

        ExecutionTaskStatusUpdateDto dto = ExecutionTaskStatusUpdateDto.builder()
                .status("COMPLETED").build();

        ExecutionTask result = useCase.execute(1L, dto);

        assertEquals(ExecutionStatus.completed(), result.status());
        verify(eventPublisher).publishExecutionCompleted(any());
    }

    @Test
    void shouldUpdateToFailed() {
        ExecutionTask existing = createTask(ExecutionStatus.inProgress());
        when(gateway.findById(1L)).thenReturn(Optional.of(existing));
        when(gateway.update(any(ExecutionTask.class))).thenAnswer(
                inv -> inv.getArgument(0));

        ExecutionTaskStatusUpdateDto dto = ExecutionTaskStatusUpdateDto.builder()
                .status("FAILED").build();

        ExecutionTask result = useCase.execute(1L, dto);

        assertEquals(ExecutionStatus.failed(), result.status());
        verify(eventPublisher).publishExecutionFailed(any());
    }

    @Test
    void shouldThrowWhenTaskNotFound() {
        when(gateway.findById(999L)).thenReturn(Optional.empty());

        ExecutionTaskStatusUpdateDto dto = ExecutionTaskStatusUpdateDto.builder()
                .status("IN_PROGRESS").build();

        assertThrows(NotFoundException.class,
                () -> useCase.execute(999L, dto));
    }

    @Test
    void shouldThrowWhenInvalidTransition() {
        ExecutionTask existing = createTask(ExecutionStatus.completed());
        when(gateway.findById(1L)).thenReturn(Optional.of(existing));

        ExecutionTaskStatusUpdateDto dto = ExecutionTaskStatusUpdateDto.builder()
                .status("IN_PROGRESS").build();

        assertThrows(InvalidDataException.class,
                () -> useCase.execute(1L, dto));
    }

    @Test
    void shouldThrowWhenQueuedToCompleted() {
        ExecutionTask existing = createTask(ExecutionStatus.queued());
        when(gateway.findById(1L)).thenReturn(Optional.of(existing));

        ExecutionTaskStatusUpdateDto dto = ExecutionTaskStatusUpdateDto.builder()
                .status("COMPLETED").build();

        assertThrows(InvalidDataException.class,
                () -> useCase.execute(1L, dto));
    }
}
