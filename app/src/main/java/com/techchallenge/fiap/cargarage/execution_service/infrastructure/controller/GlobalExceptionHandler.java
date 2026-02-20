package com.techchallenge.fiap.cargarage.execution_service.infrastructure.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.techchallenge.fiap.cargarage.execution_service.application.dto.ErrorMessageDto;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.BusinessException;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.InvalidDataException;
import com.techchallenge.fiap.cargarage.execution_service.application.exception.NotFoundException;

/**
 * Global exception handler for the application.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Supplier<String> fieldErrorSupplier = () -> ex.getBindingResult()
                .getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList().toString();

        ErrorMessageDto errorMessage = getErrorMessage(
                "Validation Error", fieldErrorSupplier, status, request);

        return ResponseEntity.status(status).headers(headers).body(errorMessage);
    }

    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ErrorMessageDto> handleInvalidDataException(
            InvalidDataException ex, WebRequest request) {
        ErrorMessageDto errorMessage = getErrorMessage(
                BAD_REQUEST.toString(), ex::getMessage, BAD_REQUEST, request);
        return new ResponseEntity<>(errorMessage, BAD_REQUEST);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorMessageDto> handleBusinessException(
            BusinessException ex, WebRequest request) {
        ErrorMessageDto errorMessage = getErrorMessage(
                BAD_REQUEST.toString(), ex::getMessage, BAD_REQUEST, request);
        return new ResponseEntity<>(errorMessage, BAD_REQUEST);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleNotFoundException(
            NotFoundException ex, WebRequest request) {
        ErrorMessageDto errorMessage = getErrorMessage(
                NOT_FOUND.toString(), ex::getMessage, NOT_FOUND, request);
        return new ResponseEntity<>(errorMessage, NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorMessageDto> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        ErrorMessageDto errorMessage = getErrorMessage(
                BAD_REQUEST.toString(), ex::getMessage, BAD_REQUEST, request);
        return new ResponseEntity<>(errorMessage, BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(
            Exception ex, WebRequest request) {
        ErrorMessageDto errorMessage = getErrorMessage(
                "Internal Server Error", ex::getMessage,
                INTERNAL_SERVER_ERROR, request);
        return new ResponseEntity<>(errorMessage, INTERNAL_SERVER_ERROR);
    }

    private ErrorMessageDto getErrorMessage(
            String error, Supplier<String> messageSupplier,
            HttpStatusCode status, WebRequest request) {
        return ErrorMessageDto.builder()
                .error(error)
                .message(messageSupplier.get())
                .status(status.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(LocalDateTime.now())
                .build();
    }
}
