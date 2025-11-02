package com.oreo.insight_factory.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Object> build(HttpStatus status, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", status.name());
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());
        body.put("path", path);
        return ResponseEntity.status(status).body(body);
    }

    // 400: Bad Request
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex, WebRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Validación fallida: " + ex.getMessage(), req.getDescription(false));
    }

    // 401: No autenticado
    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<Object> handleUnauthorized(Exception ex, WebRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas o usuario no autenticado", req.getDescription(false));
    }

    // 403: Sin permisos
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Object> handleForbidden(Exception ex, WebRequest req) {
        return build(HttpStatus.FORBIDDEN, "No tienes permisos para acceder a este recurso", req.getDescription(false));
    }

    // 404: Recurso no encontrado
    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<Object> handleNotFound(Exception ex, WebRequest req) {
        return build(HttpStatus.NOT_FOUND, "Recurso no encontrado: " + ex.getMessage(), req.getDescription(false));
    }

    // 409: Conflicto (registro duplicado)
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleConflict(Exception ex, WebRequest req) {
        return build(HttpStatus.CONFLICT, "Conflicto con datos existentes", req.getDescription(false));
    }

    // 503: Servicio no disponible (Mail o LLM)
    @ExceptionHandler({jakarta.mail.MessagingException.class, java.net.ConnectException.class})
    public ResponseEntity<Object> handleServiceUnavailable(Exception ex, WebRequest req) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Servicio externo no disponible (LLM o Email)", req.getDescription(false));
    }

    // Default: 500 Internal Server Error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneral(Exception ex, WebRequest req) {
        ex.printStackTrace();
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor: " + ex.getMessage(), req.getDescription(false));
    }
}
