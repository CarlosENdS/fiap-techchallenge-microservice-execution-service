package com.techchallenge.fiap.cargarage.execution_service.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.techchallenge.fiap.cargarage.execution_service.application.controller.ExecutionTaskCleanArchController;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.CreateExecutionTaskUseCase;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.FailExecutionTaskUseCase;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.FindExecutionTaskUseCase;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.UpdateExecutionTaskStatusUseCase;

/**
 * Clean Architecture controller configuration for dependency injection.
 */
@Configuration
public class CleanArchControllerConfiguration {

    @Bean
    public ExecutionTaskCleanArchController executionTaskCleanArchController(
            FindExecutionTaskUseCase findExecutionTaskUseCase,
            CreateExecutionTaskUseCase createExecutionTaskUseCase,
            UpdateExecutionTaskStatusUseCase updateExecutionTaskStatusUseCase,
            FailExecutionTaskUseCase failExecutionTaskUseCase) {
        return new ExecutionTaskCleanArchController(
                findExecutionTaskUseCase,
                createExecutionTaskUseCase,
                updateExecutionTaskStatusUseCase,
                failExecutionTaskUseCase);
    }
}
