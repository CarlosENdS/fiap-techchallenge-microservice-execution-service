package com.techchallenge.fiap.cargarage.execution_service.application.controller;

import lombok.RequiredArgsConstructor;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskRequestDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskStatusDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskStatusUpdateDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageRequestDto;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionTask;
import com.techchallenge.fiap.cargarage.execution_service.application.presenter.ExecutionTaskPresenter;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.CreateExecutionTaskUseCase;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.FailExecutionTaskUseCase;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.FindExecutionTaskUseCase;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.UpdateExecutionTaskStatusUseCase;

/**
 * Clean Architecture controller for Execution Task operations.
 * Acts as the application layer entry point.
 */
@RequiredArgsConstructor
public class ExecutionTaskCleanArchController {

    private final FindExecutionTaskUseCase findUseCase;
    private final CreateExecutionTaskUseCase createUseCase;
    private final UpdateExecutionTaskStatusUseCase updateStatusUseCase;
    private final FailExecutionTaskUseCase failUseCase;

    public ExecutionTaskDto findById(Long id) {
        return ExecutionTaskPresenter.toResponseDto(findUseCase.findById(id));
    }

    public ExecutionTaskDto findByServiceOrderId(Long serviceOrderId) {
        return ExecutionTaskPresenter.toResponseDto(
                findUseCase.findByServiceOrderId(serviceOrderId));
    }

    public PageDto<ExecutionTaskDto> findAll(int page, int size) {
        PageRequestDto pageRequest = new PageRequestDto(page, size);
        PageDto<ExecutionTask> modelPage = findUseCase.findAll(pageRequest);
        return new PageDto<>(
                modelPage.content().stream()
                        .map(ExecutionTaskPresenter::toResponseDto).toList(),
                modelPage.totalElements(),
                modelPage.pageNumber(),
                modelPage.pageSize());
    }

    public PageDto<ExecutionTaskDto> findByStatus(String status, int page, int size) {
        PageRequestDto pageRequest = new PageRequestDto(page, size);
        PageDto<ExecutionTask> modelPage = findUseCase.findByStatus(
                status, pageRequest);
        return new PageDto<>(
                modelPage.content().stream()
                        .map(ExecutionTaskPresenter::toResponseDto).toList(),
                modelPage.totalElements(),
                modelPage.pageNumber(),
                modelPage.pageSize());
    }

    public ExecutionTaskDto create(ExecutionTaskRequestDto requestDto) {
        return ExecutionTaskPresenter.toResponseDto(createUseCase.execute(requestDto));
    }

    public ExecutionTaskDto updateStatus(
            Long id, ExecutionTaskStatusUpdateDto statusDto) {
        return ExecutionTaskPresenter.toResponseDto(
                updateStatusUseCase.execute(id, statusDto));
    }

    public ExecutionTaskStatusDto getStatus(Long id) {
        ExecutionTask task = findUseCase.findById(id);
        return ExecutionTaskPresenter.toStatusDto(task);
    }

    public ExecutionTaskDto fail(Long id, String reason) {
        return ExecutionTaskPresenter.toResponseDto(
                failUseCase.execute(id, reason));
    }
}
