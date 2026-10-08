package com.chavez.store.api_rest.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce excepciones a respuestas HTTP con el formato estandar.
 * Catalogo de codigos en LOGICA_NEGOCIO.md seccion 6.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Excepciones de negocio: cada una trae su codigo y su status HTTP. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorDTO> handleBusiness(BusinessException ex, HttpServletRequest request) {
        log.warn("Negocio [{}]: {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(new ApiErrorDTO(ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    /** Bean Validation fallido en el body del request. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> handleValidation(MethodArgumentNotValidException ex,
                                                         HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            campos.put(error.getField(), error.getDefaultMessage());
        }
        String resumen = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        ApiErrorDTO error = new ApiErrorDTO("VAL_001", resumen, request.getRequestURI());
        error.setDetails(campos);
        return ResponseEntity.badRequest().body(error);
    }

    /** JSON malformado o tipo incompatible. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorDTO> handleUnreadable(HttpMessageNotReadableException ex,
                                                         HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorDTO("VAL_002", "Peticion mal formada o tipo de dato incorrecto",
                        request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDTO> handleAccessDenied(AccessDeniedException ex,
                                                          HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorDTO("AUTH_006", "No tiene permisos para esta operacion",
                        request.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorDTO> handleAuthentication(AuthenticationException ex,
                                                            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorDTO("AUTH_001", "Credenciales invalidas", request.getRequestURI()));
    }

    /** Violacion de integridad referencial o unique. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorDTO> handleIntegrity(DataIntegrityViolationException ex,
                                                       HttpServletRequest request) {
        log.warn("Integridad: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorDTO("DAT_002", "Violacion de integridad: "
                        + ex.getMostSpecificCause().getMessage(), request.getRequestURI()));
    }

    /** Cualquier otra excepcion no controlada. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorDTO("ERR_500", "Error interno del servidor",
                        request.getRequestURI()));
    }
}