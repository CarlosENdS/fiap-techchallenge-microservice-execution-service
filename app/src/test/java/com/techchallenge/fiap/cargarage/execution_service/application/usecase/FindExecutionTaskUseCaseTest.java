package com.techchallenge.fiap.cargarage.execution_service.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageRequestDto;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionStatus;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionTask;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.NotFoundException;
import com.techchallenge.fiap.cargarage.execution_service.application.gateway.ExecutionTaskGateway;

@ExtendWith(MockitoExtension.class)
class FindExecutionTaskUseCaseTest {

    @Mock
    private ExecutionTaskGateway gateway;

    private FindExecutionTaskUseCase useCase;

    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        useCase = new FindExecutionTaskUseCase(gateway);
    }

    private ExecutionTask createTask(Long id, Long serviceOrderId) {
        return ExecutionTask.builder()
                .id(id)
                .serviceOrderId(serviceOrderId)
                .status(ExecutionStatus.queued())
                .createdAt(now)
                .build();
    }

    @Test
    void shouldFindById() {
        ExecutionTask task = createTask(1L, 100L);
        when(gateway.findById(1L)).thenReturn(Optional.of(task));

        ExecutionTask result = useCase.findById(1L);
        assertNotNull(result);
        assertEquals(1L, result.id());
    }

    @Test
    void shouldThrowWhenNotFoundById() {
        when(gateway.findById(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> useCase.findById(999L));
    }

    @Test
    void shouldFindByServiceOrderId() {
        ExecutionTask task = createTask(1L, 100L);
        when(gateway.findByServiceOrderId(100L)).thenReturn(Optional.of(task));

        ExecutionTask result = useCase.findByServiceOrderId(100L);
        assertNotNull(result);
        assertEquals(100L, result.serviceOrderId());
    }

    @Test
    void shouldThrowWhenNotFoundByServiceOrderId() {
        when(gateway.findByServiceOrderId(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> useCase.findByServiceOrderId(999L));
    }

    @Test
    void shouldFindAll() {
        PageRequestDto pageRequest = new PageRequestDto(0, 10);
        ExecutionTask task = createTask(1L, 100L);
        PageDto<ExecutionTask> page = new PageDto<>(
                List.of(task), 1, 0, 10);
        when(gateway.findAll(pageRequest)).thenReturn(page);

        PageDto<ExecutionTask> result = useCase.findAll(pageRequest);
        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(1, result.totalElements());
    }

    @Test
    void shouldFindByStatus() {
        PageRequestDto pageRequest = new PageRequestDto(0, 10);
        ExecutionTask task = createTask(1L, 100L);
        PageDto<ExecutionTask> page = new PageDto<>(
                List.of(task), 1, 0, 10);
        when(gateway.findByStatus(ExecutionStatus.queued(), pageRequest))
                .thenReturn(page);

        PageDto<ExecutionTask> result = useCase.findByStatus(
                "QUEUED", pageRequest);
        assertNotNull(result);
        assertEquals(1, result.content().size());
    }
}
