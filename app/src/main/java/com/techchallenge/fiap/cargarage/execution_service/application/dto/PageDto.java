package com.techchallenge.fiap.cargarage.execution_service.application.dto;

import java.util.List;

/**
 * Generic page DTO for paginated results.
 */
public record PageDto<T>(
        List<T> content,
        long totalElements,
        int pageNumber,
        int pageSize) {
}
