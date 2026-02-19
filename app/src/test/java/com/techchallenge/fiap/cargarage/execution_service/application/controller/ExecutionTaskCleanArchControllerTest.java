package com.techchallenge.fiap.cargarage.execution_service.application.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskRequestDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskStatusDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskStatusUpdateDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageDto;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionStatus;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionTask;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.CreateExecutionTaskUseCase;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.FailExecutionTaskUseCase;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.FindExecutionTaskUseCase;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.UpdateExecutionTaskStatusUseCase;

@ExtendWith(MockitoExtension.class)
class ExecutionTaskCleanArchControllerTest {

    @Mock
    private FindExecutionTaskUseCase findUseCase;
    @Mock
    private CreateExecutionTaskUseCase createUseCase;
    @Mock
    private UpdateExecutionTaskStatusUseCase updateStatusUseCase;
    @Mock
    private FailExecutionTaskUseCase failUseCase;

    private ExecutionTaskCleanArchController controller;

    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        controller = new ExecutionTaskCleanArchController(
                findUseCase, createUseCase, updateStatusUseCase, failUseCase);
    }

    private ExecutionTask createTask() {
        return ExecutionTask.builder()
                .id(1L)
                .serviceOrderId(100L)
                .customerId(200L)
                .vehicleId(300L)
                .vehicleLicensePlate("ABC1D23")
                .description("Test")
                .status(ExecutionStatus.queued())
                .priority(1)
                .createdAt(now)
                .build();
    }

    @Test
    void shouldFindById() {
        when(findUseCase.findById(1L)).thenReturn(createTask());
        ExecutionTaskDto result = controller.findById(1L);
        assertNotNull(result);
        assertEquals(1L, result.id());
    }

    @Test
    void shouldFindByServiceOrderId() {
        when(findUseCase.findByServiceOrderId(100L)).thenReturn(createTask());
        ExecutionTaskDto result = controller.findByServiceOrderId(100L);
        assertNotNull(result);
        assertEquals(100L, result.serviceOrderId());
    }

    @Test
    void shouldFindAll() {
        PageDto<ExecutionTask> modelPage = new PageDto<>(
                List.of(createTask()), 1, 0, 10);
        when(findUseCase.findAll(any())).thenReturn(modelPage);

        PageDto<ExecutionTaskDto> result = controller.findAll(0, 10);
        assertNotNull(result);
        assertEquals(1, result.content().size());
    }

    @Test
    void shouldFindByStatus() {
        PageDto<ExecutionTask> modelPage = new PageDto<>(
                List.of(createTask()), 1, 0, 10);
        when(findUseCase.findByStatus(eq("QUEUED"), any()))
                .thenReturn(modelPage);

        PageDto<ExecutionTaskDto> result = controller.findByStatus(
                "QUEUED", 0, 10);
        assertNotNull(result);
        assertEquals(1, result.content().size());
    }

    @Test
    void shouldCreate() {
        ExecutionTaskRequestDto request = ExecutionTaskRequestDto.builder()
                .serviceOrderId(100L).build();
        when(createUseCase.execute(request)).thenReturn(createTask());

        ExecutionTaskDto result = controller.create(request);
        assertNotNull(result);
        assertEquals(100L, result.serviceOrderId());
    }

    @Test
    void shouldUpdateStatus() {
        ExecutionTaskStatusUpdateDto statusDto = ExecutionTaskStatusUpdateDto.builder().status("IN_PROGRESS").build();
        ExecutionTask updated = createTask();
        when(updateStatusUseCase.execute(1L, statusDto)).thenReturn(updated);

        ExecutionTaskDto result = controller.updateStatus(1L, statusDto);
        assertNotNull(result);
    }

    @Test
    void shouldGetStatus() {
        when(findUseCase.findById(1L)).thenReturn(createTask());

        ExecutionTaskStatusDto result = controller.getStatus(1L);
        assertNotNull(result);
        assertEquals("QUEUED", result.status());
    }

    @Test
    void shouldFail() {
        ExecutionTask failed = ExecutionTask.builder()
                .id(1L)
                .serviceOrderId(100L)
                .status(ExecutionStatus.failed())
                .failureReason("reason")
                .createdAt(now)
                .build();
        when(failUseCase.execute(1L, "reason")).thenReturn(failed);

        ExecutionTaskDto result = controller.fail(1L, "reason");
        assertNotNull(result);
        assertEquals("FAILED", result.status());
        verify(failUseCase).execute(1L, "reason");
    }
}
