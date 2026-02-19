package com.techchallenge.fiap.cargarage.execution_service.infrastructure.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.techchallenge.fiap.cargarage.execution_service.application.dto.ErrorMessageDto;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.BusinessException;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.InvalidDataException;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.NotFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private WebRequest createWebRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/test");
        return new ServletWebRequest(request);
    }

    @Test
    void shouldHandleInvalidDataException() {
        InvalidDataException ex = new InvalidDataException("Invalid data");
        ResponseEntity<ErrorMessageDto> response = handler.handleInvalidDataException(ex, createWebRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid data", response.getBody().message());
        assertEquals(400, response.getBody().status());
    }

    @Test
    void shouldHandleBusinessException() {
        BusinessException ex = new BusinessException("Business error");
        ResponseEntity<ErrorMessageDto> response = handler.handleBusinessException(ex, createWebRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Business error", response.getBody().message());
    }

    @Test
    void shouldHandleNotFoundException() {
        NotFoundException ex = new NotFoundException("Not found");
        ResponseEntity<ErrorMessageDto> response = handler.handleNotFoundException(ex, createWebRequest());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Not found", response.getBody().message());
        assertEquals(404, response.getBody().status());
    }

    @Test
    void shouldHandleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Bad argument");
        ResponseEntity<ErrorMessageDto> response = handler.handleIllegalArgumentException(ex, createWebRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Bad argument", response.getBody().message());
    }

    @Test
    void shouldHandleGenericException() {
        Exception ex = new Exception("Unexpected error");
        ResponseEntity<Object> response = handler.handleAllExceptions(ex, createWebRequest());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());
        assertNotNull(response.getBody());
        ErrorMessageDto body = (ErrorMessageDto) response.getBody();
        assertEquals("Unexpected error", body.message());
        assertEquals(500, body.status());
    }

    @Test
    void shouldIncludeTimestampInResponse() {
        NotFoundException ex = new NotFoundException("test");
        ResponseEntity<ErrorMessageDto> response = handler.handleNotFoundException(ex, createWebRequest());

        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void shouldIncludePathInResponse() {
        NotFoundException ex = new NotFoundException("test");
        ResponseEntity<ErrorMessageDto> response = handler.handleNotFoundException(ex, createWebRequest());

        assertNotNull(response.getBody().path());
    }
}
