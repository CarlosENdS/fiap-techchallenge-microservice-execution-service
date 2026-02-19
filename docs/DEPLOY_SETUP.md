# Configuração do CD Pipeline e IRSA

Este documento descreve a configuração necessária para deploy do execution-service.

## 📋 Visão Geral

| Componente | Método de Autenticação |
|------------|------------------------|
| Pipeline CD (GitHub Actions) | Credenciais estáticas (IAM User) |
| Pod (execution-service) | IRSA (token OIDC automático) |

## 🔧 Configuração Manual Necessária

### GitHub Secrets

Configure os seguintes **secrets** no repositório GitHub:
**Settings → Secrets and variables → Actions → New repository secret**

| Secret Name | Descrição | Onde Obter |
|-------------|-----------|------------|
| `AWS_ACCESS_KEY_ID` | Access Key do IAM User | Console AWS ou terraform output |
| `AWS_SECRET_ACCESS_KEY` | Secret Access Key | Console AWS ou terraform output |
| `DB_USERNAME` | Username do banco RDS | `execution_service_user` (conforme definido no Terraform) |
| `DB_PASSWORD` | Senha do banco RDS (execution_service_user) | Definida no `rds.tf` |
| `TF_API_TOKEN` | Token de API do Terraform Cloud | app.terraform.io → User Settings → Tokens |
| `SONAR_TOKEN` | Token de autenticação SonarCloud | sonarcloud.io → My Account → Security |

### Terraform Cloud

