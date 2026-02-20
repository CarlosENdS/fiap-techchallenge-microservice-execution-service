package com.techchallenge.fiap.cargarage.execution_service.application.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExecutionStatusEnumTest {

    @Test
    void shouldHaveFourValues() {
        assertEquals(4, ExecutionStatusEnum.values().length);
    }

    @ParameterizedTest
    @ValueSource(strings = { "QUEUED", "IN_PROGRESS", "COMPLETED", "FAILED" })
    void shouldParseValidStatusStrings(String status) {
        ExecutionStatusEnum result = ExecutionStatusEnum.fromString(status);
        assertNotNull(result);
        assertEquals(status, result.name());
    }

    @ParameterizedTest
    @ValueSource(strings = { "queued", "in_progress", "completed", "failed" })
    void shouldParseLowerCaseStrings(String status) {
        ExecutionStatusEnum result = ExecutionStatusEnum.fromString(status);
        assertNotNull(result);
    }

    @ParameterizedTest
    @ValueSource(strings = { "UNKNOWN", "PENDING", "", "null" })
    void shouldReturnNullForInvalidStrings(String status) {
        assertNull(ExecutionStatusEnum.fromString(status));
    }

    @Test
    void shouldReturnNullForNull() {
        assertNull(ExecutionStatusEnum.fromString(null));
    }

    @Test
    void shouldReturnCorrectEnumForCaseInsensitive() {
        assertEquals(ExecutionStatusEnum.QUEUED, ExecutionStatusEnum.fromString("Queued"));
        assertEquals(ExecutionStatusEnum.IN_PROGRESS, ExecutionStatusEnum.fromString("In_Progress"));
    }
}
