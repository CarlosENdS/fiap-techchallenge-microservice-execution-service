package com.techchallenge.fiap.cargarage.execution_service.infrastructure.database.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskPersistenceDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageRequestDto;

@DataJpaTest
@ActiveProfiles("test")
@Import(ExecutionTaskDataSourceImpl.class)
class ExecutionTaskDataSourceImplTest {

    @Autowired
    private ExecutionTaskDataSourceImpl dataSource;

    @Autowired
    private ExecutionTaskRepository repository;

    private ExecutionTaskPersistenceDto sampleDto;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        sampleDto = ExecutionTaskPersistenceDto.builder()
                .serviceOrderId(100L)
                .customerId(1L)
                .vehicleId(10L)
                .vehicleLicensePlate("ABC-1234")
                .description("Oil change")
                .status("QUEUED")
                .assignedTechnician("Tech A")
                .notes("Urgent")
                .priority(1)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldInsertExecutionTask() {
        ExecutionTaskDto result = dataSource.insert(sampleDto);

        assertNotNull(result.id());
        assertEquals(100L, result.serviceOrderId());
        assertEquals("QUEUED", result.status());
        assertEquals("Oil change", result.description());
        assertNotNull(result.createdAt());
    }

    @Test
    void shouldInsertWithDefaultCreatedAt() {
        ExecutionTaskPersistenceDto dto = ExecutionTaskPersistenceDto.builder()
                .serviceOrderId(200L)
                .status("QUEUED")
                .build();

        ExecutionTaskDto result = dataSource.insert(dto);

        assertNotNull(result.id());
        assertNotNull(result.createdAt());
    }

    @Test
    void shouldUpdateExecutionTask() {
        ExecutionTaskDto inserted = dataSource.insert(sampleDto);

        ExecutionTaskPersistenceDto updateDto = ExecutionTaskPersistenceDto.builder()
                .serviceOrderId(100L)
                .customerId(1L)
                .vehicleId(10L)
                .vehicleLicensePlate("ABC-1234")
                .description("Oil change + filter")
                .status("IN_PROGRESS")
                .assignedTechnician("Tech B")
                .notes("Updated notes")
                .priority(2)
                .startedAt(LocalDateTime.now())
                .build();

        ExecutionTaskDto result = dataSource.update(inserted.id(), updateDto);

        assertEquals(inserted.id(), result.id());
        assertEquals("IN_PROGRESS", result.status());
        assertEquals("Tech B", result.assignedTechnician());
        assertEquals("Oil change + filter", result.description());
        assertNotNull(result.updatedAt());
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentTask() {
        ExecutionTaskPersistenceDto dto = ExecutionTaskPersistenceDto.builder()
                .serviceOrderId(999L)
                .status("QUEUED")
                .build();

        assertThrows(RuntimeException.class, () -> dataSource.update(999L, dto));
    }

    @Test
    void shouldFindById() {
        ExecutionTaskDto inserted = dataSource.insert(sampleDto);

        Optional<ExecutionTaskDto> result = dataSource.findById(inserted.id());

        assertTrue(result.isPresent());
        assertEquals(inserted.id(), result.get().id());
        assertEquals("QUEUED", result.get().status());
    }

    @Test
    void shouldReturnEmptyForNonExistentId() {
        Optional<ExecutionTaskDto> result = dataSource.findById(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindByServiceOrderId() {
        dataSource.insert(sampleDto);

        Optional<ExecutionTaskDto> result = dataSource.findByServiceOrderId(100L);

        assertTrue(result.isPresent());
        assertEquals(100L, result.get().serviceOrderId());
    }

    @Test
    void shouldReturnEmptyForNonExistentServiceOrderId() {
        Optional<ExecutionTaskDto> result = dataSource.findByServiceOrderId(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindAllPaginated() {
        dataSource.insert(sampleDto);
        dataSource.insert(ExecutionTaskPersistenceDto.builder()
                .serviceOrderId(200L)
                .status("QUEUED")
                .createdAt(LocalDateTime.now())
                .build());
        dataSource.insert(ExecutionTaskPersistenceDto.builder()
                .serviceOrderId(300L)
                .status("IN_PROGRESS")
                .createdAt(LocalDateTime.now())
                .build());

        PageDto<ExecutionTaskDto> page = dataSource.findAll(new PageRequestDto(0, 2));

        assertEquals(2, page.content().size());
        assertEquals(3, page.totalElements());
        assertEquals(0, page.pageNumber());
        assertEquals(2, page.pageSize());
    }

    @Test
    void shouldFindByStatus() {
        dataSource.insert(sampleDto);
        dataSource.insert(ExecutionTaskPersistenceDto.builder()
                .serviceOrderId(200L)
                .status("IN_PROGRESS")
                .createdAt(LocalDateTime.now())
                .build());

        PageDto<ExecutionTaskDto> page = dataSource.findByStatus("QUEUED", new PageRequestDto(0, 10));

        assertEquals(1, page.content().size());
        assertEquals("QUEUED", page.content().get(0).status());
    }

    @Test
    void shouldReturnEmptyPageForInvalidStatus() {
        dataSource.insert(sampleDto);

        PageDto<ExecutionTaskDto> page = dataSource.findByStatus("INVALID", new PageRequestDto(0, 10));

        assertEquals(0, page.content().size());
        assertEquals(0, page.totalElements());
    }

    @Test
    void shouldDeleteById() {
        ExecutionTaskDto inserted = dataSource.insert(sampleDto);

        dataSource.deleteById(inserted.id());

        assertTrue(dataSource.findById(inserted.id()).isEmpty());
    }

    @Test
    void shouldMapAllFieldsCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        ExecutionTaskPersistenceDto fullDto = ExecutionTaskPersistenceDto.builder()
                .serviceOrderId(500L)
                .customerId(5L)
                .vehicleId(50L)
                .vehicleLicensePlate("XYZ-9999")
                .description("Full service")
                .status("COMPLETED")
                .assignedTechnician("Senior Tech")
                .notes("All done")
                .failureReason(null)
                .priority(3)
                .createdAt(now)
                .updatedAt(now)
                .startedAt(now.minusHours(2))
                .completedAt(now)
                .build();

        ExecutionTaskDto result = dataSource.insert(fullDto);

        assertEquals(500L, result.serviceOrderId());
        assertEquals(5L, result.customerId());
        assertEquals(50L, result.vehicleId());
        assertEquals("XYZ-9999", result.vehicleLicensePlate());
        assertEquals("Full service", result.description());
        assertEquals("COMPLETED", result.status());
        assertEquals("Senior Tech", result.assignedTechnician());
        assertEquals("All done", result.notes());
        assertEquals(3, result.priority());
        assertNotNull(result.startedAt());
        assertNotNull(result.completedAt());
    }
}
