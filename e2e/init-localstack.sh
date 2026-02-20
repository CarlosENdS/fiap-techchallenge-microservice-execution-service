#!/bin/bash
###############################################################################
# init-localstack.sh — Script de inicialização do LocalStack para E2E
#
# Cria TODAS as filas SQS e tabelas DynamoDB necessárias para rodar
# os 3 microserviços (OS Service, Billing Service, Execution Service)
# em um único container LocalStack compartilhado.
###############################################################################
set -e

echo "=============================================="
echo " Car Garage — E2E LocalStack Initialization"
echo "=============================================="
sleep 3

# ─────────────────────────────────────────────────────────────────────────────
# FILAS SQS — FIFO (ordenadas, deduplicadas)
# ─────────────────────────────────────────────────────────────────────────────

echo ""
echo ">>> Creating FIFO SQS queues..."

# OS Service — Eventos de ciclo de vida da OS
awslocal sqs create-queue \
  --queue-name os-order-events-queue.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=false
echo "  ✓ os-order-events-queue.fifo"

# Billing Service — Eventos de billing (aprovação, pagamento)
awslocal sqs create-queue \
  --queue-name billing-events.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=false
echo "  ✓ billing-events.fifo"

# Execution Service — Eventos de execução (tracking/auditoria)
awslocal sqs create-queue \
  --queue-name execution-service-events.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=false
echo "  ✓ execution-service-events.fifo"

# DLQs (Dead Letter Queues) — FIFO
awslocal sqs create-queue \
  --queue-name execution-service-events-dlq.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=false
echo "  ✓ execution-service-events-dlq.fifo (DLQ)"

# ─────────────────────────────────────────────────────────────────────────────
# FILAS SQS — Standard (compensação Saga / notificação)
# ─────────────────────────────────────────────────────────────────────────────

echo ""
echo ">>> Creating Standard SQS queues..."

# Billing → OS: Orçamento aprovado
awslocal sqs create-queue --queue-name quote-approved-queue
echo "  ✓ quote-approved-queue"

# Execution → OS: Execução concluída
awslocal sqs create-queue --queue-name execution-completed-queue
echo "  ✓ execution-completed-queue"

# Billing → OS: Pagamento falhou (compensação)
awslocal sqs create-queue --queue-name payment-failed-queue
echo "  ✓ payment-failed-queue"

# Execution → OS: Recurso indisponível (compensação)
awslocal sqs create-queue --queue-name resource-unavailable-queue
echo "  ✓ resource-unavailable-queue"

# Billing Service — Fila de eventos de ordem de serviço (consumida por polling)
# Nota: O Billing consome de "service-order-events" (nome sem .fifo no profile local)
awslocal sqs create-queue --queue-name service-order-events
echo "  ✓ service-order-events (billing consumer)"

# ─────────────────────────────────────────────────────────────────────────────
# DYNAMO DB — Tabelas do Billing Service
# ─────────────────────────────────────────────────────────────────────────────

echo ""
echo ">>> Creating DynamoDB tables..."

# Tabela de Budgets (Orçamentos)
awslocal dynamodb create-table \
  --table-name billing-service-budgets \
  --attribute-definitions \
    AttributeName=budgetId,AttributeType=S \
    AttributeName=serviceOrderId,AttributeType=S \
  --key-schema \
    AttributeName=budgetId,KeyType=HASH \
  --global-secondary-indexes \
    'IndexName=ServiceOrderIndex,KeySchema=[{AttributeName=serviceOrderId,KeyType=HASH}],Projection={ProjectionType=ALL}' \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
echo "  ✓ billing-service-budgets (+ GSI ServiceOrderIndex)"

# Tabela de Payments (Pagamentos)
awslocal dynamodb create-table \
  --table-name billing-service-payments \
  --attribute-definitions \
    AttributeName=paymentId,AttributeType=S \
    AttributeName=budgetId,AttributeType=S \
    AttributeName=serviceOrderId,AttributeType=S \
  --key-schema \
    AttributeName=paymentId,KeyType=HASH \
  --global-secondary-indexes \
    'IndexName=BudgetIndex,KeySchema=[{AttributeName=budgetId,KeyType=HASH}],Projection={ProjectionType=ALL}' \
    'IndexName=ServiceOrderIndex,KeySchema=[{AttributeName=serviceOrderId,KeyType=HASH}],Projection={ProjectionType=ALL}' \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
echo "  ✓ billing-service-payments (+ GSI BudgetIndex, ServiceOrderIndex)"

# ─────────────────────────────────────────────────────────────────────────────
# VERIFICAÇÃO
# ─────────────────────────────────────────────────────────────────────────────

echo ""
echo "=============================================="
echo " Summary"
echo "=============================================="
echo ""
echo "SQS Queues:"
awslocal sqs list-queues --output text | tr '\t' '\n' | grep -v "^QUEUEURLS$" | sed 's|.*/||' | sort | sed 's/^/  - /'

echo ""
echo "DynamoDB Tables:"
awslocal dynamodb list-tables --output text | tr '\t' '\n' | grep -v "^TABLENAMES$" | sort | sed 's/^/  - /'

echo ""
echo "=============================================="
echo " LocalStack E2E initialization complete!"
echo "=============================================="
