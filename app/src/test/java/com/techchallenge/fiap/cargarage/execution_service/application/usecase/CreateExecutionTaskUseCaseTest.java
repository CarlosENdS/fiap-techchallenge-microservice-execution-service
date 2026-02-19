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

import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskRequestDto;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionStatus;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionTask;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.BusinessException;
import com.techchallenge.fiap.cargarage.execution_service.application.gateway.ExecutionTaskGateway;

@ExtendWith(MockitoExtension.class)
class CreateExecutionTaskUseCaseTest {

    @Mock
    private ExecutionTaskGateway gateway;

    private CreateExecutionTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateExecutionTaskUseCase(gateway);
    }

    @Test
    void shouldCreateExecutionTaskSuccessfully() {
        ExecutionTaskRequestDto request = ExecutionTaskRequestDto.builder()
                .serviceOrderId(100L)
                .customerId(200L)
                .vehicleId(300L)
                .vehicleLicensePlate("ABC1D23")
                .description("Test execution")
                .priority(1)
                .build();

        when(gateway.findByServiceOrderId(100L)).thenReturn(Optional.empty());
        when(gateway.insert(any(ExecutionTask.class))).thenAnswer(inv -> {
            ExecutionTask task = inv.getArgument(0);
            return task.withId(1L);
        });

        ExecutionTask result = useCase.execute(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(100L, result.serviceOrderId());
        assertEquals(ExecutionStatus.queued(), result.status());
        verify(gateway).insert(any(ExecutionTask.class));
    }

    @Test
    void shouldCreateTaskWhenExistingTaskIsFailed() {
        ExecutionTask failedTask = ExecutionTask.builder()
                .id(1L)
                .serviceOrderId(100L)
                .status(ExecutionStatus.failed())
                .createdAt(LocalDateTime.now())
                .build();

        ExecutionTaskRequestDto request = ExecutionTaskRequestDto.builder()
                .serviceOrderId(100L)
                .build();

        when(gateway.findByServiceOrderId(100L))
                .thenReturn(Optional.of(failedTask));
        when(gateway.insert(any(ExecutionTask.class))).thenAnswer(inv -> {
            ExecutionTask task = inv.getArgument(0);
            return task.withId(2L);
        });

        ExecutionTask result = useCase.execute(request);
        assertNotNull(result);
    }

    @Test
    void shouldThrowWhenActiveTaskExists() {
        ExecutionTask activeTask = ExecutionTask.builder()
                .id(1L)
                .serviceOrderId(100L)
                .status(ExecutionStatus.queued())
                .createdAt(LocalDateTime.now())
                .build();

        ExecutionTaskRequestDto request = ExecutionTaskRequestDto.builder()
                .serviceOrderId(100L)
                .build();

        when(gateway.findByServiceOrderId(100L))
                .thenReturn(Optional.of(activeTask));

        assertThrows(BusinessException.class, () -> useCase.execute(request));
    }

    @Test
    void shouldSetDefaultPriorityWhenNull() {
        ExecutionTaskRequestDto request = ExecutionTaskRequestDto.builder()
                .serviceOrderId(100L)
                .priority(null)
                .build();

        when(gateway.findByServiceOrderId(100L)).thenReturn(Optional.empty());
        when(gateway.insert(any(ExecutionTask.class))).thenAnswer(inv -> {
            ExecutionTask task = inv.getArgument(0);
            assertEquals(0, task.priority());
            return task.withId(1L);
        });

        useCase.execute(request);
        verify(gateway).insert(any(ExecutionTask.class));
    }
}
