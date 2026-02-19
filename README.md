# Execution Service Microservice

Microsserviço de **gerenciamento de execução de tarefas** extraído da arquitetura Car Garage. Responsável por gerenciar a fila de execução de serviços, atualizar status de tarefas e comunicar-se com outros microsserviços (OS Service e Billing Service) via eventos SQS no padrão **Saga Coreografada**.

## 📋 Visão Geral

| Aspecto | Detalhe |
|---------|---------|
| **Arquitetura** | Clean Architecture (Application + Infrastructure) |
| **Padrão de Comunicação** | Saga Coreografada via AWS SQS |
| **Banco de Dados** | PostgreSQL 16 (RDS compartilhado) |
| **Deploy** | AWS EKS (Kubernetes) com IRSA |
| **CI/CD** | GitHub Actions |

## 🏗️ Arquitetura

```
┌──────────────────────────────────────────────────────────────────────┐
│                        INFRASTRUCTURE                                │
│  ┌────────────────┐  ┌───────────────────┐  ┌────────────────────┐  │
│  │  REST Controller│  │  SQS Publisher    │  │  SQS Listener      │  │
│  │  (Spring MVC)   │  │  (SqsClient)      │  │  (@SqsListener)    │  │
│  └───────┬────────┘  └───────┬───────────┘  └───────┬────────────┘  │
│          │                   │                       │               │
│  ┌───────┴───────────────────┴───────────────────────┴────────────┐  │
│  │                    JPA Repository (DataSourceImpl)              │  │
│  └────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────┬───────────────────────────────────┘
                                   │
┌──────────────────────────────────┴───────────────────────────────────┐
│                         APPLICATION                                   │
│  ┌────────────────┐  ┌─────────────┐  ┌──────────────┐              │
│  │  Use Cases      │  │  Gateway     │  │  Presenter   │              │
│  │  - Create       │  │  (Adapter)   │  │  (Mapper)    │              │
│  │  - Find         │  └─────────────┘  └──────────────┘              │
│  │  - UpdateStatus │                                                  │
│  │  - Fail         │  ┌─────────────┐  ┌──────────────┐              │
│  └────────────────┘  │  Entities    │  │  DTOs        │              │
│                       │  - Task      │  │  - Request   │              │
│  ┌────────────────┐  │  - Status    │  │  - Response  │              │
│  │  Interfaces     │  └─────────────┘  │  - Page      │              │
│  │  - DataSource   │                    └──────────────┘              │
│  └────────────────┘                                                   │
└──────────────────────────────────────────────────────────────────────┘
```

## 🛠️ Tecnologias

| Tecnologia | Versão | Uso |
|------------|--------|-----|
| Java | 21 | Linguagem principal |
| Spring Boot | 3.4.7 | Framework |
| Spring Data JPA | 3.4.x | Persistência |
| PostgreSQL | 16 | Banco de dados |
| AWS SQS | - | Mensageria assíncrona |
| LocalStack | 3.4 | Emulação AWS local |
| Maven | 3.9+ | Build tool |
| JUnit 5 | 5.x | Testes unitários |
| JaCoCo | 0.8.12 | Cobertura de código |
| Lombok | 1.18.x | Redução de boilerplate |
| SpringDoc OpenAPI | 2.8.9 | Documentação da API |
| Docker | 24+ | Containerização |
| Kubernetes | 1.25+ | Orquestração |

## 📁 Estrutura do Projeto

```
fiap-techchallenge-microservice-execution-service/
├── app/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/techchallenge/fiap/cargarage/execution_service/
│       │   ├── ExecutionServiceApplication.java
│       │   ├── application/
│       │   │   ├── controller/        # Clean Arch Controller
│       │   │   ├── dto/               # DTOs (Request, Response, Persistence, Page)
│       │   │   ├── entity/            # ExecutionTask, ExecutionStatus
│       │   │   ├── enums/             # ExecutionStatusEnum
│       │   │   ├── exception/         # Business, InvalidData, NotFound
│       │   │   ├── gateway/           # ExecutionTaskGateway (adapter)
│       │   │   ├── interfaces/        # ExecutionTaskDataSource
│       │   │   ├── presenter/         # ExecutionTaskPresenter
│       │   │   └── usecase/           # Create, Find, UpdateStatus, Fail
│       │   ├── configuration/         # Spring @Bean wiring
│       │   └── infrastructure/
│       │       ├── controller/        # REST Controller + GlobalExceptionHandler
│       │       ├── database/          # JPA Entity, Repository, DataSourceImpl
│       │       └── messaging/         # SQS Publisher + Listener
│       ├── main/resources/
│       │   ├── application.properties
│       │   └── application-local.properties
│       └── test/
│           ├── java/...               # Unit + Integration tests
│           └── resources/application-test.properties
├── database/init-scripts/
│   └── 00-init-database.sql
├── docker-compose.yaml
├── k8s/                               # Kubernetes manifests
├── localstack/
│   └── init-aws.sh
└── .github/workflows/
    ├── ci.yml
    └── cd.yml
```

