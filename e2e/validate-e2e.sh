#!/bin/bash
###############################################################################
# validate-e2e.sh — Script de validação automatizada do fluxo fim a fim
#
# Executa o fluxo completo da Saga (OS → Billing → Execution) e valida cada
# etapa, reportando status de sucesso/falha.
#
# Pré-requisitos:
#   - docker-compose.e2e.yaml rodando (todos os serviços healthy)
#   - curl e jq instalados
#
# Uso:
#   chmod +x e2e/validate-e2e.sh
#   ./e2e/validate-e2e.sh
#
#   # Ou com base URLs customizados (para AWS/EKS):
#   OS_BASE_URL=http://os-svc:8080 \
#   BILLING_BASE_URL=http://billing-svc:8080 \
#   EXECUTION_BASE_URL=http://execution-svc:8080 \
#   ./e2e/validate-e2e.sh
###############################################################################
set -uo pipefail

# ═══════════════════════════════════════════════════════════════════════════════
# CONFIGURAÇÃO
# ═══════════════════════════════════════════════════════════════════════════════

OS_BASE_URL="${OS_BASE_URL:-http://localhost:8080}"
BILLING_BASE_URL="${BILLING_BASE_URL:-http://localhost:8081}"
EXECUTION_BASE_URL="${EXECUTION_BASE_URL:-http://localhost:8082}"

OS_API="${OS_BASE_URL}/api/os-service"
BILLING_API="${BILLING_BASE_URL}/api/v1"
EXECUTION_API="${EXECUTION_BASE_URL}/api/execution-service"

# Tempos de espera (segundos) — ajuste para ambientes mais lentos
WAIT_BUDGET_CREATION=60      # Tempo para Billing criar Budget a partir do evento
WAIT_PAYMENT_PROCESSING=60   # Tempo para gateway processar pagamento
WAIT_EXECUTION_TASK=60       # Tempo para Execution criar task a partir do evento
POLL_INTERVAL=5              # Intervalo entre polls
MAX_POLL_ATTEMPTS=20         # Máximo de tentativas de polling

# ═══════════════════════════════════════════════════════════════════════════════
# CONTADORES E FORMATAÇÃO
# ═══════════════════════════════════════════════════════════════════════════════

TOTAL=0
PASSED=0
FAILED=0
WARNINGS=0

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

print_header() {
  echo ""
  echo -e "${BOLD}${CYAN}═══════════════════════════════════════════════════════════════${NC}"
  echo -e "${BOLD}${CYAN}  $1${NC}"
  echo -e "${BOLD}${CYAN}═══════════════════════════════════════════════════════════════${NC}"
}

print_step() {
  echo ""
  echo -e "${BOLD}  ▶ $1${NC}"
}

check() {
  local description="$1"
  local expected="$2"
  local actual="$3"
  TOTAL=$((TOTAL + 1))

  if [ "$actual" = "$expected" ]; then
    PASSED=$((PASSED + 1))
    echo -e "    ${GREEN}✔ PASS${NC} | $description"
    echo -e "           Esperado: ${expected}  |  Obtido: ${actual}"
  else
    FAILED=$((FAILED + 1))
    echo -e "    ${RED}✘ FAIL${NC} | $description"
    echo -e "           Esperado: ${expected}  |  Obtido: ${actual}"
  fi
}

check_not_empty() {
  local description="$1"
  local actual="$2"
  TOTAL=$((TOTAL + 1))

  if [ -n "$actual" ] && [ "$actual" != "null" ] && [ "$actual" != "" ]; then
    PASSED=$((PASSED + 1))
    echo -e "    ${GREEN}✔ PASS${NC} | $description"
    echo -e "           Valor: ${actual}"
  else
    FAILED=$((FAILED + 1))
    echo -e "    ${RED}✘ FAIL${NC} | $description"
    echo -e "           Valor está vazio ou null"
  fi
}

