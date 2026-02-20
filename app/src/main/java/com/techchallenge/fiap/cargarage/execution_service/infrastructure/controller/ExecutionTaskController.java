package com.techchallenge.fiap.cargarage.execution_service.infrastructure.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.techchallenge.fiap.cargarage.execution_service.application.controller.ExecutionTaskCleanArchController;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ErrorMessageDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskRequestDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskStatusDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskStatusUpdateDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageDto;

/**
 * REST controller for Execution Task endpoints.
 */
@Tag(name = "Execution Tasks", description = "Execution queue management endpoints")
@RestController
@RequiredArgsConstructor
@RequestMapping("/execution-tasks")
public class ExecutionTaskController {

    private final ExecutionTaskCleanArchController controller;

    @Operation(summary = "Get execution task by ID")
    @ApiResponse(responseCode = "200", description = "Task found")
    @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(schema = @Schema(implementation = ErrorMessageDto.class)))
    @GetMapping("/{id}")
    public ResponseEntity<ExecutionTaskDto> findById(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        return ResponseEntity.ok(controller.findById(id));
    }

    @Operation(summary = "Get execution task by service order ID")
    @ApiResponse(responseCode = "200", description = "Task found")
    @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(schema = @Schema(implementation = ErrorMessageDto.class)))
    @GetMapping("/service-order/{serviceOrderId}")
    public ResponseEntity<ExecutionTaskDto> findByServiceOrderId(
            @Parameter(description = "Service Order ID") @PathVariable Long serviceOrderId) {
        return ResponseEntity.ok(controller.findByServiceOrderId(serviceOrderId));
    }

    @Operation(summary = "Get all execution tasks (paginated)")
    @ApiResponse(responseCode = "200", description = "List of tasks")
    @GetMapping
    public ResponseEntity<PageDto<ExecutionTaskDto>> findAll(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size) {
        return ResponseEntity.ok(controller.findAll(page, size));
    }

    @Operation(summary = "Get execution tasks by status")
    @ApiResponse(responseCode = "200", description = "List of tasks")
    @GetMapping("/status/{status}")
    public ResponseEntity<PageDto<ExecutionTaskDto>> findByStatus(
            @Parameter(description = "Task status") @PathVariable String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size) {
        return ResponseEntity.ok(controller.findByStatus(status, page, size));
    }

    @Operation(summary = "Create a new execution task")
    @ApiResponse(responseCode = "201", description = "Task created")
    @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorMessageDto.class)))
    @PostMapping
    public ResponseEntity<ExecutionTaskDto> create(
            @Valid @RequestBody ExecutionTaskRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(controller.create(requestDto));
    }

    @Operation(summary = "Update execution task status")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "400", description = "Invalid transition", content = @Content(schema = @Schema(implementation = ErrorMessageDto.class)))
    @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(schema = @Schema(implementation = ErrorMessageDto.class)))
    @PutMapping("/{id}/status")
    public ResponseEntity<ExecutionTaskDto> updateStatus(
            @Parameter(description = "Task ID") @PathVariable Long id,
            @Valid @RequestBody ExecutionTaskStatusUpdateDto statusDto) {
        return ResponseEntity.ok(controller.updateStatus(id, statusDto));
    }

    @Operation(summary = "Get execution task status")
    @ApiResponse(responseCode = "200", description = "Status returned")
    @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(schema = @Schema(implementation = ErrorMessageDto.class)))
    @GetMapping("/{id}/status")
    public ResponseEntity<ExecutionTaskStatusDto> getStatus(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        return ResponseEntity.ok(controller.getStatus(id));
    }

    @Operation(summary = "Fail an execution task (compensation)")
    @ApiResponse(responseCode = "200", description = "Task failed")
    @ApiResponse(responseCode = "400", description = "Cannot fail task", content = @Content(schema = @Schema(implementation = ErrorMessageDto.class)))
    @ApiResponse(responseCode = "404", description = "Task not found", content = @Content(schema = @Schema(implementation = ErrorMessageDto.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<ExecutionTaskDto> fail(
            @Parameter(description = "Task ID") @PathVariable Long id,
            @RequestParam(value = "reason", required = false) String reason) {
        return ResponseEntity.ok(controller.fail(id, reason));
    }
}
