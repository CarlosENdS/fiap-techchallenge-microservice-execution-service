package com.techchallenge.fiap.cargarage.execution_service.infrastructure.database.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskPersistenceDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageRequestDto;
import com.techchallenge.fiap.cargarage.execution_service.application.enums.ExecutionStatusEnum;
import com.techchallenge.fiap.cargarage.execution_service.application.interfaces.ExecutionTaskDataSource;
import com.techchallenge.fiap.cargarage.execution_service.infrastructure.database.entity.ExecutionTaskEntity;

/**
 * Implementation of ExecutionTaskDataSource using JPA.
 */
@Component
@Transactional
public class ExecutionTaskDataSourceImpl implements ExecutionTaskDataSource {

    private final ExecutionTaskRepository repository;

    public ExecutionTaskDataSourceImpl(ExecutionTaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExecutionTaskDto insert(ExecutionTaskPersistenceDto dto) {
        ExecutionTaskEntity entity = toEntity(dto);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        ExecutionTaskEntity saved = repository.save(entity);
        return toDto(saved);
    }

    @Override
    public ExecutionTaskDto update(Long id, ExecutionTaskPersistenceDto dto) {
        ExecutionTaskEntity existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Execution task not found"));

        existing.setServiceOrderId(dto.serviceOrderId());
        existing.setCustomerId(dto.customerId());
        existing.setVehicleId(dto.vehicleId());
        existing.setVehicleLicensePlate(dto.vehicleLicensePlate());
        existing.setDescription(dto.description());
        existing.setStatus(dto.status());
        existing.setAssignedTechnician(dto.assignedTechnician());
        existing.setNotes(dto.notes());
        existing.setFailureReason(dto.failureReason());
        existing.setPriority(dto.priority());
        existing.setUpdatedAt(
                dto.updatedAt() != null ? dto.updatedAt() : LocalDateTime.now());
        existing.setStartedAt(dto.startedAt());
        existing.setCompletedAt(dto.completedAt());

        ExecutionTaskEntity saved = repository.save(existing);
        return toDto(saved);
    }

    @Override
    public Optional<ExecutionTaskDto> findById(Long id) {
        return repository.findById(id).map(this::toDto);
    }

    @Override
    public Optional<ExecutionTaskDto> findByServiceOrderId(Long serviceOrderId) {
        return repository.findByServiceOrderId(serviceOrderId).map(this::toDto);
    }

    @Override
    public PageDto<ExecutionTaskDto> findAll(PageRequestDto pageRequest) {
        Pageable pageable = PageRequest.of(pageRequest.page(), pageRequest.size());
        Page<ExecutionTaskEntity> page = repository.findAll(pageable);
        List<ExecutionTaskDto> dtos = page.stream().map(this::toDto).toList();
        return new PageDto<>(dtos, page.getTotalElements(),
                page.getNumber(), page.getSize());
    }

    @Override
    public PageDto<ExecutionTaskDto> findByStatus(
            String status, PageRequestDto pageRequest) {
        ExecutionStatusEnum enumVal = ExecutionStatusEnum.fromString(
                status == null ? "" : status);
        Pageable pageable = PageRequest.of(pageRequest.page(), pageRequest.size());
        if (enumVal == null) {
            return new PageDto<>(List.of(), 0,
                    pageable.getPageNumber(), pageable.getPageSize());
        }
        Page<ExecutionTaskEntity> page = repository.findByStatus(
                enumVal.name(), pageable);
        List<ExecutionTaskDto> dtos = page.stream().map(this::toDto).toList();
        return new PageDto<>(dtos, page.getTotalElements(),
                page.getNumber(), page.getSize());
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private ExecutionTaskEntity toEntity(ExecutionTaskPersistenceDto dto) {
        return ExecutionTaskEntity.builder()
                .serviceOrderId(dto.serviceOrderId())
                .customerId(dto.customerId())
                .vehicleId(dto.vehicleId())
                .vehicleLicensePlate(dto.vehicleLicensePlate())
                .description(dto.description())
                .status(dto.status())
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

    private ExecutionTaskDto toDto(ExecutionTaskEntity entity) {
        return ExecutionTaskDto.builder()
                .id(entity.getId())
                .serviceOrderId(entity.getServiceOrderId())
                .customerId(entity.getCustomerId())
                .vehicleId(entity.getVehicleId())
                .vehicleLicensePlate(entity.getVehicleLicensePlate())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .assignedTechnician(entity.getAssignedTechnician())
                .notes(entity.getNotes())
                .failureReason(entity.getFailureReason())
                .priority(entity.getPriority())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .build();
    }
}