check_contains() {
  local description="$1"
  local expected_substring="$2"
  local actual="$3"
  TOTAL=$((TOTAL + 1))

  if echo "$actual" | grep -qi "$expected_substring"; then
    PASSED=$((PASSED + 1))
    echo -e "    ${GREEN}✔ PASS${NC} | $description"
    echo -e "           Contém: '${expected_substring}' em '${actual}'"
  else
    FAILED=$((FAILED + 1))
    echo -e "    ${RED}✘ FAIL${NC} | $description"
    echo -e "           Esperado conter: '${expected_substring}'  |  Obtido: '${actual}'"
  fi
}

warn() {
  WARNINGS=$((WARNINGS + 1))
  echo -e "    ${YELLOW}⚠ WARN${NC} | $1"
}

fail_fast() {
  echo -e "\n    ${RED}✘ FATAL${NC} | $1"
  echo -e "    ${RED}  Abortando validação.${NC}\n"
  print_summary
  exit 1
}

# Poll até que uma condição seja atendida
# Uso: poll_until "description" "curl_cmd" "jq_filter" "expected_value"
poll_until() {
  local description="$1"
  local curl_cmd="$2"
  local jq_filter="$3"
  local expected="$4"
  local attempts=0

  while [ $attempts -lt $MAX_POLL_ATTEMPTS ]; do
    local response
    response=$(eval "$curl_cmd" 2>/dev/null || echo "{}")
    local actual
    actual=$(echo "$response" | jq -r "$jq_filter" 2>/dev/null || echo "")

    if [ "$actual" = "$expected" ]; then
      check "$description" "$expected" "$actual"
      echo "$response"
      return 0
    fi

    attempts=$((attempts + 1))
    sleep "$POLL_INTERVAL"
  done

  # Timeout — reporta como falha
  local last_actual
  last_actual=$(eval "$curl_cmd" 2>/dev/null | jq -r "$jq_filter" 2>/dev/null || echo "TIMEOUT")
  check "$description (após ${MAX_POLL_ATTEMPTS} tentativas)" "$expected" "$last_actual"
  return 1
}

print_summary() {
  echo ""
  echo -e "${BOLD}${CYAN}═══════════════════════════════════════════════════════════════${NC}"
  echo -e "${BOLD}${CYAN}  RESULTADO DA VALIDAÇÃO E2E${NC}"
  echo -e "${BOLD}${CYAN}═══════════════════════════════════════════════════════════════${NC}"
  echo ""
  echo -e "  Total de verificações: ${BOLD}${TOTAL}${NC}"
  echo -e "  ${GREEN}✔ Passou:${NC}  ${BOLD}${PASSED}${NC}"
  echo -e "  ${RED}✘ Falhou:${NC}  ${BOLD}${FAILED}${NC}"
  echo -e "  ${YELLOW}⚠ Avisos:${NC}  ${BOLD}${WARNINGS}${NC}"
  echo ""

  if [ "$FAILED" -eq 0 ]; then
    echo -e "  ${GREEN}${BOLD}▶ FLUXO FIM A FIM VALIDADO COM SUCESSO ◀${NC}"
  else
    echo -e "  ${RED}${BOLD}▶ FALHAS DETECTADAS — VERIFIQUE OS LOGS ◀${NC}"
  fi
  echo ""
}

# ═══════════════════════════════════════════════════════════════════════════════
# PRÉ-REQUISITOS
# ═══════════════════════════════════════════════════════════════════════════════

print_header "VALIDAÇÃO FIM A FIM — Car Garage Microservices"
echo ""
echo "  OS Service:        ${OS_API}"
echo "  Billing Service:   ${BILLING_API}"
echo "  Execution Service: ${EXECUTION_API}"
echo "  Timestamp:         $(date -Iseconds)"

