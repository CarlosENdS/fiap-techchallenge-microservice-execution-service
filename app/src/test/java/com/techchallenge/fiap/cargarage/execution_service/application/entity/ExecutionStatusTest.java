package com.techchallenge.fiap.cargarage.execution_service.application.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.techchallenge.fiap.cargarage.execution_service.application.exception.InvalidDataException;

class ExecutionStatusTest {

    @Test
    void shouldCreateQueuedStatus() {
        ExecutionStatus status = ExecutionStatus.queued();
        assertNotNull(status);
        assertEquals("QUEUED", status.value());
        assertTrue(status.isQueued());
        assertFalse(status.isInProgress());
        assertFalse(status.isCompleted());
        assertFalse(status.isFailed());
    }

    @Test
    void shouldCreateInProgressStatus() {
        ExecutionStatus status = ExecutionStatus.inProgress();
        assertEquals("IN_PROGRESS", status.value());
        assertTrue(status.isInProgress());
        assertFalse(status.isQueued());
    }

    @Test
    void shouldCreateCompletedStatus() {
        ExecutionStatus status = ExecutionStatus.completed();
        assertEquals("COMPLETED", status.value());
        assertTrue(status.isCompleted());
    }

    @Test
    void shouldCreateFailedStatus() {
        ExecutionStatus status = ExecutionStatus.failed();
        assertEquals("FAILED", status.value());
        assertTrue(status.isFailed());
    }

    @Test
    void shouldCreateFromValidString() {
        ExecutionStatus status = ExecutionStatus.of("QUEUED");
        assertEquals("QUEUED", status.value());
    }

    @Test
    void shouldCreateFromLowerCaseString() {
        ExecutionStatus status = ExecutionStatus.of("in_progress");
        assertEquals("IN_PROGRESS", status.value());
    }

    @Test
    void shouldCreateFromStringWithSpaces() {
        ExecutionStatus status = ExecutionStatus.of("  COMPLETED  ");
        assertEquals("COMPLETED", status.value());
    }

    @Test
    void shouldThrowForNullString() {
        assertThrows(IllegalArgumentException.class,
                () -> ExecutionStatus.of(null));
    }

    @Test
    void shouldThrowForBlankString() {
        assertThrows(IllegalArgumentException.class,
                () -> ExecutionStatus.of("   "));
    }

    @Test
    void shouldThrowForInvalidString() {
        assertThrows(InvalidDataException.class,
                () -> ExecutionStatus.of("UNKNOWN"));
    }

    // Transition tests
    @Test
    void shouldAllowQueuedToInProgress() {
        assertTrue(ExecutionStatus.queued()
                .canTransitionTo(ExecutionStatus.inProgress()));
    }

    @Test
    void shouldAllowQueuedToFailed() {
        assertTrue(ExecutionStatus.queued()
                .canTransitionTo(ExecutionStatus.failed()));
    }

    @Test
    void shouldNotAllowQueuedToCompleted() {
        assertFalse(ExecutionStatus.queued()
                .canTransitionTo(ExecutionStatus.completed()));
    }

    @Test
    void shouldAllowInProgressToCompleted() {
        assertTrue(ExecutionStatus.inProgress()
                .canTransitionTo(ExecutionStatus.completed()));
    }

    @Test
    void shouldAllowInProgressToFailed() {
        assertTrue(ExecutionStatus.inProgress()
                .canTransitionTo(ExecutionStatus.failed()));
    }

    @Test
    void shouldNotAllowInProgressToQueued() {
        assertFalse(ExecutionStatus.inProgress()
                .canTransitionTo(ExecutionStatus.queued()));
    }

    @Test
    void shouldNotAllowCompletedToAny() {
        assertFalse(ExecutionStatus.completed()
                .canTransitionTo(ExecutionStatus.queued()));
        assertFalse(ExecutionStatus.completed()
                .canTransitionTo(ExecutionStatus.inProgress()));
        assertFalse(ExecutionStatus.completed()
                .canTransitionTo(ExecutionStatus.failed()));
    }

    @Test
    void shouldNotAllowFailedToAny() {
        assertFalse(ExecutionStatus.failed()
                .canTransitionTo(ExecutionStatus.queued()));
        assertFalse(ExecutionStatus.failed()
                .canTransitionTo(ExecutionStatus.inProgress()));
        assertFalse(ExecutionStatus.failed()
                .canTransitionTo(ExecutionStatus.completed()));
    }

    @Test
    void shouldNotTransitionToNull() {
        assertFalse(ExecutionStatus.queued().canTransitionTo(null));
    }

    @Test
    void shouldReturnStringRepresentation() {
        assertEquals("QUEUED", ExecutionStatus.queued().toString());
    }

    @Test
    void shouldBeEqualWhenSameStatus() {
        assertEquals(ExecutionStatus.queued(), ExecutionStatus.queued());
        assertEquals(ExecutionStatus.of("QUEUED"), ExecutionStatus.queued());
    }

    @Test
    void shouldNotBeEqualWhenDifferentStatus() {
        assertFalse(ExecutionStatus.queued().equals(ExecutionStatus.inProgress()));
    }
}
