package com.techchallenge.fiap.cargarage.execution_service.application.gateway;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskPersistenceDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageRequestDto;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionStatus;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionTask;
import com.techchallenge.fiap.cargarage.execution_service.application.interfaces.ExecutionTaskDataSource;

/**
 * Gateway for Execution Task operations.
 * Adapter between the application layer and the data source.
 */
@RequiredArgsConstructor
public class ExecutionTaskGateway {

    private final ExecutionTaskDataSource dataSource;

    public Optional<ExecutionTask> findById(Long id) {
        return dataSource.findById(id).map(this::toModel);
    }

    public Optional<ExecutionTask> findByServiceOrderId(Long serviceOrderId) {
        return dataSource.findByServiceOrderId(serviceOrderId).map(this::toModel);
    }

    public PageDto<ExecutionTask> findAll(PageRequestDto pageRequest) {
        PageDto<ExecutionTaskDto> dtoPage = dataSource.findAll(pageRequest);
        List<ExecutionTask> content = dtoPage.content().stream()
                .map(this::toModel).toList();
        return new PageDto<>(content, dtoPage.totalElements(),
                dtoPage.pageNumber(), dtoPage.pageSize());
    }

    public PageDto<ExecutionTask> findByStatus(
            ExecutionStatus status, PageRequestDto pageRequest) {
        PageDto<ExecutionTaskDto> dtoPage = dataSource.findByStatus(
                status.value(), pageRequest);
        List<ExecutionTask> content = dtoPage.content().stream()
                .map(this::toModel).toList();
        return new PageDto<>(content, dtoPage.totalElements(),
                dtoPage.pageNumber(), dtoPage.pageSize());
    }

    public ExecutionTask insert(ExecutionTask task) {
        ExecutionTaskPersistenceDto dto = toPersistenceDto(task);
        ExecutionTaskDto saved = dataSource.insert(dto);
        return toModel(saved);
    }

    public ExecutionTask update(ExecutionTask task) {
        ExecutionTaskPersistenceDto dto = toPersistenceDto(task);
        ExecutionTaskDto saved = dataSource.update(task.id(), dto);
        return toModel(saved);
    }

    public void deleteById(Long id) {
        dataSource.deleteById(id);
    }

    private ExecutionTaskPersistenceDto toPersistenceDto(ExecutionTask model) {
        return ExecutionTaskPersistenceDto.builder()
                .id(model.id())
                .serviceOrderId(model.serviceOrderId())
                .customerId(model.customerId())
                .vehicleId(model.vehicleId())
                .vehicleLicensePlate(model.vehicleLicensePlate())
                .description(model.description())
                .status(model.status() != null ? model.status().value() : null)
                .assignedTechnician(model.assignedTechnician())
                .notes(model.notes())
                .failureReason(model.failureReason())
                .priority(model.priority())
                .createdAt(model.createdAt())
                .updatedAt(model.updatedAt())
                .startedAt(model.startedAt())
                .completedAt(model.completedAt())
                .build();
    }

    private ExecutionTask toModel(ExecutionTaskDto dto) {
        return ExecutionTask.builder()
                .id(dto.id())
                .serviceOrderId(dto.serviceOrderId())
                .customerId(dto.customerId())
                .vehicleId(dto.vehicleId())
                .vehicleLicensePlate(dto.vehicleLicensePlate())
                .description(dto.description())
                .status(dto.status() != null ? ExecutionStatus.of(dto.status()) : null)
                .assignedTechnician(dto.assignedTechnician())
                .notes(dto.notes())
                .failureReason(dto.failureReason())
                .priority(dto.priority())
                .createdAt(dto.createdAt())
                .updatedAt(dto.updatedAt())
                .startedAt(dto.startedAt())
                .completedAt(dto.completedAt())
                .build();
    }
}