# Verificar dependências
for cmd in curl jq; do
  if ! command -v "$cmd" &>/dev/null; then
    fail_fast "'$cmd' não encontrado. Instale com: apt install $cmd / brew install $cmd"
  fi
done

# ═══════════════════════════════════════════════════════════════════════════════
# FASE 0 — HEALTH CHECK
# ═══════════════════════════════════════════════════════════════════════════════

print_header "FASE 0 — Health Check dos Serviços"

print_step "Verificando OS Service..."
OS_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "${OS_API}/actuator/health" 2>/dev/null || echo "000")
check "OS Service health endpoint responde 200" "200" "$OS_HEALTH"
[ "$OS_HEALTH" != "200" ] && fail_fast "OS Service não está acessível em ${OS_API}"

print_step "Verificando Billing Service..."
BILLING_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "${BILLING_API}/actuator/health" 2>/dev/null || echo "000")
check "Billing Service health endpoint responde 200" "200" "$BILLING_HEALTH"
[ "$BILLING_HEALTH" != "200" ] && fail_fast "Billing Service não está acessível em ${BILLING_API}"

print_step "Verificando Execution Service..."
EXEC_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "${EXECUTION_API}/actuator/health" 2>/dev/null || echo "000")
check "Execution Service health endpoint responde 200" "200" "$EXEC_HEALTH"
[ "$EXEC_HEALTH" != "200" ] && fail_fast "Execution Service não está acessível em ${EXECUTION_API}"

# ═══════════════════════════════════════════════════════════════════════════════
# FASE 1 — CRIAR ORDEM DE SERVIÇO (OS Service)
# ═══════════════════════════════════════════════════════════════════════════════

print_header "FASE 1 — Criar Ordem de Serviço (OS Service)"

print_step "POST ${OS_API}/service-orders"

TIMESTAMP=$(date +%s)
OS_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${OS_API}/service-orders" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerId\": 9001,
    \"customerName\": \"E2E Test Customer ${TIMESTAMP}\",
    \"vehicleId\": 9001,
    \"vehicleLicensePlate\": \"E2E${TIMESTAMP: -4}\",
    \"vehicleModel\": \"TestModel\",
    \"vehicleBrand\": \"TestBrand\",
    \"description\": \"Validacao E2E automatizada - ${TIMESTAMP}\",
    \"services\": [
      {
        \"serviceId\": 901,
        \"serviceName\": \"Servico E2E\",
        \"serviceDescription\": \"Servico de teste E2E\",
        \"price\": 150.00,
        \"quantity\": 1
      }
    ],
    \"resources\": [
      {
        \"resourceId\": 801,
        \"resourceName\": \"Peca E2E\",
        \"resourceDescription\": \"Peca de teste E2E\",
        \"resourceType\": \"PART\",
        \"price\": 200.00,
        \"quantity\": 2
      }
    ]
  }")

OS_HTTP_CODE=$(echo "$OS_RESPONSE" | tail -1)
OS_BODY=$(echo "$OS_RESPONSE" | sed '$d')

check "HTTP Status da criação da OS" "201" "$OS_HTTP_CODE"

ORDER_ID=$(echo "$OS_BODY" | jq -r '.id // empty')
ORDER_STATUS=$(echo "$OS_BODY" | jq -r '.status // empty')
ORDER_TOTAL=$(echo "$OS_BODY" | jq -r '.totalPrice // empty')
ORDER_SERVICES_COUNT=$(echo "$OS_BODY" | jq '.services | length' 2>/dev/null || echo "0")
ORDER_RESOURCES_COUNT=$(echo "$OS_BODY" | jq '.resources | length' 2>/dev/null || echo "0")

check_not_empty "OS criada com ID" "$ORDER_ID"
check "OS contém 1 serviço" "1" "$ORDER_SERVICES_COUNT"
check "OS contém 1 recurso" "1" "$ORDER_RESOURCES_COUNT"
check "Total calculado (150 + 200*2 = 550)" "550.00" "$ORDER_TOTAL"

