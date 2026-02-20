# ADR-001: Clean Architecture

## Status

**Aceita** — 2025-06-15

## Contexto

O Execution Service é um microsserviço extraído do monolito Car Garage, responsável por gerenciar a fila de execução e acompanhamento de tarefas na oficina mecânica. Precisávamos de uma arquitetura que:

- Isolasse regras de negócio de detalhes de infraestrutura (banco, filas SQS, framework)
- Facilitasse testes unitários sem dependências externas
- Permitisse trocar componentes de infraestrutura sem afetar a lógica de domínio
- Mantivesse consistência com os demais microsserviços do projeto (OS Service, Billing Service)

## Decisão

Adotamos a **Clean Architecture** (Robert C. Martin), organizando o código em camadas com dependências apontando para o centro (regras de negócio).

### Estrutura de Camadas

```
┌─────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                      │
│  - Controllers REST (recebe requisições HTTP)                │
│  - Repositórios JPA (persistência PostgreSQL)                │
│  - Mensageria AWS SQS (listeners + publishers)               │
│  - Configuração Spring                                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
│  - Entities (ExecutionTask, ExecutionStatus)                  │
│  - Use Cases (Create, Update, Find, Fail)                    │
│  - Gateways (interfaces para infraestrutura)                 │
│  - DTOs e Presenters                                         │
│  - Exceptions de negócio                                     │
└─────────────────────────────────────────────────────────────┘
```

### Organização de Pacotes

```
com.techchallenge.fiap.cargarage.execution_service/
├── application/
│   ├── controller/      # Definições de API (interfaces)
│   ├── dto/             # Data Transfer Objects
│   ├── entity/          # Entidades de domínio (ExecutionTask, ExecutionStatus)
│   ├── enums/           # Enumerações de domínio
│   ├── exception/       # Exceções de negócio
│   ├── gateway/         # Interfaces para infraestrutura
│   ├── interfaces/      # Interfaces adicionais
│   ├── presenter/       # Formatadores de resposta
│   └── usecase/         # Casos de uso
│       ├── CreateExecutionTaskUseCase
│       ├── UpdateExecutionTaskStatusUseCase
│       ├── FindExecutionTaskUseCase
│       └── FailExecutionTaskUseCase
├── configuration/       # Configurações Spring (Beans, SQS, Swagger)
└── infrastructure/
    ├── controller/      # Controllers REST (implementações)
    ├── database/        # Repositórios JPA + entidades de persistência
    └── messaging/       # SQS Listeners e Publishers
```

### Regras de Dependência

1. **Entities** (ex: `ExecutionTask` record) — sem dependências externas
2. **Use Cases** — dependem apenas de Entities e Gateways (interfaces)
3. **Infrastructure** — implementa Gateways, depende de Spring, JPA, AWS SDK
4. **Inversão de Dependência** — Use Cases definem interfaces; Infrastructure as implementa

## Consequências

### Positivas

- ✅ **Testabilidade**: Use cases testados com mocks dos gateways (15 classes de teste)
- ✅ **Independência de Framework**: `ExecutionTask` é um `record` Java puro, sem anotações JPA
- ✅ **Substituibilidade**: Trocar PostgreSQL por outro banco requer apenas nova implementação do Gateway
- ✅ **Consistência**: Mesma estrutura dos outros microsserviços do projeto
- ✅ **Manutenibilidade**: Fronteiras claras entre camadas

### Negativas

- ❌ **Mapeamento duplicado**: Entidade de domínio (`ExecutionTask`) ≠ entidade JPA (`ExecutionTaskEntity`)
- ❌ **Mais arquivos**: Indireções gateway/implementação adicionam boilerplate
- ❌ **Curva de aprendizado**: Novos desenvolvedores precisam entender a convenção de camadas

## Alternativas Consideradas

| Alternativa | Motivo da Rejeição |
|---|---|
| MVC tradicional | Acopla lógica de negócio aos controllers |
| Hexagonal Architecture | Muito similar, mas Clean Architecture é mais conhecida pela equipe |
| DDD Layered | Menos isolamento entre domínio e infraestrutura |

## Referências

- [Clean Architecture — Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Clean Architecture Book](https://www.amazon.com/Clean-Architecture-Craftsmans-Software-Structure/dp/0134494164)
