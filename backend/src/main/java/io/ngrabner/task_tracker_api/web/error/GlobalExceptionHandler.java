package io.ngrabner.task_tracker_api.web.error;

import io.ngrabner.task_tracker_api.service.NotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String message = "Validation failed";
        if (!ex.getBindingResult().getFieldErrors().isEmpty()) {
            var fe = ex.getBindingResult().getFieldErrors().get(0);
            message = fe.getField() + ": " + fe.getDefaultMessage();
        }
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message,
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler({ NotFoundException.class, EntityNotFoundException.class })
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {

        ex.printStackTrace(); // ✅ TEMP: show full stack trace in console

        ErrorResponse body = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred",
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // Response DTO for NotFoundException
    public record ErrorResponse(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path) {
        public static ErrorResponse of(HttpStatus status, String error, String message, String path) {
            return new ErrorResponse(
                    Instant.now(),
                    status.value(),
                    error,
                    message,
                    path);
        }
    }
}