if [ -z "$ORDER_ID" ] || [ "$ORDER_ID" = "null" ]; then
  fail_fast "Não foi possível obter o ID da OS criada"
fi

# OS with complete quote auto-advances to WAITING_APPROVAL
# (RECEIVED → IN_DIAGNOSIS → WAITING_APPROVAL happens inside CreateServiceOrderUseCase)
# The response still shows the initial status, so we poll for the final status
sleep 2
OS_CURRENT=$(curl -s "${OS_API}/service-orders/${ORDER_ID}" 2>/dev/null || echo "{}")
ORDER_STATUS_FINAL=$(echo "$OS_CURRENT" | jq -r '.status // empty' 2>/dev/null)
check "OS auto-avançou para WAITING_APPROVAL (quote completa)" "WAITING_APPROVAL" "$ORDER_STATUS_FINAL"

echo ""
echo -e "    ${CYAN}→ OS criada: ID=${ORDER_ID}, Status=${ORDER_STATUS_FINAL}, Total=${ORDER_TOTAL}${NC}"

# ═══════════════════════════════════════════════════════════════════════════════
# FASE 2 — VERIFICAR BUDGET CRIADO AUTOMATICAMENTE (Billing Service)
# ═══════════════════════════════════════════════════════════════════════════════

print_header "FASE 2 — Budget Criado Automaticamente (Billing Service)"

print_step "Aguardando Billing consumir ORDER_CREATED e criar Budget..."
echo "    (polling a cada ${POLL_INTERVAL}s, máx ${WAIT_BUDGET_CREATION}s)"

# Poll até encontrar budget para esta OS
BUDGET_RESPONSE=""
BUDGET_ID=""
ATTEMPTS=0
MAX_BUDGET_ATTEMPTS=$((WAIT_BUDGET_CREATION / POLL_INTERVAL))

while [ $ATTEMPTS -lt $MAX_BUDGET_ATTEMPTS ]; do
  BUDGET_RESPONSE=$(curl -s "${BILLING_API}/budgets/service-order/${ORDER_ID}" 2>/dev/null || echo "")

  if [ -n "$BUDGET_RESPONSE" ] && [ "$BUDGET_RESPONSE" != "null" ]; then
    # Pode retornar um objeto ou uma lista — normaliza para objeto
    BUDGET_ID=$(echo "$BUDGET_RESPONSE" | jq -r 'if type == "array" then .[0].budgetId else .budgetId end // empty' 2>/dev/null)
    if [ -n "$BUDGET_ID" ] && [ "$BUDGET_ID" != "null" ]; then
      break
    fi
  fi

  ATTEMPTS=$((ATTEMPTS + 1))
  sleep "$POLL_INTERVAL"
done

if [ -z "$BUDGET_ID" ] || [ "$BUDGET_ID" = "null" ]; then
  TOTAL=$((TOTAL + 1))
  FAILED=$((FAILED + 1))
  echo -e "    ${RED}✘ FAIL${NC} | Budget não foi criado após ${WAIT_BUDGET_CREATION}s"
  echo -e "           Verifique: OS publicou ORDER_CREATED? Billing consumer está rodando?"
  fail_fast "Budget não criado — integração OS → Billing falhou"
fi

BUDGET_STATUS=$(echo "$BUDGET_RESPONSE" | jq -r 'if type == "array" then .[0].status else .status end // empty' 2>/dev/null)
BUDGET_TOTAL=$(echo "$BUDGET_RESPONSE" | jq -r 'if type == "array" then .[0].totalAmount else .totalAmount end // empty' 2>/dev/null)
BUDGET_ITEMS_COUNT=$(echo "$BUDGET_RESPONSE" | jq 'if type == "array" then .[0].items else .items end | length' 2>/dev/null || echo "0")
BUDGET_SERVICE_ORDER=$(echo "$BUDGET_RESPONSE" | jq -r 'if type == "array" then .[0].serviceOrderId else .serviceOrderId end // empty' 2>/dev/null)