## 📊 Modelo de Dados

### Tabela `execution_task`

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `id` | BIGSERIAL (PK) | Identificador único |
| `service_order_id` | BIGINT (NOT NULL) | ID da Ordem de Serviço |
| `customer_id` | BIGINT | ID do cliente |
| `vehicle_id` | BIGINT | ID do veículo |
| `vehicle_license_plate` | VARCHAR(20) | Placa do veículo |
| `description` | TEXT | Descrição da tarefa |
| `status` | VARCHAR(40) (NOT NULL) | Status atual |
| `assigned_technician` | VARCHAR(255) | Técnico responsável |
| `notes` | TEXT | Observações |
| `failure_reason` | VARCHAR(500) | Motivo da falha |
| `priority` | INTEGER | Prioridade (0 = padrão) |
| `created_at` | TIMESTAMP (NOT NULL) | Data de criação |
| `updated_at` | TIMESTAMP | Data de atualização |
| `started_at` | TIMESTAMP | Data de início da execução |
| `completed_at` | TIMESTAMP | Data de conclusão |

## 🔄 Workflow de Status

```
                ┌─────────┐
                │ QUEUED  │ (estado inicial)
                └────┬────┘
                     │
              ┌──────┴──────┐
              │             │
              ▼             ▼
       ┌─────────────┐ ┌────────┐
       │ IN_PROGRESS  │ │ FAILED │
       └──────┬──────┘ └────────┘
              │
       ┌──────┴──────┐
       │             │
       ▼             ▼
  ┌───────────┐ ┌────────┐
  │ COMPLETED │ │ FAILED │
  └───────────┘ └────────┘
```

### Transições Válidas

| De | Para | Descrição |
|----|------|-----------|
| `QUEUED` | `IN_PROGRESS` | Tarefa iniciada pelo técnico |
| `QUEUED` | `FAILED` | Falha antes de iniciar (compensação Saga) |
| `IN_PROGRESS` | `COMPLETED` | Tarefa concluída com sucesso |
| `IN_PROGRESS` | `FAILED` | Falha durante execução (compensação Saga) |

## 📨 Saga Pattern (Filas SQS)

### Filas de Saída (Publica)

| Fila | Tipo | Descrição |
|------|------|-----------|
| `execution-service-events.fifo` | FIFO | Eventos de lifecycle (STARTED, COMPLETED, FAILED) |
| `execution-completed-queue` | Standard | Notifica OS Service de conclusão |
| `resource-unavailable-queue` | Standard | Compensação: recurso indisponível |

### Filas de Entrada (Consome)

| Fila | Tipo | Evento | Ação |
|------|------|--------|------|
| `billing-events.fifo` | FIFO | `PaymentProcessed` | Inicia execução (QUEUED → IN_PROGRESS) |
| `billing-events.fifo` | FIFO | `PaymentFailed` | Falha na tarefa (→ FAILED) |
| `billing-events.fifo` | FIFO | `PaymentRefunded` | Falha na tarefa (→ FAILED) |
| `os-order-events-queue.fifo` | FIFO | `ORDER_CANCELLED` / `ServiceOrderCancelled` | Falha na tarefa (→ FAILED) |

## 🌐 API Endpoints

Base path: `/api/execution-service`

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/execution-tasks` | Criar tarefa de execução |
| `GET` | `/execution-tasks/{id}` | Buscar por ID |
| `GET` | `/execution-tasks/service-order/{serviceOrderId}` | Buscar por Ordem de Serviço |
| `GET` | `/execution-tasks` | Listar todas (paginado) |
| `GET` | `/execution-tasks/status/{status}` | Listar por status (paginado) |
| `PUT` | `/execution-tasks/{id}/status` | Atualizar status |
| `GET` | `/execution-tasks/{id}/status` | Consultar status atual |
| `DELETE` | `/execution-tasks/{id}` | Remover tarefa |

### Swagger UI

- **Local**: `http://localhost:8082/api/execution-service/swagger-ui/index.html`
- **EKS**: `http://<LOAD_BALANCER>/api/execution-service/swagger-ui/index.html`

