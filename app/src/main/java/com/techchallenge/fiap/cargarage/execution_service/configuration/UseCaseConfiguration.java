package com.techchallenge.fiap.cargarage.execution_service.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.techchallenge.fiap.cargarage.execution_service.application.gateway.ExecutionTaskGateway;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.CreateExecutionTaskUseCase;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.FailExecutionTaskUseCase;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.FindExecutionTaskUseCase;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.UpdateExecutionTaskStatusUseCase;
import com.techchallenge.fiap.cargarage.execution_service.infrastructure.messaging.ExecutionEventPublisher;

/**
 * Use case configuration for dependency injection.
 */
@Configuration
public class UseCaseConfiguration {

    @Bean
    public FindExecutionTaskUseCase findExecutionTaskUseCase(ExecutionTaskGateway gateway) {
        return new FindExecutionTaskUseCase(gateway);
    }

    @Bean
    public CreateExecutionTaskUseCase createExecutionTaskUseCase(ExecutionTaskGateway gateway) {
        return new CreateExecutionTaskUseCase(gateway);
    }

    @Bean
    public UpdateExecutionTaskStatusUseCase updateExecutionTaskStatusUseCase(
            ExecutionTaskGateway gateway,
            ExecutionEventPublisher eventPublisher) {
        return new UpdateExecutionTaskStatusUseCase(gateway, eventPublisher);
    }

    @Bean
    public FailExecutionTaskUseCase failExecutionTaskUseCase(
            ExecutionTaskGateway gateway,
            ExecutionEventPublisher eventPublisher) {
        return new FailExecutionTaskUseCase(gateway, eventPublisher);
    }
}
