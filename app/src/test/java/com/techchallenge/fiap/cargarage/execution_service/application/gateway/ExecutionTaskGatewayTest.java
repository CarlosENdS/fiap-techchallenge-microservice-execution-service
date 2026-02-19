package com.techchallenge.fiap.cargarage.execution_service.application.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskPersistenceDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageRequestDto;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionStatus;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionTask;
import com.techchallenge.fiap.cargarage.execution_service.application.interfaces.ExecutionTaskDataSource;

@ExtendWith(MockitoExtension.class)
class ExecutionTaskGatewayTest {

    @Mock
    private ExecutionTaskDataSource dataSource;

    private ExecutionTaskGateway gateway;

    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        gateway = new ExecutionTaskGateway(dataSource);
    }

    private ExecutionTaskDto createDto(Long id) {
        return ExecutionTaskDto.builder()
                .id(id)
                .serviceOrderId(100L)
                .customerId(200L)
                .vehicleId(300L)
                .vehicleLicensePlate("ABC1D23")
                .description("Test")
                .status("QUEUED")
                .assignedTechnician("Tech")
                .priority(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    void shouldFindById() {
        when(dataSource.findById(1L)).thenReturn(Optional.of(createDto(1L)));

        Optional<ExecutionTask> result = gateway.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().id());
        assertEquals(ExecutionStatus.queued(), result.get().status());
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        when(dataSource.findById(999L)).thenReturn(Optional.empty());

        Optional<ExecutionTask> result = gateway.findById(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindByServiceOrderId() {
        when(dataSource.findByServiceOrderId(100L))
                .thenReturn(Optional.of(createDto(1L)));

        Optional<ExecutionTask> result = gateway.findByServiceOrderId(100L);

        assertTrue(result.isPresent());
        assertEquals(100L, result.get().serviceOrderId());
    }

    @Test
    void shouldFindAll() {
        PageRequestDto pageRequest = new PageRequestDto(0, 10);
        PageDto<ExecutionTaskDto> dtoPage = new PageDto<>(
                List.of(createDto(1L)), 1, 0, 10);
        when(dataSource.findAll(pageRequest)).thenReturn(dtoPage);

        PageDto<ExecutionTask> result = gateway.findAll(pageRequest);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(1, result.totalElements());
    }

    @Test
    void shouldFindByStatus() {
        PageRequestDto pageRequest = new PageRequestDto(0, 10);
        PageDto<ExecutionTaskDto> dtoPage = new PageDto<>(
                List.of(createDto(1L)), 1, 0, 10);
        when(dataSource.findByStatus("QUEUED", pageRequest))
                .thenReturn(dtoPage);

        PageDto<ExecutionTask> result = gateway.findByStatus(
                ExecutionStatus.queued(), pageRequest);

        assertNotNull(result);
        assertEquals(1, result.content().size());
    }

    @Test
    void shouldInsert() {
        ExecutionTask task = ExecutionTask.builder()
                .serviceOrderId(100L)
                .status(ExecutionStatus.queued())
                .createdAt(now)
                .build();

        when(dataSource.insert(any(ExecutionTaskPersistenceDto.class)))
                .thenReturn(createDto(1L));

        ExecutionTask result = gateway.insert(task);

        assertNotNull(result);
        assertEquals(1L, result.id());
        verify(dataSource).insert(any(ExecutionTaskPersistenceDto.class));
    }

    @Test
    void shouldUpdate() {
        ExecutionTask task = ExecutionTask.builder()
                .id(1L)
                .serviceOrderId(100L)
                .status(ExecutionStatus.inProgress())
                .createdAt(now)
                .build();

        ExecutionTaskDto updatedDto = ExecutionTaskDto.builder()
                .id(1L)
                .serviceOrderId(100L)
                .status("IN_PROGRESS")
                .createdAt(now)
                .build();
        when(dataSource.update(eq(1L), any(ExecutionTaskPersistenceDto.class)))
                .thenReturn(updatedDto);

        ExecutionTask result = gateway.update(task);

        assertNotNull(result);
        assertEquals(ExecutionStatus.inProgress(), result.status());
    }

    @Test
    void shouldDeleteById() {
        gateway.deleteById(1L);
        verify(dataSource).deleteById(1L);
    }
}
