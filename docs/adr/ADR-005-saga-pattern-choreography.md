# ADR-005: Saga Pattern — Coreografia

## Status

**Aceita** — 2025-06-15

## Contexto

O fluxo de uma ordem de serviço (OS) na oficina mecânica percorre 3 microsserviços:

1. **OS Service** — Cria a OS, gerencia status
2. **Billing Service** — Cria orçamento, processa pagamento
3. **Execution Service** — Gerencia task de execução na oficina

Não existe banco de dados compartilhado, portanto transações distribuídas (2PC) não são viáveis. Precisamos de um padrão para:

- Coordenar transações entre serviços
- Garantir consistência eventual
- Implementar compensação (rollback distribuído) em caso de falha
- Manter os serviços desacoplados

## Decisão

Adotamos o **Saga Pattern** na variante **Coreografada (Choreography)**, onde cada serviço:

1. Reage a eventos recebidos via filas SQS
2. Executa sua lógica local
3. Publica novos eventos para o próximo passo

**Não há orquestrador central.** Cada serviço conhece apenas os eventos que consome e produz.

### Fluxo Principal (Happy Path)

```
┌──────────────┐    ORDER_CREATED     ┌──────────────────┐
│  OS Service  │ ──────────────────►  │  Billing Service │
│              │                      │                  │
│  Cria OS     │   BudgetApproved     │  Cria Budget     │
│  Status:     │ ◄──────────────────  │  Aprova Budget   │
│  IN_EXECUTION│                      │  Registra Pgto   │
│              │                      │                  │
│              │   PaymentProcessed   │                  │
│              │ ◄──────────────      │  Processa Pgto   │
│              │                      └──────────────────┘
│              │                              │
│              │                              │ PaymentProcessed
│              │                              ▼
│              │  ExecutionCompleted  ┌───────────────────┐
│  Status:     │ ◄────────────────── │ Execution Service │
│  FINISHED    │                     │                   │
│              │                     │  Cria Task        │
└──────────────┘                     │  Executa          │
                                     │  Completa         │
                                     └───────────────────┘
```

### Fluxo de Compensação (Failure)

```
Cenário: Pagamento falha
─────────────────────────
Billing publica PaymentFailed → OS Service consome → Status = CANCELLED

Cenário: Recurso indisponível na execução
──────────────────────────────────────────
Execution publica ResourceUnavailable → OS Service consome → Status = CANCELLED

Cenário: OS cancelada externamente
──────────────────────────────────
OS publica ServiceOrderCancelled → Execution consome → Task = CANCELLED
```

### Papel do Execution Service na Saga

| Evento Consumido | Fonte | Ação |
|-----------------|-------|------|
| `PaymentProcessed` | Billing (via `billing-events.fifo`) | Cria `ExecutionTask` com status PENDING |
| `ServiceOrderCancelled` | OS (via `os-order-events-queue.fifo`) | Cancela task existente |

| Evento Publicado | Destino | Quando |
|-----------------|---------|--------|
| `ExecutionStarted` | `execution-events.fifo` | Task iniciada |
| `ExecutionCompleted` | `execution-events.fifo` + `execution-completed` (std) | Task finalizada |
| `ExecutionFailed` | `execution-events.fifo` + `resource-unavailable` (std) | Falha na execução |

### Filas SQS — Topologia Completa

```
        FIFO Queues (ordenação garantida)           Standard Queues (compensação/notificação)
┌────────────────────────────────────────┐    ┌─────────────────────────────────────┐
│  service-order-events.fifo             │    │  quote-approved                     │
│  billing-events.fifo                   │    │  execution-completed                │
│  execution-events.fifo                 │    │  payment-failed                     │
│  os-order-events-queue.fifo            │    │  resource-unavailable               │
└────────────────────────────────────────┘    └─────────────────────────────────────┘
```

## Justificativa: Coreografia vs Orquestração

| Critério | Coreografia (escolhida) | Orquestração |
|----------|:---:|:---:|
| Desacoplamento | ✅ Máximo — cada serviço é autônomo | ❌ Orquestrador conhece todos os passos |
| Ponto único de falha | ✅ Nenhum — SQS garante entrega | ❌ Orquestrador é SPOF |
| Complexidade com 3 serviços | ✅ Gerenciável — fluxo é linear | ✅ Também gerenciável |
| Visibilidade do fluxo | ❌ Distribuída — requer correlação de logs | ✅ Centralizado |
| Operação/infra | ✅ Sem componente extra | ❌ Requer Step Functions ou similar |

Para **3 microsserviços com fluxo predominantemente linear**, a coreografia é a escolha mais simples e resiliente.

## Consequências

### Positivas

- ✅ **Resiliência**: SQS garante entrega; serviço fora do ar recebe mensagens quando volta
- ✅ **Escalabilidade**: Cada serviço escala independentemente
- ✅ **Autonomia**: Cada equipe mantém seu serviço sem coordenação central
- ✅ **Simplicidade operacional**: Sem Step Functions ou processo orquestrador para manter

### Negativas

- ❌ **Rastreabilidade**: Fluxo distribuído dificulta debug — requer correlação por `serviceOrderId`
- ❌ **Complexidade crescente**: Com mais serviços, coreografia pode virar "espaguete de eventos"
- ❌ **Testes E2E**: Requer ambiente completo com todos os serviços para validar fluxo

## Alternativas Consideradas

| Alternativa | Motivo da Rejeição |
|---|---|
| Saga Orquestrada (Step Functions) | Adiciona componente central, custo operacional desproporcional para 3 serviços |
| Transação Distribuída (2PC) | Não viável com bancos heterogêneos (PostgreSQL + DynamoDB) |
| Eventual Consistency sem Saga | Sem compensação explícita, risco de dados inconsistentes |

## Referências

- [Microservices Patterns — Chris Richardson (Cap. 4: Sagas)](https://microservices.io/patterns/data/saga.html)
- [AWS Prescriptive Guidance — Saga Pattern](https://docs.aws.amazon.com/prescriptive-guidance/latest/modernization-data-persistence/saga-pattern.html)
