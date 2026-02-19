package com.techchallenge.fiap.cargarage.execution_service.infrastructure.messaging;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskRequestDto;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.CreateExecutionTaskUseCase;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.FailExecutionTaskUseCase;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;

/**
 * AWS SQS message listener for consuming events from other services.
 * Handles Saga events: PaymentProcessed (start execution),
 * ServiceOrderCancelled (cancel execution).
 */
@Slf4j
@Component
public class SqsEventListener {

    private final CreateExecutionTaskUseCase createUseCase;
    private final FailExecutionTaskUseCase failUseCase;
    private final ObjectMapper objectMapper;

    public SqsEventListener(
            CreateExecutionTaskUseCase createUseCase,
            FailExecutionTaskUseCase failUseCase) {
        this.createUseCase = createUseCase;
        this.failUseCase = failUseCase;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Handles billing events (PaymentProcessed, PaymentFailed).
     * When payment is processed, creates an execution task in the queue.
     */
    @SqsListener("${messaging.sqs.queue.billing-events}")
    public void handleBillingEvent(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            String eventType = json.has("eventType")
                    ? json.get("eventType").asText()
                    : "";

            log.info("Received billing event: {}", eventType);

            switch (eventType) {
                case "PaymentProcessed" -> handlePaymentProcessed(json);
                case "PaymentFailed" -> handlePaymentFailed(json);
                case "PaymentRefunded" -> handlePaymentRefunded(json);
                default -> log.info("Ignoring billing event: {}", eventType);
            }
        } catch (JsonProcessingException e) {
            log.error("Error processing billing event", e);
            throw new RuntimeException("Failed to process billing event", e);
        } catch (Exception e) {
            log.error("Error handling billing event", e);
            throw e;
        }
    }

    /**
     * Handles OS service events (ServiceOrderCancelled).
     * When an OS is cancelled, cancels any associated execution task.
     */
    @SqsListener("${messaging.sqs.queue.os-events}")
    public void handleOsServiceEvent(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            String eventType = json.has("eventType")
                    ? json.get("eventType").asText()
                    : "";

            log.info("Received OS event: {}", eventType);

            if ("ORDER_CANCELLED".equals(eventType) || "ServiceOrderCancelled".equals(eventType)) {
                handleOrderCancelled(json);
            } else {
                log.debug("Ignoring OS event: {}", eventType);
            }
        } catch (JsonProcessingException e) {
            log.error("Error processing OS event", e);
            throw new RuntimeException("Failed to process OS event", e);
        } catch (Exception e) {
            log.error("Error handling OS event", e);
            throw e;
        }
    }

    private void handlePaymentProcessed(JsonNode json) {
        Long serviceOrderId = extractServiceOrderId(json);
        log.info("Payment processed for OS: {}. Creating execution task.", serviceOrderId);

        ExecutionTaskRequestDto request = ExecutionTaskRequestDto.builder()
                .serviceOrderId(serviceOrderId)
                .customerId(json.has("customerId")
                        ? json.get("customerId").asLong()
                        : null)
                .vehicleId(json.has("vehicleId")
                        ? json.get("vehicleId").asLong()
                        : null)
                .vehicleLicensePlate(json.has("vehicleLicensePlate")
                        ? json.get("vehicleLicensePlate").asText()
                        : null)
                .description("Execution for service order " + serviceOrderId)
                .priority(0)
                .build();

        createUseCase.execute(request);
        log.info("Execution task created for OS: {}", serviceOrderId);
    }

    private void handlePaymentFailed(JsonNode json) {
        Long serviceOrderId = extractServiceOrderId(json);
        String reason = json.has("failureReason")
                ? json.get("failureReason").asText()
                : "Payment failed";
        log.info("Payment failed for OS: {}. Cancelling execution if exists.",
                serviceOrderId);

        try {
            failUseCase.executeByServiceOrderId(serviceOrderId, reason);
        } catch (Exception e) {
            log.warn("No execution task to cancel for OS: {}", serviceOrderId);
        }
    }

    private void handlePaymentRefunded(JsonNode json) {
        Long serviceOrderId = extractServiceOrderId(json);
        String reason = json.has("refundReason")
                ? json.get("refundReason").asText()
                : "Payment refunded";
        log.info("Payment refunded for OS: {}. Cancelling execution if exists.",
                serviceOrderId);

        try {
            failUseCase.executeByServiceOrderId(serviceOrderId, reason);
        } catch (Exception e) {
            log.warn("No execution task to cancel for OS: {}", serviceOrderId);
        }
    }

    private void handleOrderCancelled(JsonNode json) {
        Long serviceOrderId = extractServiceOrderId(json);
        String reason = json.has("cancellationReason")
                ? json.get("cancellationReason").asText()
                : "Order cancelled";
        log.info("OS {} cancelled. Cancelling execution if exists.",
                serviceOrderId);

        try {
            failUseCase.executeByServiceOrderId(serviceOrderId, reason);
        } catch (Exception e) {
            log.warn("No execution task to cancel for OS: {}", serviceOrderId);
        }
    }

    private Long extractServiceOrderId(JsonNode json) {
        if (json.has("serviceOrderId")) {
            return json.get("serviceOrderId").asLong();
        }
        if (json.has("orderId")) {
            return json.get("orderId").asLong();
        }
        throw new RuntimeException("serviceOrderId or orderId not found in event");
    }
}