check_not_empty "Budget criado com ID" "$BUDGET_ID"
check "Budget associado à OS correta" "${ORDER_ID}" "$BUDGET_SERVICE_ORDER"
check "Status do Budget" "PENDING_APPROVAL" "$BUDGET_STATUS"
check "Budget contém 2 itens (1 service + 1 resource)" "2" "$BUDGET_ITEMS_COUNT"

if [ "$BUDGET_TOTAL" = "0" ] || [ "$BUDGET_TOTAL" = "0.00" ] || [ "$BUDGET_TOTAL" = "0.0" ]; then
  warn "Budget totalAmount é zero — itens podem ter sido criados sem preço"
else
  check_not_empty "Budget totalAmount preenchido" "$BUDGET_TOTAL"
fi

echo ""
echo -e "    ${CYAN}→ Budget: ID=${BUDGET_ID}, Status=${BUDGET_STATUS}, Total=${BUDGET_TOTAL}, Items=${BUDGET_ITEMS_COUNT}${NC}"

# ═══════════════════════════════════════════════════════════════════════════════
# FASE 3 — APROVAR BUDGET (Billing Service)
# ═══════════════════════════════════════════════════════════════════════════════

print_header "FASE 3 — Aprovar Budget (Billing Service)"

print_step "PUT ${BILLING_API}/budgets/${BUDGET_ID}/approve"

APPROVE_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "${BILLING_API}/budgets/${BUDGET_ID}/approve" \
  -H "Content-Type: application/json")

APPROVE_HTTP_CODE=$(echo "$APPROVE_RESPONSE" | tail -1)
APPROVE_BODY=$(echo "$APPROVE_RESPONSE" | sed '$d')

check "HTTP Status da aprovação" "200" "$APPROVE_HTTP_CODE"

APPROVED_STATUS=$(echo "$APPROVE_BODY" | jq -r '.status // empty' 2>/dev/null)
check "Budget status após aprovação" "APPROVED" "$APPROVED_STATUS"

echo ""
echo -e "    ${CYAN}→ Budget aprovado. BudgetApprovedEvent publicado nas filas.${NC}"

# Aguardar propagação — OS deve transicionar para IN_EXECUTION (via quote-approved-queue)
print_step "Verificando se OS transitou para IN_EXECUTION..."
sleep 5

OS_STATUS_NOW=""
ATTEMPTS=0
while [ $ATTEMPTS -lt 10 ]; do
  OS_STATUS_RESPONSE=$(curl -s "${OS_API}/service-orders/${ORDER_ID}" 2>/dev/null || echo "{}")
  OS_STATUS_NOW=$(echo "$OS_STATUS_RESPONSE" | jq -r '.status // empty' 2>/dev/null)

  if [ "$OS_STATUS_NOW" = "IN_EXECUTION" ]; then
    break
  fi
  ATTEMPTS=$((ATTEMPTS + 1))
  sleep "$POLL_INTERVAL"
done

check "OS transitou para IN_EXECUTION (via quote-approved-queue)" "IN_EXECUTION" "$OS_STATUS_NOW"

# ═══════════════════════════════════════════════════════════════════════════════
# FASE 4 — REGISTRAR PAGAMENTO (Billing Service)
# ═══════════════════════════════════════════════════════════════════════════════

print_header "FASE 4 — Registrar Pagamento (Billing Service)"

print_step "POST ${BILLING_API}/payments"

PAYMENT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BILLING_API}/payments" \
  -H "Content-Type: application/json" \
  -d "{
    \"budgetId\": \"${BUDGET_ID}\",
    \"method\": \"PIX\"
  }")

PAYMENT_HTTP_CODE=$(echo "$PAYMENT_RESPONSE" | tail -1)
PAYMENT_BODY=$(echo "$PAYMENT_RESPONSE" | sed '$d')

