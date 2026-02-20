# ADR-002: AWS SQS com Spring Cloud AWS

## Status

**Aceita** — 2025-06-15

## Contexto

O Execution Service precisa de comunicação assíncrona com outros microsserviços para:

- **Consumir** eventos do Billing Service (pagamento processado/falhou) e do OS Service (ordem cancelada)
- **Publicar** eventos de início, conclusão e falha de execução para a Saga
- Garantir resiliência: mensagens não podem ser perdidas se o serviço estiver temporariamente fora do ar
- Funcionar nativamente com a infraestrutura AWS (EKS + SQS) já provisionada por Terraform

### Escolha adicional: Spring Cloud AWS vs AWS SDK puro

Os outros serviços (OS Service e Billing) utilizam o **AWS SDK SQS Client** diretamente (`SqsClient`). No Execution Service, decidimos avaliar o **Spring Cloud AWS** como abordagem alternativa para consumo de filas.

## Decisão

Adotamos **AWS SQS** como message broker e **Spring Cloud AWS** (`io.awspring.cloud`) para a camada de consumo, combinado com **AWS SDK SqsClient** para a camada de publicação.

### Abordagem Híbrida

| Operação | Tecnologia | Justificativa |
|----------|------------|---------------|
| **Consumo (Listeners)** | Spring Cloud AWS `@SqsListener` | Configuração declarativa, auto-acknowledgment, concorrência gerenciada |
| **Publicação (Publishers)** | AWS SDK `SqsClient` | Controle fino de FIFO MessageGroupId e atributos de mensagem |

### Filas Configuradas

| Fila | Tipo | Direção | Propósito |
|------|------|---------|-----------|
| `billing-events.fifo` | FIFO | Inbound | PaymentProcessed, PaymentFailed, PaymentRefunded |
| `os-order-events-queue.fifo` | FIFO | Inbound | ServiceOrderCancelled |
| `execution-events.fifo` | FIFO | Outbound | ExecutionStarted, ExecutionCompleted, ExecutionFailed |
| `execution-completed` | Standard | Outbound | Notifica OS Service conclusão (Saga) |
| `resource-unavailable` | Standard | Outbound | Compensação — recurso indisponível |

### Consumo com @SqsListener

```java
@SqsListener("${messaging.sqs.queue.billing-events}")
public void handleBillingEvent(String message) {
    JsonNode json = objectMapper.readTree(message);
    String eventType = json.get("eventType").asText();
    switch (eventType) {
        case "PaymentProcessed" -> handlePaymentProcessed(json);
        case "PaymentFailed"    -> handlePaymentFailed(json);
        case "PaymentRefunded"  -> handlePaymentRefunded(json);
    }
}
```

### Publicação com SqsClient (FIFO + Standard)

```java
// FIFO — com MessageGroupId para ordenação
sqsClient.sendMessage(SendMessageRequest.builder()
    .queueUrl(executionEventsQueueUrl)
    .messageBody(payload)
    .messageGroupId("execution-" + task.serviceOrderId())
    .messageDeduplicationId(event.getEventId())
    .build());

// Standard — para filas de compensação/notificação ao OS Service
sqsClient.sendMessage(SendMessageRequest.builder()
    .queueUrl(executionCompletedQueueUrl)
    .messageBody(payload)
    .build());
```

## Consequências

### Positivas

- ✅ **@SqsListener declarativo**: Menos boilerplate para consumidores; polling e ack automáticos
- ✅ **Controle de publicação**: SqsClient permite set preciso de `MessageGroupId` e `MessageDeduplicationId`
- ✅ **Integração AWS nativa**: IRSA no EKS, sem credenciais fixas em produção
- ✅ **FIFO + Standard**: FIFO para ordenação de eventos; Standard para notificações de Saga sem custo de deduplicação
- ✅ **Dead Letter Queue**: Mensagens que falham N vezes vão para DLQ (configurado na infra Terraform)

### Negativas

- ❌ **Abordagem híbrida**: Dois modelos (Spring Cloud + raw SDK) no mesmo serviço aumenta complexidade
- ❌ **Vendor lock-in**: Acoplamento forte com AWS SQS
- ❌ **Desenvolvimento local**: Requer LocalStack para simular SQS

## Alternativas Consideradas

| Alternativa | Motivo da Rejeição |
|---|---|
| AWS SDK puro para tudo | Mais boilerplate no consumo (polling manual, ack manual) |
| Spring Cloud AWS para tudo | `SqsTemplate` não oferece controle simples de `MessageGroupId` para FIFO |
| RabbitMQ | Requer infra adicional no EKS; equipe já usa AWS SQS nos outros serviços |
| Apache Kafka | Overkill para 3 serviços com volume moderado; custo de operação alto (MSK) |

## Referências

- [Spring Cloud AWS SQS](https://docs.awspring.io/spring-cloud-aws/docs/3.0.0/reference/html/index.html#sqs-support)
- [AWS SQS FIFO Queues](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues.html)
