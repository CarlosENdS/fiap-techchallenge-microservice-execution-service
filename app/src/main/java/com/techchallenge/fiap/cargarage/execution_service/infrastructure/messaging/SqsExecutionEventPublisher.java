package com.techchallenge.fiap.cargarage.execution_service.infrastructure.messaging;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionTask;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * AWS SQS implementation of ExecutionEventPublisher.
 * Publishes events to SQS queues for Saga pattern integration.
 */
@Slf4j
@Component
public class SqsExecutionEventPublisher implements ExecutionEventPublisher {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${messaging.sqs.queue.execution-events-url}")
    private String executionEventsQueueUrl;

    @Value("${messaging.sqs.queue.execution-completed-url}")
    private String executionCompletedQueueUrl;

    @Value("${messaging.sqs.queue.resource-unavailable-url}")
    private String resourceUnavailableQueueUrl;

    public SqsExecutionEventPublisher(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void publishExecutionStarted(ExecutionTask task) {
        publishFifoEvent("ExecutionStarted", task);
    }

    @Override
    public void publishExecutionCompleted(ExecutionTask task) {
        // Publish to FIFO queue for Saga tracking
        publishFifoEvent("ExecutionCompleted", task);
        // Publish to standard queue for OS Service consumption
        publishToOsServiceQueue(executionCompletedQueueUrl, task);
    }

    @Override
    public void publishExecutionFailed(ExecutionTask task) {
        // Publish to FIFO queue for Saga tracking
        publishFifoEvent("ExecutionFailed", task);
        // Publish compensation event to OS Service
        publishToOsServiceQueue(resourceUnavailableQueueUrl, task);
    }

    private void publishFifoEvent(String eventType, ExecutionTask task) {
        try {
            ExecutionEventDto event = ExecutionEventDto.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .executionTaskId(task.id())
                    .serviceOrderId(task.serviceOrderId())
                    .customerId(task.customerId())
                    .vehicleId(task.vehicleId())
                    .vehicleLicensePlate(task.vehicleLicensePlate())
                    .status(task.status() != null ? task.status().value() : null)
                    .failureReason(task.failureReason())
                    .timestamp(LocalDateTime.now())
                    .build();

            String messageBody = objectMapper.writeValueAsString(event);

            Map<String, MessageAttributeValue> attrs = new HashMap<>();
            attrs.put("eventType", MessageAttributeValue.builder()
                    .stringValue(eventType).dataType("String").build());
            attrs.put("serviceOrderId", MessageAttributeValue.builder()
                    .stringValue(task.serviceOrderId().toString())
                    .dataType("String").build());

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(executionEventsQueueUrl)
                    .messageBody(messageBody)
                    .messageAttributes(attrs)
                    .messageGroupId("execution-service-events")
                    .messageDeduplicationId(
                            task.id() + "-" + eventType + "-" + System.currentTimeMillis())
                    .build();

            sqsClient.sendMessage(request);
            log.info("Published FIFO event: {} for task: {} (OS: {})",
                    eventType, task.id(), task.serviceOrderId());
        } catch (JsonProcessingException e) {
            log.error("Error serializing event for task: {}", task.id(), e);
            throw new RuntimeException("Failed to serialize event", e);
        } catch (Exception e) {
            log.error("Error publishing FIFO event for task: {}", task.id(), e);
            throw new RuntimeException("Failed to publish event to SQS", e);
        }
    }

    /**
     * Publishes a simple event to a standard queue consumed by OS Service.
     * Payload: { "orderId": <serviceOrderId>, "reason": "<failureReason>" }
     */
    private void publishToOsServiceQueue(String queueUrl, ExecutionTask task) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderId", task.serviceOrderId());
            if (task.failureReason() != null) {
                payload.put("reason", task.failureReason());
            }

            String messageBody = objectMapper.writeValueAsString(payload);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build();

            sqsClient.sendMessage(request);
            log.info("Published to OS Service queue: {} for OS: {}",
                    queueUrl, task.serviceOrderId());
        } catch (JsonProcessingException e) {
            log.error("Error serializing OS event for task: {}", task.id(), e);
            throw new RuntimeException("Failed to serialize event", e);
        } catch (Exception e) {
            log.error("Error publishing to OS Service queue for task: {}",
                    task.id(), e);
            throw new RuntimeException("Failed to publish event to SQS", e);
        }
    }
}
