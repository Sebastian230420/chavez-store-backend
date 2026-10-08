package com.chavez.store.api_rest.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Genera y valida tokens JWT con firma HS256.
 * Payload: sub (username), roles, iat, exp. Nunca incluye el password (regla R-A-04).
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = buildKey(secret);
        this.expirationMs = expirationMs;
    }

    /**
     * Acepta el secreto en Base64 o en texto plano.
     * Primero intenta Base64; si no decodifica, usa los bytes del texto.
     * HS256 exige al menos 256 bits: si falta longitud, falla el arranque
     * en lugar de producir un token debil en silencio.
     */
    private SecretKey buildKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret no puede estar vacio");
        }

        // Decoders.BASE64 lanza DecodingException (RuntimeException) si hay caracteres no validos
        try {
            byte[] bytes = Decoders.BASE64.decode(secret.trim());
            if (bytes.length >= 32) {
                return Keys.hmacShaKeyFor(bytes);
            }
        } catch (RuntimeException e) {
            log.debug("El secreto no es Base64 valido, se usara como texto plano: {}", e.getMessage());
        }

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret debe tener al menos 32 bytes (256 bits) para HS256. "
                            + "Configuracion actual: " + bytes.length + " bytes. "
                            + "Generar uno con: openssl rand -base64 48");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public String generateToken(UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .toList();
        Date now = new Date();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    /** @return los claims, o null si el token es invalido o ya expiro */
    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new com.chavez.store.api_rest.exception.TokenExpiredException();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token invalido: {}", e.getMessage());
            throw new com.chavez.store.api_rest.exception.InvalidTokenException();
        }
    }

    public String getUsernameFromToken(String token) {
        return extractClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Object roles = extractClaims(token).get("roles");
        return roles instanceof List ? (List<String>) roles : List.of();
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    /** Segundos hasta la expiracion, para que el frontend sepa cuando renovar. */
    public long getExpiresInSeconds() {
        return expirationMs / 1000;
    }
}