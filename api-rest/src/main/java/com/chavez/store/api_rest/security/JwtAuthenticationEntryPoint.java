package com.chavez.store.api_rest.security;

// Spring Boot 4 usa Jackson 3, cuyo paquete es tools.jackson (no com.fasterxml.jackson)
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Devuelve JSON con el formato de error cuando falla la autenticacion o el rol. */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /** Sin token valido. AUTH_001. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws java.io.IOException {
        escribir(response, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_001",
                "Credenciales invalidas: se requiere un token valido", request);
    }

    /** Token valido pero sin el rol requerido. AUTH_006. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws java.io.IOException {
        escribir(response, HttpServletResponse.SC_FORBIDDEN, "AUTH_006",
                "No tiene permisos para esta operacion", request);
    }

    private void escribir(HttpServletResponse response, int status, String code,
                          String message, HttpServletRequest request) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of(
                "code", code,
                "message", message,
                "timestamp", LocalDateTime.now().toString(),
                "path", request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}