check "HTTP Status do pagamento" "201" "$PAYMENT_HTTP_CODE"

PAYMENT_ID=$(echo "$PAYMENT_BODY" | jq -r '.paymentId // empty' 2>/dev/null)
PAYMENT_STATUS=$(echo "$PAYMENT_BODY" | jq -r '.status // empty' 2>/dev/null)
PAYMENT_AMOUNT=$(echo "$PAYMENT_BODY" | jq -r '.amount // empty' 2>/dev/null)

check_not_empty "Payment criado com ID" "$PAYMENT_ID"
check "Payment status inicial" "PROCESSING" "$PAYMENT_STATUS"
check_not_empty "Payment amount preenchido" "$PAYMENT_AMOUNT"

echo ""
echo -e "    ${CYAN}→ Payment: ID=${PAYMENT_ID}, Status=${PAYMENT_STATUS}, Amount=${PAYMENT_AMOUNT}${NC}"

# ═══════════════════════════════════════════════════════════════════════════════
# FASE 5 — AGUARDAR PROCESSAMENTO DO GATEWAY (Billing Service)
# ═══════════════════════════════════════════════════════════════════════════════

print_header "FASE 5 — Processamento do Gateway de Pagamento"

print_step "Aguardando gateway processar (simulador, ~10s-30s)..."
echo "    (polling a cada ${POLL_INTERVAL}s, máx ${WAIT_PAYMENT_PROCESSING}s)"

PAYMENT_FINAL_STATUS=""
ATTEMPTS=0
MAX_PAYMENT_ATTEMPTS=$((WAIT_PAYMENT_PROCESSING / POLL_INTERVAL))

while [ $ATTEMPTS -lt $MAX_PAYMENT_ATTEMPTS ]; do
  PAYMENT_CHECK=$(curl -s "${BILLING_API}/payments/${PAYMENT_ID}" 2>/dev/null || echo "{}")
  PAYMENT_FINAL_STATUS=$(echo "$PAYMENT_CHECK" | jq -r '.status // empty' 2>/dev/null)

  if [ "$PAYMENT_FINAL_STATUS" = "PROCESSED" ] || [ "$PAYMENT_FINAL_STATUS" = "PAID" ] || [ "$PAYMENT_FINAL_STATUS" = "FAILED" ]; then
    break
  fi
  ATTEMPTS=$((ATTEMPTS + 1))
  sleep "$POLL_INTERVAL"
done

