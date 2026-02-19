# Fluxo Fim a Fim — Car Garage Microservices

> Documento de referência que explica o ciclo de vida completo de uma Ordem de Serviço (OS), desde a criação até o encerramento, mostrando a integração entre os 3 microsserviços via Saga Coreografada com AWS SQS.

## 📋 Índice

1. [Visão Geral dos Serviços](#1-visão-geral-dos-serviços)
2. [Mapa de Filas SQS](#2-mapa-de-filas-sqs)
3. [Fluxo Completo (Happy Path)](#3-fluxo-completo-happy-path)
4. [Fluxo de Compensação (Saga Reversa)](#4-fluxo-de-compensação-saga-reversa)
5. [Diagrama de Sequência Detalhado](#5-diagrama-de-sequência-detalhado)
6. [Passo a Passo para Simular o Fluxo Completo](#6-passo-a-passo-para-simular-o-fluxo-completo)
7. [Troubleshooting](#7-troubleshooting)

---

## 1. Visão Geral dos Serviços

| Serviço | Porta (local) | Context Path | Banco de Dados | Responsabilidade |
|---------|--------------|-------------|----------------|------------------|
| **OS Service** | 8080 | `/api/os-service` | PostgreSQL (`os_service_db`) | Gerenciar o ciclo de vida da Ordem de Serviço (CRUD, status, itens, recursos) |
| **Billing Service** | 8081 | `/api/v1` | DynamoDB (`budgets`, `payments`) | Gerar orçamentos, processar pagamentos, publicar resultados financeiros |
| **Execution Service** | 8082 | `/api/execution-service` | PostgreSQL (`execution_service_db`) | Gerenciar a fila de execução de tarefas, controlar técnicos e reportar conclusão |

### Responsabilidade de cada serviço

#### OS Service (Orquestrador da OS)
- **Dono do ciclo de vida da OS**: cria, evolui status e encerra
- **Publica** eventos em `os-order-events-queue.fifo` a cada mudança de status
- **Consome** eventos de compensação para cancelar/atualizar a OS quando algo falha em outro serviço
- **Status Machine**: RECEIVED → IN_DIAGNOSIS → WAITING_APPROVAL → IN_EXECUTION → FINISHED → DELIVERED (ou CANCELLED)

#### Billing Service (Financeiro)
- **Cria orçamento (Budget)** automaticamente ao receber `ORDER_CREATED`
- **Orçamento** fica em `PENDING_APPROVAL` até aprovação manual (admin via REST)
- **Processa pagamento** (90% sucesso / 10% falha via simulador) quando solicitado via REST
- **Publica** resultado do pagamento em `billing-events.fifo`

#### Execution Service (Execução Técnica)
- **Cria tarefa de execução** automaticamente ao receber `PaymentProcessed`
- **Gerencia status**: QUEUED → IN_PROGRESS → COMPLETED (ou FAILED)
- **Notifica OS Service** de conclusão (`execution-completed-queue`) ou falha (`resource-unavailable-queue`)

---

## 2. Mapa de Filas SQS

```
┌──────────────────┐                                    ┌──────────────────┐
│    OS SERVICE     │                                    │ BILLING SERVICE  │
│                   │                                    │                  │
│  Publica ────────►│ os-order-events-queue.fifo ───────►│                  │
│  Publica ────────►│ service-order-events (std) ───────►│ Consome (poll)   │
│                   │   (ORDER_CREATED → cria Budget)     │                  │
│                   │                                    │                  │
│  Consome ◄────────│ quote-approved-queue ◄──────────── │ Publica (std)    │
│                   │ payment-failed-queue ◄──────────── │ Publica (std)    │
│                   │                                    │                  │
│                   │ billing-events.fifo ───────────────►│ Publica (FIFO)   │
│                   │   (PaymentProcessed,               │                  │
│                   │    PaymentFailed,                   └──────────────────┘
│                   │    PaymentRefunded)                        │
│  Consome ◄────────│                                           │
│  (execution-      │                   ┌──────────────────┐    │
│   completed-queue)│                   │EXECUTION SERVICE │    │
│  (resource-       │                   │                  │    │
│   unavailable-    │                   │  Consome ◄───────┘    │
│   queue)          │                   │  (billing-events.fifo)│
│                   │                   │                       │
│  Consome ◄────────│ execution-        │  Publica              │
│                   │  completed-queue ◄│                       │
│                   │                   │                       │
│  Consome ◄────────│ resource-         │  Consome ◄──── os-order-events-queue.fifo
│                   │  unavailable-queue│  (ORDER_CANCELLED)    │
└──────────────────┘                    └──────────────────┘
                     execution-service-events.fifo
                       (tracking/auditoria)
```

### Tabela de Filas

| Fila | Tipo | Produz | Consome | Propósito |
|------|------|--------|---------|-----------|
| `os-order-events-queue.fifo` | FIFO | OS Service | Execution Service | Eventos de ciclo de vida da OS (ORDER_CANCELLED) |
| `service-order-events` | Standard | OS Service | Billing Service | ORDER_CREATED → Billing cria Budget |
| `billing-events.fifo` | FIFO | Billing Service | Execution Service | Resultado de pagamento (PaymentProcessed/Failed/Refunded) |
| `execution-service-events.fifo` | FIFO | Execution Service | (Tracking/Auditoria) | Eventos de execução (Started/Completed/Failed) |
| `quote-approved-queue` | Standard | Billing Service | OS Service | BudgetApproved → OS transiciona para IN_EXECUTION |
| `execution-completed-queue` | Standard | Execution Service | OS Service | Notificação de execução concluída → FINISHED |
| `payment-failed-queue` | Standard | Billing Service | OS Service | Compensação: pagamento falhou → CANCELLED |
| `resource-unavailable-queue` | Standard | Execution Service | OS Service | Compensação: recurso indisponível → CANCELLED |

> \* Todas as filas standard são populadas diretamente pelo serviço produtor (sem SNS fan-out). Cada produtor publica explicitamente nas filas que cada consumidor espera.

---

## 3. Fluxo Completo (Happy Path)

### Fase 1 — Criação da OS (OS Service)

```
[Cliente/Admin] ──POST /service-orders──► [OS Service]
                                              │
                                              ▼
                                        Status: RECEIVED
                                        Publica: ORDER_CREATED
                                              │
                                              ▼ (auto-advance: quote completa)
                                        Status: IN_DIAGNOSIS
                                              │
                                              ▼
                                        Status: WAITING_APPROVAL
                                        Publica: ORDER_WAITING_APPROVAL
```

**O que acontece**: A OS é criada com itens (serviços) e recursos (peças). Quando a OS já vem com uma cotação completa (serviços e recursos com preços > 0), o `CreateServiceOrderUseCase` auto-avança o status de `RECEIVED → IN_DIAGNOSIS → WAITING_APPROVAL` automaticamente. Isso permite que o fluxo da Saga prossiga sem intervenção manual.

> **Nota**: Se a OS for criada sem cotação (sem preços), ela permanece em `RECEIVED` e os passos manuais `IN_DIAGNOSIS → WAITING_APPROVAL` devem ser executados via REST.

### Fase 2 — Orçamento e Aprovação (Billing Service)

```
[Billing Service] ◄── consome ORDER_CREATED ── [service-order-events (standard)]
        │
        ▼
  Cria Budget (PENDING_APPROVAL) com dados do pedido
        │
        ▼
[Admin] ──PUT /budgets/{id}/approve──► Budget: APPROVED
                                        Publica: BudgetApprovedEvent
                                            para billing-events.fifo
                                            + quote-approved-queue (OS)
```

**O que acontece**: O Billing Service escuta a fila `service-order-events` (standard, publicada pelo OS Service) e automaticamente gera um orçamento (Budget). O admin revisa e aprova. Ao aprovar, o `BudgetApprovedEvent` é publicado na fila FIFO (para audit/Execution) e também na `quote-approved-queue` (para o OS Service transicionar para IN_EXECUTION).

### Fase 3 — Pagamento (Billing Service)

```
[Cliente/Admin] ──POST /payments──► [Billing Service]
        │                               │
        ▼                               ▼
  Cria Payment (PROCESSING)       PaymentGatewaySimulator
        │                          (processa a cada 10s)
        │                               │
        ▼                        ┌──────┴──────┐
                              SUCCESS (90%)   FAILURE (10%)
                                 │               │
                                 ▼               ▼
                            Status: PAID    Status: FAILED
                            Publica:        Publica:
                            PaymentProcessed PaymentFailed
                            (billing-events  (billing-events
                             .fifo)           .fifo)

> **Nota**: O status final de pagamento bem-sucedido é `PAID` (não `PROCESSED`).
```

**O que acontece**: O pagamento é registrado e o simulador de gateway processa a cada 10 segundos. Com 90% de chance de sucesso, publica `PaymentProcessed` na fila FIFO (para Execution). Com 10% de falha, publica `PaymentFailed` na fila FIFO (para Execution) **e** na `payment-failed-queue` (para OS Service cancelar a OS via Saga).

### Fase 4 — Execução (Execution Service)

```
[Execution Service] ◄── consome PaymentProcessed ── [billing-events.fifo]
        │
        ▼
  Cria ExecutionTask (QUEUED)
        │
        ▼
[Técnico/Admin] ──PUT /execution-tasks/{id}/status──► Status: IN_PROGRESS
                                                        Publica: ExecutionStarted
        │
        ▼
[Técnico/Admin] ──PUT /execution-tasks/{id}/status──► Status: COMPLETED
                                                        Publica: ExecutionCompleted
                                                        + envia para execution-completed-queue
```

**O que acontece**: Ao receber a confirmação de pagamento, o Execution Service cria uma tarefa de execução em fila (QUEUED). O técnico inicia o trabalho (IN_PROGRESS) e ao finalizar marca como COMPLETED. Isso notifica o OS Service.

### Fase 5 — Encerramento (OS Service)

```
[OS Service] ◄── consome execution-completed-queue ── [Execution Service]
        │
        ▼
  Status: FINISHED (Publica: ORDER_FINISHED)
        │
        ▼
[Admin] ──PUT /service-orders/{id}/status──► Status: DELIVERED
                                              Publica: ORDER_DELIVERED
```

**O que acontece**: O OS Service recebe a notificação de conclusão e automaticamente marca a OS como FINISHED. O admin então entrega o veículo ao cliente e marca como DELIVERED.

---

## 4. Fluxo de Compensação (Saga Reversa)

### Cenário A: Pagamento Falha

```
[Billing Service] ──PaymentFailed──► [billing-events.fifo]
                                           │
                                           ▼
                                    [Execution Service]
                                    (cancela tarefa se existir)
                                    Publica ExecutionFailed
                                    + envia para resource-unavailable-queue
                                           │
                                           ▼
                                    [OS Service]
                                    (consome payment-failed-queue ou
                                     resource-unavailable-queue)
                                    Status: CANCELLED
```

### Cenário B: Recurso Indisponível na Execução

```
[Técnico] ──marca falha via REST──► [Execution Service]
                                    ExecutionTask: FAILED
                                    Publica ExecutionFailed
                                    + envia para resource-unavailable-queue
                                           │
                                           ▼
                                    [OS Service]
                                    consome resource-unavailable-queue
                                    Status: CANCELLED
```

### Cenário C: OS é Cancelada durante Execução

```
[Admin] ──cancela OS via REST──► [OS Service]
                                  Status: CANCELLED
                                  Publica: ORDER_CANCELLED
                                       │
                                       ▼
                                [os-order-events-queue.fifo]
                                       │
                                       ▼
                               [Execution Service]
                               (consome ORDER_CANCELLED)
                               ExecutionTask: FAILED
```

> **Nota**: O `canTransitionTo` do OS Service **não permite** cancelar uma OS em status `IN_EXECUTION` diretamente. Cancelamentos só são possíveis nos status RECEIVED, IN_DIAGNOSIS e WAITING_APPROVAL. Para compensação pós-execução, o fluxo usa `resource-unavailable-queue`.

---

## 5. Diagrama de Sequência Detalhado

```
 Cliente/Admin          OS Service           Billing Service       Execution Service
      │                     │                      │                      │
      │──POST /service-     │                      │                      │
      │  orders────────────►│                      │                      │
      │                     │─RECEIVED             │                      │
      │                     │  (auto-advance)      │                      │
      │                     │─IN_DIAGNOSIS         │                      │
      │                     │─WAITING_APPROVAL     │                      │
      │                     │──ORDER_CREATED──────►│                      │
      │                     │  + ORDER_WAITING_    │                      │
      │                     │  APPROVAL───────────►│                      │
      │                     │                      │─Cria Budget          │
      │                     │                      │ (PENDING_APPROVAL)   │
      │                     │                      │                      │
      │                     │                      │                      │
      │──PUT /budgets/      │                      │                      │
      │  {id}/approve──────────────────────────────►│                      │
      │                     │                      │─Budget: APPROVED     │
      │                     │                      │──BudgetApproved─────►│
      │                     │                      │  (billing-events)    │
      │                     │                      │                      │
      │──POST /payments────────────────────────────►│                      │
      │                     │                      │─Payment: PROCESSING  │
      │                     │                      │                      │
      │                     │                      │(10s poll: Gateway    │
      │                     │                      │ simula sucesso)      │
      │                     │                      │─Payment: PAID        │
      │                     │                      │──PaymentProcessed───►│
      │                     │                      │  (billing-events)    │
      │                     │                      │                      │
      │                     │                      │              ┌───────┤
      │                     │                      │              │ Cria  │
      │                     │                      │              │ Task  │
      │                     │                      │              │QUEUED │
      │                     │                      │              └───────┤
      │                     │                      │                      │
      │──PUT /execution-    │                      │                      │
      │  tasks/{id}/status: │                      │                      │
      │  IN_PROGRESS───────────────────────────────────────────────────────►│
      │                     │                      │              ┌───────┤
      │                     │                      │              │IN_    │
      │                     │                      │              │PROGRESS│
      │                     │                      │              └───────┤
      │                     │                      │                      │
      │──PUT /execution-    │                      │                      │
      │  tasks/{id}/status: │                      │                      │
      │  COMPLETED─────────────────────────────────────────────────────────►│
      │                     │                      │              ┌───────┤
      │                     │                      │              │COMPLET│
      │                     │                      │              │ED     │
      │                     │◄─execution-          │              └───────┤
      │                     │  completed-queue─────────────────────────────┤
      │                     │                      │                      │
      │                     │─FINISHED             │                      │
      │                     │──ORDER_FINISHED─────►│                      │
      │                     │                      │                      │
      │──PUT status:        │                      │                      │
      │  DELIVERED─────────►│                      │                      │
      │                     │─DELIVERED            │                      │
      │                     │──ORDER_DELIVERED────►│                      │
      │                     │                      │                      │
      ▼                     ▼                      ▼                      ▼
```

---

## 6. Passo a Passo para Simular o Fluxo Completo

### Pré-requisitos

- Docker e Docker Compose instalados
- Acesso aos repositórios dos 3 serviços
- `curl` ou Postman para chamadas REST

### 6.1. Subir a infraestrutura

Use o `docker-compose.e2e.yaml` (externo) que sobe toda a infra compartilhada:

```bash
cd fiap-techchallenge-microservice-execution-service
docker-compose -f docker-compose.e2e.yaml up -d
```

Aguarde todos os serviços ficarem saudáveis:

```bash
docker-compose -f docker-compose.e2e.yaml ps
```

### 6.2. Verificar saúde dos serviços

```bash
# OS Service
curl http://localhost:8080/api/os-service/actuator/health

# Billing Service
curl http://localhost:8081/api/v1/actuator/health

# Execution Service
curl http://localhost:8082/api/execution-service/actuator/health
```

### 6.3. Passo 1 — Criar uma Ordem de Serviço

```bash
curl -X POST http://localhost:8080/api/os-service/service-orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1001,
    "customerName": "Carlos Santos",
    "vehicleId": 2001,
    "vehicleLicensePlate": "ABC1D23",
    "vehicleModel": "Civic",
    "vehicleBrand": "Honda",
    "description": "Troca de pastilhas de freio e revisão geral",
    "services": [
      {
        "serviceId": 301,
        "serviceName": "Troca de pastilhas",
        "serviceDescription": "Substituição das pastilhas dianteiras",
        "quantity": 1,
        "price": 220.00
      }
    ],
    "resources": [
      {
        "resourceId": 401,
        "resourceName": "Pastilha de freio dianteira",
        "resourceDescription": "Jogo com 4 pastilhas",
        "resourceType": "PART",
        "quantity": 1,
        "price": 320.00
      }
    ]
  }'
```

> **Resultado**: OS criada com `status: WAITING_APPROVAL`, `id: 1`. Evento `ORDER_CREATED` e `ORDER_WAITING_APPROVAL` publicados.
> A OS auto-avançou de `RECEIVED → IN_DIAGNOSIS → WAITING_APPROVAL` porque veio com cotação completa (serviços e recursos com preços).
> Anote o `id` retornado (ex: `1`).

### 6.4. Passo 2 — Verificar se o Budget foi criado no Billing Service

Aguarde ~5 segundos (polling do consumer) e verifique:

```bash
curl http://localhost:8081/api/v1/budgets/service-order/1
```

> Deve retornar um budget com `serviceOrderId: "1"` e `status: PENDING_APPROVAL`.
> Anote o `budgetId` retornado.

### 6.5. Passo 3 — Aprovar o Orçamento

```bash
curl -X PUT http://localhost:8081/api/v1/budgets/{budgetId}/approve
```

> Publica `BudgetApprovedEvent` na fila `billing-events.fifo` e `quote-approved-queue`.
> O OS Service consome `quote-approved-queue` e transiciona a OS para `IN_EXECUTION`.

### 6.6. Passo 4 — Registrar Pagamento

```bash
curl -X POST http://localhost:8081/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "budgetId": "{budgetId}",
    "method": "PIX"
  }'
```

> Pagamento criado com status `PROCESSING`.
> O `PaymentProcessingOrchestrator` processa a cada 10 segundos.

### 6.7. Passo 5 — Aguardar processamento do pagamento

Aguarde ~15 segundos e verifique:

```bash
curl http://localhost:8081/api/v1/payments/{paymentId}
```

> Se `status: PAID` → `PaymentProcessedEvent` foi publicado para `billing-events.fifo`.
> Se `status: FAILED` → `PaymentFailedEvent` foi publicado. Crie nova OS e repita.

### 6.8. Passo 6 — Verificar ExecutionTask criada

Aguarde ~5 segundos após o pagamento ser processado:

```bash
curl http://localhost:8082/api/execution-service/execution-tasks/service-order/1
```

> Deve retornar uma ExecutionTask com `status: QUEUED` e `serviceOrderId: 1`.
> Anote o `id` da task.

### 6.9. Passo 7 — Técnico Inicia a Execução

```bash
curl -X PUT http://localhost:8082/api/execution-service/execution-tasks/{taskId}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}'
```

> Publica `ExecutionStarted` na fila FIFO de eventos.

### 6.10. Passo 8 — Técnico Conclui a Execução

```bash
curl -X PUT http://localhost:8082/api/execution-service/execution-tasks/{taskId}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "COMPLETED"}'
```

> Publica `ExecutionCompleted` + envia para `execution-completed-queue`.

### 6.11. Passo 9 — Verificar OS finalizada

Aguarde ~5 segundos:

```bash
curl http://localhost:8080/api/os-service/service-orders/1
```

> Status deve ser `FINISHED` (transição automática via evento).

### 6.12. Passo 10 — Entregar Veículo

```bash
curl -X PUT http://localhost:8080/api/os-service/service-orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "DELIVERED"}'
```

> Status: `DELIVERED`. **Fluxo completo encerrado!** 🎉

### 6.13. (Opcional) Simular Compensação

Para testar a Saga Reversa, crie uma nova OS, Budget e faça o pagamento falhar:

```bash
# Crie nova OS (repita passos 6.3–6.4)
# Aprove o Budget (passo 6.5)
# Registre pagamento repetidamente até falhar (10% de chance):
curl -X POST http://localhost:8081/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{"budgetId": "{newBudgetId}", "method": "PIX"}'

# Quando o pagamento falhar, verifique:
# - Execution Service: tarefa cancelada ou não criada
# - OS Service: status CANCELLED
```

---

## 7. Troubleshooting

| Sintoma | Causa | Solução |
|---------|-------|---------|
| Budget não criado no Billing | OS Service não publicou `ORDER_CREATED` ou Billing não está consumindo | Verificar logs do Billing: `docker logs billing-service` |
| Pagamento fica em PROCESSING | Orchestrator não está rodando | Verificar se o `@Scheduled` está ativo; checar logs |
| ExecutionTask não criada | `PaymentProcessed` não chegou na fila `billing-events.fifo` | Verificar filas no LocalStack: `awslocal sqs list-queues` |
| OS não transita para FINISHED | `execution-completed-queue` sem mensagem | Verificar logs do Execution Service |
| Compensação não funciona | Fila errada ou listener não configurado | Checar propriedades de fila nos `application.properties` |

### Comandos úteis para debug com LocalStack

```bash
# Listar filas
docker exec e2e-localstack awslocal sqs list-queues

# Ver mensagens pendentes em uma fila
docker exec e2e-localstack awslocal sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/billing-events.fifo \
  --attribute-names ApproximateNumberOfMessages

# Peek em uma fila (receive sem deletar — visibility timeout protege)
docker exec e2e-localstack awslocal sqs receive-message \
  --queue-url http://localhost:4566/000000000000/billing-events.fifo
```

