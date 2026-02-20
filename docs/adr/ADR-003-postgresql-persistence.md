# ADR-003: PostgreSQL para Persistência

## Status

**Aceita** — 2025-06-15

## Contexto

O Execution Service precisa persistir tarefas de execução (`ExecutionTask`) com:

- Consultas por status (PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED)
- Consultas por `serviceOrderId` (buscar task de uma OS específica)
- Histórico de execuções com timestamps de criação, início e conclusão
- Integridade referencial e transações ACID para atualizações de status
- Relatórios operacionais (tarefas por técnico, tempo médio de execução, etc.)

O projeto já utiliza PostgreSQL no OS Service (via AWS RDS) e DynamoDB no Billing Service, cumprindo o requisito de ter pelo menos um banco relacional e um não-relacional.

## Decisão

Adotamos **PostgreSQL** hospedado em **AWS RDS** como banco de dados do Execution Service.

### Modelo de Dados

```sql
CREATE TABLE execution_tasks (
    id                    BIGSERIAL PRIMARY KEY,
    service_order_id      BIGINT NOT NULL,
    customer_id           BIGINT,
    vehicle_id            BIGINT,
    vehicle_license_plate VARCHAR(20),
    description          TEXT,
    status               VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    assigned_technician  VARCHAR(255),
    notes                TEXT,
    failure_reason       TEXT,
    priority             INTEGER DEFAULT 0,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at           TIMESTAMP,
    completed_at         TIMESTAMP
);

-- Índices para consultas frequentes
CREATE INDEX idx_execution_tasks_service_order_id ON execution_tasks(service_order_id);
CREATE INDEX idx_execution_tasks_status ON execution_tasks(status);
```

### Mapeamento JPA

```java
@Entity
@Table(name = "execution_tasks")
public class ExecutionTaskEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_order_id", nullable = false)
    private Long serviceOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;
    // ...
}
```

### Separação Domínio vs Persistência

Seguindo a Clean Architecture (ADR-001), mantemos:
- `ExecutionTask` (record Java puro) — entidade de domínio na camada application
- `ExecutionTaskEntity` (classe JPA) — entidade de persistência na camada infrastructure
- `ExecutionTaskRepositoryImpl` — converte entre as duas representações

### Configuração

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5434/execution_service_db}
spring.jpa.hibernate.ddl-auto=none      # Migrations controladas por script SQL
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

## Consequências

### Positivas

- ✅ **Consultas relacionais**: SQL nativo para relatórios complexos (joins, aggregations)
- ✅ **Transações ACID**: Garantia de consistência na atualização de status
- ✅ **Índices eficientes**: Busca por `service_order_id` e `status` com performance
- ✅ **AWS RDS gerenciado**: Backup automático, Multi-AZ, réplicas de leitura
- ✅ **Consistência com OS Service**: Mesmo tipo de banco simplifica operação
- ✅ **Ferramentas maduras**: PgAdmin, migrations, profiling

### Negativas

- ❌ **Custo fixo**: RDS tem custo mínimo mesmo com baixo uso (vs DynamoDB on-demand)
- ❌ **Escalabilidade vertical**: Escalar leitura requer read replicas
- ❌ **Schema rígido**: Alterações no modelo requerem migrations SQL

## Alternativas Consideradas

| Alternativa | Motivo da Rejeição |
|---|---|
| DynamoDB | Já usado no Billing; usar no Execution não traria diversidade e dificulta queries por status |
| MongoDB | Adiciona complexidade operacional sem benefício claro para modelo relacional simples |
| H2 (in-memory) | Adequado apenas para testes, não para produção |

## Referências

- [AWS RDS PostgreSQL](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_PostgreSQL.html)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