1. Crie uma conta em [app.terraform.io](https://app.terraform.io)
2. Organização: `fiap-soat-techchallenge`
3. Workspace: `fiap-techchallenge-infra-database`
4. Gere um API token e configure como `TF_API_TOKEN` no GitHub

### Variáveis de Ambiente no cd.yml

```yaml
env:
  AWS_REGION: us-east-1
  ECR_REPOSITORY: cargarage-app
  EKS_CLUSTER_NAME: cargarage-eks-prod
  TF_CLOUD_ORGANIZATION: fiap-soat-techchallenge
  TF_WORKSPACE: fiap-techchallenge-infra-database
```

## 🔄 Fluxo do Deploy

```
┌─────────────────────────────────────────────────────────────────────────┐
│  1. TERRAFORM (infra-database)                                          │
│     - Provisiona RDS (+ execution_service_db), SQS, IAM Roles           │
│     - Outputs salvos no Terraform Cloud                                 │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  2. CD PIPELINE (GitHub Actions)                                        │
│     - Autentica com credenciais estáticas (IAM User)                    │
│     - Busca outputs do Terraform Cloud via API REST (curl)              │
│     - Substitui placeholders nos manifests K8s (sed)                    │
│     - kubectl apply dos recursos                                         │
│     - Gera summary com URLs de acesso à API                             │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  3. POD (EKS)                                                           │
│     - ServiceAccount anotado com IRSA role ARN                          │
│     - Token OIDC montado automaticamente pelo EKS                       │
│     - SDK AWS assume a role e acessa SQS sem credenciais                │
└─────────────────────────────────────────────────────────────────────────┘
```

### Outputs do Terraform Cloud

| Output | Descrição |
|--------|-----------|
| `execution_service_jdbc_url` | URL JDBC para conexão com o banco |
| `execution_service_irsa_role_arn` | ARN da role IRSA para pods |
| `sqs_execution_events_queue_url` | Fila FIFO de eventos do execution-service |
| `sqs_execution_completed_queue_url` | Fila standard de execução concluída (→ OS Service) |
| `sqs_resource_unavailable_queue_url` | Fila standard de recurso indisponível (→ OS Service) |
| `sqs_billing_events_queue_url` | Fila FIFO de eventos do billing-service (consumida) |
| `sqs_os_events_queue_url` | Fila FIFO de eventos do OS Service (consumida) |

## 🗄️ Estrutura do RDS

O RDS PostgreSQL compartilhado contém três bancos de dados:

| Database | User | Aplicação |
|----------|------|-----------|
| `cargarage` | `postgres` | Monolito Car Garage |
| `os_service_db` | `os_service_user` | Microservice OS Service |
| `execution_service_db` | `execution_service_user` | Microservice Execution Service |

### Inicialização dos Bancos

A criação e população dos bancos é feita **automaticamente via Terraform** durante o `terraform apply`.

O Terraform executa 5 Kubernetes Jobs em sequência:

1. **cargarage-db-seed**: Popula o banco `cargarage` (monolito)
2. **os-service-db-init**: Cria o database `os_service_db` e user `os_service_user`
3. **os-service-db-seed**: Popula o banco `os_service_db`
4. **execution-service-db-init**: Cria o database `execution_service_db` e user `execution_service_user`
5. **execution-service-db-seed**: Popula o banco `execution_service_db`

```bash
kubectl get jobs -n db-init
kubectl logs job/execution-service-db-init -n db-init
kubectl logs job/execution-service-db-seed -n db-init
```

Os scripts SQL estão em `terraform/scripts/`:
- `init-execution-service-db.sql`
- `seed-execution-service.sql`

## 🔐 IRSA (IAM Roles for Service Accounts)

IRSA permite que pods EKS assumam IAM roles sem credenciais estáticas. O EKS injeta automaticamente um token OIDC no pod, que o AWS SDK usa para assumir a role IAM.

### Como funciona

1. **Terraform cria**: IAM Role `execution-service-irsa-role` com trust policy para o OIDC Provider do EKS
2. **ServiceAccount**: `execution-service-sa` anotado com `eks.amazonaws.com/role-arn`
3. **EKS injeta**: Token OIDC montado em `/var/run/secrets/eks.amazonaws.com/serviceaccount/token`
4. **AWS SDK**: Detecta automaticamente e assume a role

### Permissões SQS

| Ação | Filas |
|------|-------|
| `sqs:SendMessage` | `execution-service-events.fifo`, `execution-completed-queue`, `resource-unavailable-queue` |
| `sqs:ReceiveMessage` | `billing-events.fifo`, `os-order-events-queue.fifo` |

```bash
kubectl get sa execution-service-sa -n execution-service -o yaml
kubectl exec -n execution-service deployment/execution-service -- \
  ls -la /var/run/secrets/eks.amazonaws.com/serviceaccount/
```

## 📁 Placeholders nos Manifests K8s

| Arquivo | Placeholder | Valor |
|---------|-------------|-------|
| `secrets.yaml` | `__DB_URL_B64__` | URL do banco (base64) |
| `secrets.yaml` | `__DB_USERNAME_B64__` | Username (base64) |
| `secrets.yaml` | `__DB_PASSWORD_B64__` | Password (base64) |
| `secrets.yaml` | `__SQS_EXECUTION_EVENTS_QUEUE_URL_B64__` | URL da fila FIFO (base64) |
| `configmap.yaml` | `__SQS_EXECUTION_COMPLETED_QUEUE_URL__` | URL da fila de conclusão |
| `configmap.yaml` | `__SQS_RESOURCE_UNAVAILABLE_QUEUE_URL__` | URL da fila de compensação |
| `configmap.yaml` | `__SQS_BILLING_EVENTS_QUEUE__` | URL da fila de billing |
| `configmap.yaml` | `__SQS_OS_EVENTS_QUEUE__` | URL da fila de OS events |
| `service-account.yaml` | `__IRSA_ROLE_ARN__` | ARN da role IRSA |
| `app-deployment.yaml` | `__IMAGE_URI__` | URI da imagem ECR |

## 🚀 Executando o Deploy

### Automático (Push para main)

```bash
git push origin main
```

### Manual (Workflow Dispatch)

1. Vá para **Actions** no GitHub
2. Selecione **CD Pipeline**
3. Clique em **Run workflow**
4. Escolha o environment (staging/production)

## ❓ Troubleshooting

| Problema | Causa Provável | Solução |
|----------|---------------|---------|
| "Unable to locate credentials" | Pod não assume a role IRSA | Verificar anotação no ServiceAccount |
| "Access Denied" ao SQS | IAM policy sem permissões | Verificar `execution-service-sqs-policy` |
| "terraform output" falha | Token/org/workspace incorretos | Verificar `TF_API_TOKEN` e nomes |
| Pod em CrashLoopBackOff | Banco não inicializado | Verificar jobs em `db-init` namespace |
| Filas não criadas | Terraform não aplicado | Rodar `terraform apply` no infra-database |
