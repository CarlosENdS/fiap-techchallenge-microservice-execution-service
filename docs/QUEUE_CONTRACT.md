# Contrato de Filas SQS — Execution Service

Contratos **reais** usados pela aplicação para publicação e consumo de mensagens SQS.

## Configuração

Chaves em `application.properties`:

| Propriedade | Descrição |
|-------------|-----------|
| `messaging.sqs.queue.execution-events-url` | Fila FIFO de saída (eventos de lifecycle) |
| `messaging.sqs.queue.execution-completed-url` | Fila standard → OS Service (conclusão) |
| `messaging.sqs.queue.resource-unavailable-url` | Fila standard → OS Service (compensação) |
| `messaging.sqs.queue.billing-events` | Fila FIFO de entrada (eventos do billing) |
| `messaging.sqs.queue.os-events` | Fila FIFO de entrada (eventos do OS Service) |

## Fila de Saída: `execution-service-events.fifo`

Publicada por `SqsExecutionEventPublisher`.

### Payload (DTO `ExecutionEventDto`)

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `eventId` | String (UUID) | Identificador único do evento |
| `eventType` | String | Tipo do evento (ExecutionStarted, ExecutionCompleted, ExecutionFailed) |
| `executionTaskId` | Long | ID da tarefa de execução |
| `serviceOrderId` | Long | ID da Ordem de Serviço |
| `customerId` | Long | ID do cliente (nullable) |
| `vehicleId` | Long | ID do veículo (nullable) |
| `vehicleLicensePlate` | String | Placa do veículo (nullable) |
| `status` | String | Status atual da tarefa (QUEUED, IN_PROGRESS, COMPLETED, FAILED) |
| `failureReason` | String | Motivo da falha (nullable, presente em ExecutionFailed) |
| `timestamp` | LocalDateTime | Data/hora do evento |

### Exemplo

```json
{
  "eventId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "eventType": "ExecutionCompleted",
  "executionTaskId": 42,
  "serviceOrderId": 1001,
  "customerId": 1001,
  "vehicleId": 2001,
  "vehicleLicensePlate": "ABC1D23",
  "status": "COMPLETED",
  "failureReason": null,
  "timestamp": "2026-02-12T15:04:05"
}
```

### FIFO Properties

- `messageGroupId`: `execution-service-events`
- `messageDeduplicationId`: `<executionTaskId>-<eventType>-<timestampMillis>`

## Fila de Saída: `execution-completed-queue` (Standard)

Publicada em conjunto com `EXECUTION_COMPLETED` no evento FIFO. Notifica o OS Service que a execução foi concluída.

### Payload

Mesmo `ExecutionEventDto` acima com `eventType = ExecutionCompleted`.

## Fila de Saída: `resource-unavailable-queue` (Standard)

Publicada em conjunto com `EXECUTION_FAILED` no evento FIFO. Notifica o OS Service para compensação (Saga).

### Payload

Mesmo `ExecutionEventDto` acima com `eventType = ExecutionFailed`.

## Filas de Entrada (Consumidas por `SqsEventListener`)

### `billing-events.fifo`

| Evento | Campo obrigatório | Ação |
|--------|-------------------|------|
| `PaymentProcessed` | `serviceOrderId` ou `orderId` | Cria ExecutionTask com status QUEUED |
| `PaymentFailed` | `serviceOrderId` ou `orderId` | Falha na task → FAILED (publica EXECUTION_FAILED) |
| `PaymentRefunded` | `serviceOrderId` ou `orderId` | Falha na task → FAILED (publica EXECUTION_FAILED) |

#### Exemplo de mensagem consumida

```json
{
  "eventType": "PaymentProcessed",
  "serviceOrderId": 1001,
  "status": "PAID",
  "timestamp": "2026-02-12T14:00:00"
}
```

### `os-order-events-queue.fifo`

| Evento | Campo obrigatório | Ação |
|--------|-------------------|------|
| `ORDER_CANCELLED` | `orderId` | Falha na task → FAILED |
| `ServiceOrderCancelled` | `orderId` ou `serviceOrderId` | Falha na task → FAILED |

#### Exemplo de mensagem consumida

```json
{
  "eventType": "ORDER_CANCELLED",
  "orderId": 1001,
  "status": "CANCELLED",
  "timestamp": "2026-02-12T16:30:00"
}
```

## Fluxo Completo (Saga)

```
OS Service                    Billing Service              Execution Service
    │                              │                              │
    │ ORDER_CREATED ──────────────►│                              │
    │                              │                              │
    │                              │ PaymentProcessed ───────────►│
    │                              │                              │ → IN_PROGRESS
    │                              │                              │ (EXECUTION_STARTED)
    │                              │                              │
    │                              │                              │ → COMPLETED
    │◄─── execution-completed ─────│                              │ (EXECUTION_COMPLETED)
    │                              │                              │
    │ ORDER_FINISHED               │                              │
    │                              │                              │
```

### Compensação (Saga Reversa)

```
Billing Service              Execution Service              OS Service
    │                              │                              │
    │ PaymentFailed ──────────────►│                              │
    │                              │ → FAILED                     │
    │                              │ (EXECUTION_FAILED)           │
    │                              │── resource-unavailable ──────►│
    │                              │                              │ → CANCELLED
```

## Dead Letter Queues

| Fila | DLQ | Max Receive Count |
|------|-----|-------------------|
| `execution-service-events.fifo` | `execution-service-events-dlq.fifo` | 3 |
| `execution-completed-queue` | `os-service-standard-dlq` | 3 |
| `resource-unavailable-queue` | `os-service-standard-dlq` | 3 |
| `billing-events.fifo` | `billing-events-dlq.fifo` | 3 |
| `os-order-events-queue.fifo` | `os-order-events-dlq.fifo` | 3 |
