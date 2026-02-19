package com.techchallenge.fiap.cargarage.execution_service.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionStatus;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionTask;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@ExtendWith(MockitoExtension.class)
class SqsExecutionEventPublisherTest {

    @Mock
    private SqsClient sqsClient;

    private SqsExecutionEventPublisher publisher;

    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        publisher = new SqsExecutionEventPublisher(sqsClient);
        ReflectionTestUtils.setField(publisher, "executionEventsQueueUrl",
                "http://localhost:4566/000000000000/execution-service-events.fifo");
        ReflectionTestUtils.setField(publisher, "executionCompletedQueueUrl",
                "http://localhost:4566/000000000000/execution-completed-queue");
        ReflectionTestUtils.setField(publisher, "resourceUnavailableQueueUrl",
                "http://localhost:4566/000000000000/resource-unavailable-queue");
    }

    private ExecutionTask createTask(ExecutionStatus status) {
        return ExecutionTask.builder()
                .id(1L)
                .serviceOrderId(100L)
                .customerId(200L)
                .vehicleId(300L)
                .vehicleLicensePlate("ABC1D23")
                .status(status)
                .failureReason(status.isFailed() ? "Test failure" : null)
                .createdAt(now)
                .build();
    }

    @Test
    void shouldPublishExecutionStarted() {
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().build());

        publisher.publishExecutionStarted(createTask(ExecutionStatus.inProgress()));

        // Only FIFO queue
        verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldPublishExecutionCompleted() {
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().build());

        publisher.publishExecutionCompleted(
                createTask(ExecutionStatus.completed()));

        // FIFO queue + execution-completed-queue
        verify(sqsClient, times(2)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldPublishExecutionFailed() {
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().build());

        publisher.publishExecutionFailed(createTask(ExecutionStatus.failed()));

        // FIFO queue + resource-unavailable-queue
        verify(sqsClient, times(2)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldThrowWhenSqsFails() {
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(new RuntimeException("SQS error"));

        ExecutionTask task = createTask(ExecutionStatus.inProgress());

        assertThrows(RuntimeException.class,
                () -> publisher.publishExecutionStarted(task));
    }
}