### Actuator

- **Health**: `/api/execution-service/actuator/health`
- **Info**: `/api/execution-service/actuator/info`

## 🐳 Desenvolvimento Local

### Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker e Docker Compose

### Executando com Docker Compose

```bash
# Subir infraestrutura (PostgreSQL + LocalStack)
docker-compose up -d

# Compilar e executar a aplicação
cd app
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Serviços locais

| Serviço | URL | Credenciais |
|---------|-----|-------------|
| Execution Service API | `http://localhost:8082/api/execution-service` | - |
| PostgreSQL | `localhost:5434` | `execution_service_user` / `ExecutionService2024!` |
| LocalStack SQS | `http://localhost:4568` | `test` / `test` |
| Swagger UI | `http://localhost:8082/api/execution-service/swagger-ui/index.html` | - |

### Executando Testes

```bash
cd app

# Testes unitários
./mvnw test

# Testes + relatório de cobertura
./mvnw verify

# Relatório JaCoCo disponível em:
# target/site/jacoco/index.html
```

## ☸️ Kubernetes

### Manifests

| Arquivo | Descrição |
|---------|-----------|
| `namespace.yaml` | Namespace `execution-service` |
| `service-account.yaml` | ServiceAccount com IRSA |
| `configmap.yaml` | Configurações (URLs das filas) |
| `secrets.yaml` | Credenciais do banco |
| `app-deployment.yaml` | Deployment (probes, resources, anti-affinity) |
| `app-service.yaml` | Service (LoadBalancer) |
| `hpa.yaml` | HorizontalPodAutoscaler (1-2 replicas) |

### Recursos

| Recurso | Request | Limit |
|---------|---------|-------|
| CPU | 100m | 300m |
| Memória | 256Mi | 512Mi |

### Deploy Manual

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/service-account.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/app-deployment.yaml
kubectl apply -f k8s/app-service.yaml
kubectl apply -f k8s/hpa.yaml
```

## 🚀 CI/CD

### Pipeline CI (`ci.yml`)

Acionado em: `push` e `pull_request` para `main` e `develop`

| Job | Descrição |
|-----|-----------|
| `build` | Compila + testes unitários e integração |
| `code-quality` | Checkstyle (Google) + SpotBugs |
| `security-scan` | OWASP Dependency Check |
| `sonarcloud` | Análise SonarCloud + JaCoCo |
| `docker-build` | Build da imagem + Trivy scan |

### Pipeline CD (`cd.yml`)

Acionado em: `push` para `main` (após CI verde)

| Step | Descrição |
|------|-----------|
| Build & Push | Constrói imagem e envia ao ECR |
| Fetch TF Outputs | Busca outputs do Terraform Cloud |
| Substitute Placeholders | `sed` nos manifests K8s |
| Deploy | `kubectl apply` no EKS |
| Rollback | Rollback automático em caso de falha |

## 🧪 Testes

| Tipo | Framework | Cobertura Mínima |
|------|-----------|------------------|
| Unitários | JUnit 5 + Mockito | 80% |
| Integração | @DataJpaTest (H2) | - |
| Controller | @WebMvcTest + MockMvc | - |

### Classes testadas

- Entidades (ExecutionTask, ExecutionStatus, ExecutionStatusEnum)
- Use Cases (Create, Find, UpdateStatus, Fail)
- Gateway (ExecutionTaskGateway)
- Presenter (ExecutionTaskPresenter)
- Messaging (SqsExecutionEventPublisher, SqsEventListener)
- Controllers (ExecutionTaskController, ExecutionTaskCleanArchController)
- Infrastructure (ExecutionTaskDataSourceImpl, GlobalExceptionHandler)

## 📄 Documentação Adicional

- [DEPLOY_SETUP.md](docs/DEPLOY_SETUP.md) — Configuração do CD Pipeline e IRSA
- [QUEUE_CONTRACT.md](docs/QUEUE_CONTRACT.md) — Contrato das filas SQS

## 📝 Licença

Este projeto é parte do Tech Challenge FIAP — uso educacional.
