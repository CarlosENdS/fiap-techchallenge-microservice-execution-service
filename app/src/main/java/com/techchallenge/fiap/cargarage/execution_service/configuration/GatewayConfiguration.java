package com.techchallenge.fiap.cargarage.execution_service.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.techchallenge.fiap.cargarage.execution_service.application.gateway.ExecutionTaskGateway;
import com.techchallenge.fiap.cargarage.execution_service.application.interfaces.ExecutionTaskDataSource;

/**
 * Gateway configuration for dependency injection.
 */
@Configuration
public class GatewayConfiguration {

    @Bean
    public ExecutionTaskGateway executionTaskGateway(ExecutionTaskDataSource dataSource) {
        return new ExecutionTaskGateway(dataSource);
    }
}
