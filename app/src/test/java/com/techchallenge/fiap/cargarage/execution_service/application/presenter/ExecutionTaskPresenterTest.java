package com.techchallenge.fiap.cargarage.execution_service.application.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskStatusDto;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionStatus;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionTask;

class ExecutionTaskPresenterTest {

    private final LocalDateTime now = LocalDateTime.now();

    private ExecutionTask createTask() {
        return ExecutionTask.builder()
                .id(1L)
                .serviceOrderId(100L)
                .customerId(200L)
                .vehicleId(300L)
                .vehicleLicensePlate("ABC1D23")
                .description("Test task")
                .status(ExecutionStatus.queued())
                .assignedTechnician("Tech")
                .notes("Notes")
                .failureReason(null)
                .priority(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    void shouldConvertToResponseDto() {
        ExecutionTask task = createTask();
        ExecutionTaskDto dto = ExecutionTaskPresenter.toResponseDto(task);

        assertNotNull(dto);
        assertEquals(1L, dto.id());
        assertEquals(100L, dto.serviceOrderId());
        assertEquals(200L, dto.customerId());
        assertEquals(300L, dto.vehicleId());
        assertEquals("ABC1D23", dto.vehicleLicensePlate());
        assertEquals("Test task", dto.description());
        assertEquals("QUEUED", dto.status());
        assertEquals("Tech", dto.assignedTechnician());
        assertEquals("Notes", dto.notes());
        assertNull(dto.failureReason());
        assertEquals(1, dto.priority());
    }

    @Test
    void shouldReturnNullForNullModel() {
        assertNull(ExecutionTaskPresenter.toResponseDto(null));
    }

    @Test
    void shouldConvertToStatusDto() {
        ExecutionTask task = createTask();
        ExecutionTaskStatusDto dto = ExecutionTaskPresenter.toStatusDto(task);

        assertNotNull(dto);
        assertEquals("QUEUED", dto.status());
    }

    @Test
    void shouldReturnNullStatusDtoForNullModel() {
        assertNull(ExecutionTaskPresenter.toStatusDto(null));
    }

    @Test
    void shouldHandleNullStatus() {
        ExecutionTask task = ExecutionTask.builder()
                .id(1L)
                .serviceOrderId(100L)
                .status(null)
                .createdAt(now)
                .build();

        ExecutionTaskDto dto = ExecutionTaskPresenter.toResponseDto(task);
        assertNotNull(dto);
        assertNull(dto.status());

        ExecutionTaskStatusDto statusDto = ExecutionTaskPresenter.toStatusDto(task);
        assertNotNull(statusDto);
        assertNull(statusDto.status());
    }
}