if [ "$PAYMENT_FINAL_STATUS" = "PROCESSED" ] || [ "$PAYMENT_FINAL_STATUS" = "PAID" ]; then
  check "Pagamento processado com sucesso" "PAID" "$PAYMENT_FINAL_STATUS"
  echo ""
  echo -e "    ${CYAN}→ Payment processado. PaymentProcessedEvent publicado.${NC}"

  # ═══════════════════════════════════════════════════════════════════════════
  # FASE 6 — VERIFICAR EXECUTION TASK (Execution Service)
  # ═══════════════════════════════════════════════════════════════════════════

  print_header "FASE 6 — ExecutionTask Criada (Execution Service)"

  print_step "Aguardando Execution consumir PaymentProcessed e criar task..."
  sleep "$POLL_INTERVAL"

  EXEC_TASK_RESPONSE=""
  EXEC_TASK_ID=""
  ATTEMPTS=0
  MAX_EXEC_ATTEMPTS=$((WAIT_EXECUTION_TASK / POLL_INTERVAL))

  while [ $ATTEMPTS -lt $MAX_EXEC_ATTEMPTS ]; do
    EXEC_TASK_RESPONSE=$(curl -s "${EXECUTION_API}/execution-tasks/service-order/${ORDER_ID}" 2>/dev/null || echo "")

    if [ -n "$EXEC_TASK_RESPONSE" ] && [ "$EXEC_TASK_RESPONSE" != "null" ]; then
      EXEC_TASK_ID=$(echo "$EXEC_TASK_RESPONSE" | jq -r 'if type == "array" then .[0].id else .id end // empty' 2>/dev/null)
      if [ -n "$EXEC_TASK_ID" ] && [ "$EXEC_TASK_ID" != "null" ]; then
        break
      fi
    fi

    ATTEMPTS=$((ATTEMPTS + 1))
    sleep "$POLL_INTERVAL"
  done

  if [ -z "$EXEC_TASK_ID" ] || [ "$EXEC_TASK_ID" = "null" ]; then
    TOTAL=$((TOTAL + 1))
    FAILED=$((FAILED + 1))
    echo -e "    ${RED}✘ FAIL${NC} | ExecutionTask não foi criada após ${WAIT_EXECUTION_TASK}s"
    echo -e "           Verifique: Billing publicou PaymentProcessed? Execution consumer está rodando?"
  else
    EXEC_STATUS=$(echo "$EXEC_TASK_RESPONSE" | jq -r 'if type == "array" then .[0].status else .status end // empty' 2>/dev/null)
    EXEC_ORDER_ID=$(echo "$EXEC_TASK_RESPONSE" | jq -r 'if type == "array" then .[0].serviceOrderId else .serviceOrderId end // empty' 2>/dev/null)

    check_not_empty "ExecutionTask criada com ID" "$EXEC_TASK_ID"
    check "ExecutionTask associada à OS correta" "${ORDER_ID}" "$EXEC_ORDER_ID"
    check_contains "ExecutionTask em estado inicial esperado" "QUEUED\|IN_PROGRESS\|STARTED" "$EXEC_STATUS"

    echo ""
    echo -e "    ${CYAN}→ ExecutionTask: ID=${EXEC_TASK_ID}, Status=${EXEC_STATUS}, OS=${EXEC_ORDER_ID}${NC}"

    # ═════════════════════════════════════════════════════════════════════════
    # FASE 7 — COMPLETAR EXECUÇÃO (Execution Service)
    # ═════════════════════════════════════════════════════════════════════════

    print_header "FASE 7 — Completar ExecutionTask (Execution Service)"

    # Se a task não estiver IN_PROGRESS, avança primeiro
    if [ "$EXEC_STATUS" = "QUEUED" ]; then
      print_step "PUT status para IN_PROGRESS"
      curl -s -X PUT "${EXECUTION_API}/execution-tasks/${EXEC_TASK_ID}/status" \
        -H "Content-Type: application/json" \
        -d '{"status": "IN_PROGRESS"}' > /dev/null 2>&1
      sleep 2
    fi

    print_step "PUT status para COMPLETED"
    COMPLETE_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT \
      "${EXECUTION_API}/execution-tasks/${EXEC_TASK_ID}/status" \
      -H "Content-Type: application/json" \
      -d '{"status": "COMPLETED"}')

    COMPLETE_HTTP_CODE=$(echo "$COMPLETE_RESPONSE" | tail -1)
    COMPLETE_BODY=$(echo "$COMPLETE_RESPONSE" | sed '$d')
    COMPLETE_STATUS=$(echo "$COMPLETE_BODY" | jq -r '.status // empty' 2>/dev/null)

    check "HTTP Status da atualização" "200" "$COMPLETE_HTTP_CODE"
    check "ExecutionTask completada" "COMPLETED" "$COMPLETE_STATUS"

    echo ""
    echo -e "    ${CYAN}→ ExecutionTask completada. ExecutionCompleted publicado.${NC}"

    # ═════════════════════════════════════════════════════════════════════════
    # FASE 8 — VERIFICAR OS FINALIZADA (OS Service)
    # ═════════════════════════════════════════════════════════════════════════

    print_header "FASE 8 — OS Finalizada (OS Service)"

    print_step "Verificando se OS transitou para FINISHED..."

    OS_FINAL_STATUS=""
    ATTEMPTS=0
    while [ $ATTEMPTS -lt 10 ]; do
      OS_FINAL_RESPONSE=$(curl -s "${OS_API}/service-orders/${ORDER_ID}" 2>/dev/null || echo "{}")
      OS_FINAL_STATUS=$(echo "$OS_FINAL_RESPONSE" | jq -r '.status // empty' 2>/dev/null)

      if [ "$OS_FINAL_STATUS" = "FINISHED" ]; then
        break
      fi
      ATTEMPTS=$((ATTEMPTS + 1))
      sleep "$POLL_INTERVAL"
    done

    check "OS transitou para FINISHED (via execution-completed-queue)" "FINISHED" "$OS_FINAL_STATUS"

    echo ""
    echo -e "    ${CYAN}→ OS final: ID=${ORDER_ID}, Status=${OS_FINAL_STATUS}${NC}"
  fi

