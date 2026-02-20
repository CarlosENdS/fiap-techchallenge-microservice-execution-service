package com.techchallenge.fiap.cargarage.execution_service.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskRequestDto;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionStatus;
import com.techchallenge.fiap.cargarage.execution_service.application.entity.ExecutionTask;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.CreateExecutionTaskUseCase;
import com.techchallenge.fiap.cargarage.execution_service.application.usecase.FailExecutionTaskUseCase;

@ExtendWith(MockitoExtension.class)
class SqsEventListenerTest {

    @Mock
    private CreateExecutionTaskUseCase createUseCase;

    @Mock
    private FailExecutionTaskUseCase failUseCase;

    private SqsEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new SqsEventListener(createUseCase, failUseCase);
    }

    // Billing events

    @Test
    void shouldHandlePaymentProcessedEvent() {
        String message = """
                {
                  "eventType": "PaymentProcessed",
                  "serviceOrderId": 100,
                  "customerId": 200,
                  "vehicleId": 300,
                  "vehicleLicensePlate": "ABC1D23"
                }
                """;

        ExecutionTask task = ExecutionTask.builder()
                .id(1L)
                .serviceOrderId(100L)
                .status(ExecutionStatus.queued())
                .createdAt(LocalDateTime.now())
                .build();
        when(createUseCase.execute(any(ExecutionTaskRequestDto.class)))
                .thenReturn(task);

        listener.handleBillingEvent(message);

        verify(createUseCase).execute(any(ExecutionTaskRequestDto.class));
    }

    @Test
    void shouldHandlePaymentFailedEvent() {
        String message = """
                {
                  "eventType": "PaymentFailed",
                  "serviceOrderId": 100,
                  "failureReason": "Insufficient funds"
                }
                """;

        ExecutionTask task = ExecutionTask.builder()
                .id(1L)
                .serviceOrderId(100L)
                .status(ExecutionStatus.failed())
                .createdAt(LocalDateTime.now())
                .build();
        when(failUseCase.executeByServiceOrderId(100L, "Insufficient funds"))
                .thenReturn(task);

        listener.handleBillingEvent(message);

        verify(failUseCase).executeByServiceOrderId(100L, "Insufficient funds");
    }

    @Test
    void shouldHandlePaymentRefundedEvent() {
        String message = """
                {
                  "eventType": "PaymentRefunded",
                  "serviceOrderId": 100,
                  "refundReason": "Customer request"
                }
                """;

        ExecutionTask task = ExecutionTask.builder()
                .id(1L)
                .serviceOrderId(100L)
                .status(ExecutionStatus.failed())
                .createdAt(LocalDateTime.now())
                .build();
        when(failUseCase.executeByServiceOrderId(100L, "Customer request"))
                .thenReturn(task);

        listener.handleBillingEvent(message);

        verify(failUseCase).executeByServiceOrderId(100L, "Customer request");
    }

    @Test
    void shouldIgnoreUnknownBillingEvent() {
        String message = """
                {
                  "eventType": "UnknownEvent",
                  "serviceOrderId": 100
                }
                """;

        listener.handleBillingEvent(message);

        verify(createUseCase, never()).execute(any());
        verify(failUseCase, never()).executeByServiceOrderId(any(), any());
    }

    @Test
    void shouldHandlePaymentFailedWhenNoTaskExists() {
        String message = """
                {
                  "eventType": "PaymentFailed",
                  "serviceOrderId": 100
                }
                """;

        when(failUseCase.executeByServiceOrderId(any(), any()))
                .thenThrow(new RuntimeException("Not found"));

        // Should not throw - swallows exception
        listener.handleBillingEvent(message);
    }

    @Test
    void shouldThrowForInvalidJsonBillingEvent() {
        assertThrows(RuntimeException.class,
                () -> listener.handleBillingEvent("invalid json"));
    }

    // OS events

    @Test
    void shouldHandleOrderCancelledEvent() {
        String message = """
                {
                  "eventType": "ORDER_CANCELLED",
                  "serviceOrderId": 100,
                  "cancellationReason": "Customer cancelled"
                }
                """;

        ExecutionTask task = ExecutionTask.builder()
                .id(1L)
                .serviceOrderId(100L)
                .status(ExecutionStatus.failed())
                .createdAt(LocalDateTime.now())
                .build();
        when(failUseCase.executeByServiceOrderId(100L, "Customer cancelled"))
                .thenReturn(task);

        listener.handleOsServiceEvent(message);

        verify(failUseCase)
                .executeByServiceOrderId(100L, "Customer cancelled");
    }

    @Test
    void shouldHandleServiceOrderCancelledEvent() {
        String message = """
                {
                  "eventType": "ServiceOrderCancelled",
                  "orderId": 100,
                  "cancellationReason": "Timeout"
                }
                """;

        ExecutionTask task = ExecutionTask.builder()
                .id(1L)
                .serviceOrderId(100L)
                .status(ExecutionStatus.failed())
                .createdAt(LocalDateTime.now())
                .build();
        when(failUseCase.executeByServiceOrderId(100L, "Timeout"))
                .thenReturn(task);

        listener.handleOsServiceEvent(message);

        verify(failUseCase).executeByServiceOrderId(100L, "Timeout");
    }

    @Test
    void shouldIgnoreUnknownOsEvent() {
        String message = """
                {
                  "eventType": "UnknownOsEvent",
                  "serviceOrderId": 100
                }
                """;

        listener.handleOsServiceEvent(message);

        verify(failUseCase, never()).executeByServiceOrderId(any(), any());
    }

    @Test
    void shouldHandleCancelWhenNoTaskExists() {
        String message = """
                {
                  "eventType": "ORDER_CANCELLED",
                  "serviceOrderId": 100
                }
                """;

        when(failUseCase.executeByServiceOrderId(any(), any()))
                .thenThrow(new RuntimeException("Not found"));

        // Should not throw
        listener.handleOsServiceEvent(message);
    }

    @Test
    void shouldExtractOrderIdField() {
        String message = """
                {
                  "eventType": "PaymentProcessed",
                  "orderId": 100
                }
                """;

        ExecutionTask task = ExecutionTask.builder()
                .id(1L)
                .serviceOrderId(100L)
                .status(ExecutionStatus.queued())
                .createdAt(LocalDateTime.now())
                .build();
        when(createUseCase.execute(any(ExecutionTaskRequestDto.class)))
                .thenReturn(task);

        listener.handleBillingEvent(message);

        verify(createUseCase).execute(any(ExecutionTaskRequestDto.class));
    }
}
