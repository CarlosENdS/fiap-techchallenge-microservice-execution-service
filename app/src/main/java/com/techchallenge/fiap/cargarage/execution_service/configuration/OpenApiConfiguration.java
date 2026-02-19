package com.techchallenge.fiap.cargarage.execution_service.configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * OpenAPI/Swagger configuration.
 *
 * Server URL is configured via the property 'app.swagger.server-url'.
 * If not set, uses relative path so Swagger UI uses the current host.
 */
@Configuration
public class OpenApiConfiguration {

    @Value("${app.swagger.server-url:}")
    private String serverUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        Server server = new Server();

        if (serverUrl != null && !serverUrl.isBlank()) {
            server.url(serverUrl).description("API Server");
        } else {
            server.url("").description("Current Server");
        }

        return new OpenAPI()
                .info(new Info()
                        .title("Execution Service Microservice API")
                        .version("1.0.0")
                        .description("""
                                API para gerenciamento de Execuções de Ordens de Serviço do Car Garage.

                                ## Funcionalidades
                                - Gerenciamento da fila de execução (diagnósticos e reparos)
                                - Atualização de status durante a execução
                                - Comunicação de conclusão/falha via eventos SQS
                                - Participação no Saga Pattern para compensação

                                ## Workflow de Status
                                QUEUED → IN_PROGRESS → COMPLETED

                                Em caso de falha, transição para FAILED a partir de QUEUED ou IN_PROGRESS.

                                ## Eventos Consumidos
                                - PaymentProcessed (billing-events.fifo) → Cria tarefa de execução
                                - PaymentFailed/PaymentRefunded (billing-events.fifo) → Falha tarefa
                                - ORDER_CANCELLED (os-service-events.fifo) → Cancela tarefa

                                ## Eventos Publicados
                                - EXECUTION_STARTED/COMPLETED/FAILED → execution-service-events.fifo
                                - Completion notification → execution-completed-queue
                                - Failure notification → resource-unavailable-queue
                                """)
                        .contact(new Contact()
                                .name("FIAP Tech Challenge Team")
                                .email("techchallenge@fiap.com.br"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(server));
    }
}
