package com.techchallenge.fiap.cargarage.execution_service.application.interfaces;

import java.util.Optional;

import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskPersistenceDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageRequestDto;

/**
 * Interface for Execution Task data source operations.
 * Abstracts the data persistence layer.
 */
public interface ExecutionTaskDataSource {

    ExecutionTaskDto insert(ExecutionTaskPersistenceDto dto);

    ExecutionTaskDto update(Long id, ExecutionTaskPersistenceDto dto);

    Optional<ExecutionTaskDto> findById(Long id);

    Optional<ExecutionTaskDto> findByServiceOrderId(Long serviceOrderId);

    PageDto<ExecutionTaskDto> findAll(PageRequestDto pageRequest);

    PageDto<ExecutionTaskDto> findByStatus(String status, PageRequestDto pageRequest);

    void deleteById(Long id);
}
