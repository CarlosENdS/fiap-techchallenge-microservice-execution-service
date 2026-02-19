package com.techchallenge.fiap.cargarage.execution_service.infrastructure.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techchallenge.fiap.cargarage.execution_service.application.controller.ExecutionTaskCleanArchController;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskRequestDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskStatusDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.ExecutionTaskStatusUpdateDto;
import com.techchallenge.fiap.cargarage.execution_service.application.dto.PageDto;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.NotFoundException;

@WebMvcTest(ExecutionTaskController.class)
class ExecutionTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExecutionTaskCleanArchController cleanArchController;

    private final LocalDateTime now = LocalDateTime.now();

    private ExecutionTaskDto createDto() {
        return ExecutionTaskDto.builder()
                .id(1L)
                .serviceOrderId(100L)
                .customerId(200L)
                .vehicleId(300L)
                .vehicleLicensePlate("ABC1D23")
                .description("Test task")
                .status("QUEUED")
                .priority(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    void shouldGetTaskById() throws Exception {
        when(cleanArchController.findById(1L)).thenReturn(createDto());

        mockMvc.perform(get("/execution-tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void shouldReturn404WhenNotFound() throws Exception {
        when(cleanArchController.findById(999L))
                .thenThrow(new NotFoundException("Not found"));

        mockMvc.perform(get("/execution-tasks/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetByServiceOrderId() throws Exception {
        when(cleanArchController.findByServiceOrderId(100L))
                .thenReturn(createDto());

        mockMvc.perform(get("/execution-tasks/service-order/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceOrderId").value(100));
    }

    @Test
    void shouldGetAllPaginated() throws Exception {
        PageDto<ExecutionTaskDto> page = new PageDto<>(
                List.of(createDto()), 1, 0, 15);
        when(cleanArchController.findAll(0, 15)).thenReturn(page);

        mockMvc.perform(get("/execution-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldGetByStatus() throws Exception {
        PageDto<ExecutionTaskDto> page = new PageDto<>(
                List.of(createDto()), 1, 0, 15);
        when(cleanArchController.findByStatus("QUEUED", 0, 15))
                .thenReturn(page);

        mockMvc.perform(get("/execution-tasks/status/QUEUED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("QUEUED"));
    }

    @Test
    void shouldCreateTask() throws Exception {
        ExecutionTaskRequestDto request = ExecutionTaskRequestDto.builder()
                .serviceOrderId(100L)
                .description("New task")
                .build();

        when(cleanArchController.create(any(ExecutionTaskRequestDto.class)))
                .thenReturn(createDto());

        mockMvc.perform(post("/execution-tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldReturn400WhenCreateWithoutServiceOrderId() throws Exception {
        String body = "{}";

        mockMvc.perform(post("/execution-tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateStatus() throws Exception {
        ExecutionTaskDto updated = ExecutionTaskDto.builder()
                .id(1L)
                .serviceOrderId(100L)
                .status("IN_PROGRESS")
                .createdAt(now)
                .build();

        when(cleanArchController.updateStatus(eq(1L),
                any(ExecutionTaskStatusUpdateDto.class))).thenReturn(updated);

        String body = objectMapper.writeValueAsString(
                ExecutionTaskStatusUpdateDto.builder()
                        .status("IN_PROGRESS").build());

        mockMvc.perform(put("/execution-tasks/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void shouldGetStatus() throws Exception {
        when(cleanArchController.getStatus(1L))
                .thenReturn(ExecutionTaskStatusDto.builder()
                        .status("QUEUED").build());

        mockMvc.perform(get("/execution-tasks/1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void shouldFailTask() throws Exception {
        ExecutionTaskDto failed = ExecutionTaskDto.builder()
                .id(1L)
                .serviceOrderId(100L)
                .status("FAILED")
                .failureReason("reason")
                .createdAt(now)
                .build();

        when(cleanArchController.fail(1L, "reason")).thenReturn(failed);

        mockMvc.perform(delete("/execution-tasks/1")
                .param("reason", "reason"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }
}
