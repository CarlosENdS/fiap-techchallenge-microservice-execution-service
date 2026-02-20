package com.techchallenge.fiap.cargarage.execution_service.application.usecase;

import lombok.RequiredArgsConstructor;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageRequestDto;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionStatus;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionTask;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.NotFoundException;
import com.techchallenge.fiap.cargarage.execution_service.application.gateway.ExecutionTaskGateway;

/**
 * Use case for finding Execution Tasks.
 */
@RequiredArgsConstructor
public class FindExecutionTaskUseCase {

    private final ExecutionTaskGateway gateway;

    public ExecutionTask findById(Long id) {
        return gateway.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Execution task not found with id: " + id));
    }

    public ExecutionTask findByServiceOrderId(Long serviceOrderId) {
        return gateway.findByServiceOrderId(serviceOrderId)
                .orElseThrow(() -> new NotFoundException(
                        "Execution task not found for service order: " + serviceOrderId));
    }

    public PageDto<ExecutionTask> findAll(PageRequestDto pageRequest) {
        return gateway.findAll(pageRequest);
    }

    public PageDto<ExecutionTask> findByStatus(
            String status, PageRequestDto pageRequest) {
        ExecutionStatus executionStatus = ExecutionStatus.of(status);
        return gateway.findByStatus(executionStatus, pageRequest);
    }
}