elif [ "$PAYMENT_FINAL_STATUS" = "FAILED" ]; then
  # ═══════════════════════════════════════════════════════════════════════════
  # FLUXO ALTERNATIVO — PAGAMENTO FALHOU (Compensação)
  # ═══════════════════════════════════════════════════════════════════════════

  print_header "FASE 5b — Pagamento FALHOU (Compensação Saga)"

  warn "Pagamento falhou (simulador de gateway tem 10% de chance de falha)"
  check "Pagamento processado (status FAILED esperado na compensação)" "FAILED" "$PAYMENT_FINAL_STATUS"

  print_step "Verificando compensação — OS deve ser CANCELLED..."

  OS_COMPENSATED_STATUS=""
  ATTEMPTS=0
  while [ $ATTEMPTS -lt 15 ]; do
    OS_COMP_RESPONSE=$(curl -s "${OS_API}/service-orders/${ORDER_ID}" 2>/dev/null || echo "{}")
    OS_COMPENSATED_STATUS=$(echo "$OS_COMP_RESPONSE" | jq -r '.status // empty' 2>/dev/null)

    if [ "$OS_COMPENSATED_STATUS" = "CANCELLED" ]; then
      break
    fi
    ATTEMPTS=$((ATTEMPTS + 1))
    sleep "$POLL_INTERVAL"
  done

  check "OS cancelada via compensação (payment-failed-queue)" "CANCELLED" "$OS_COMPENSATED_STATUS"

  echo ""
  echo -e "    ${YELLOW}→ Fluxo de compensação executado. OS=${ORDER_ID} Status=${OS_COMPENSATED_STATUS}${NC}"
  echo -e "    ${YELLOW}  Re-execute o script para testar o happy path (90% de chance).${NC}"

else
  TOTAL=$((TOTAL + 1))
  FAILED=$((FAILED + 1))
  echo -e "    ${RED}✘ FAIL${NC} | Pagamento não processou dentro de ${WAIT_PAYMENT_PROCESSING}s"
  echo -e "           Status atual: ${PAYMENT_FINAL_STATUS}"
  echo -e "           Verifique: PaymentGatewaySimulator/@Scheduled está rodando?"
fi

# ═══════════════════════════════════════════════════════════════════════════════
# RESUMO
# ═══════════════════════════════════════════════════════════════════════════════

print_summary

# Dados para debug
echo -e "  ${BOLD}Dados criados nesta execução:${NC}"
echo -e "    OS ID:        ${ORDER_ID:-N/A}"
echo -e "    Budget ID:    ${BUDGET_ID:-N/A}"
echo -e "    Payment ID:   ${PAYMENT_ID:-N/A}"
echo -e "    Exec Task ID: ${EXEC_TASK_ID:-N/A}"
echo ""

# Exit code
[ "$FAILED" -eq 0 ] && exit 0 || exit 1
