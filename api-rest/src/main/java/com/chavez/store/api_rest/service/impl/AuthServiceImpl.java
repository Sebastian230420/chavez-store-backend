package com.chavez.store.api_rest.service.impl;

import com.chavez.store.api_rest.dto.request.CambiarPasswordRequestDTO;
import com.chavez.store.api_rest.dto.request.LoginRequestDTO;
import com.chavez.store.api_rest.dto.request.RegistroUsuarioRequestDTO;
import com.chavez.store.api_rest.dto.response.JwtResponseDTO;
import com.chavez.store.api_rest.dto.response.UsuarioResponseDTO;
import com.chavez.store.api_rest.entity.Role;
import com.chavez.store.api_rest.entity.User;
import com.chavez.store.api_rest.exception.AccountInactiveException;
import com.chavez.store.api_rest.exception.AccountLockedException;
import com.chavez.store.api_rest.exception.InvalidCredentialsException;
import com.chavez.store.api_rest.exception.ReglaNegocioException;
import com.chavez.store.api_rest.exception.ResourceNotFoundException;
import com.chavez.store.api_rest.repository.dao.IRoleDAO;
import com.chavez.store.api_rest.repository.dao.IUserDAO;
import com.chavez.store.api_rest.security.CustomUserDetails;
import com.chavez.store.api_rest.security.JwtTokenProvider;
import com.chavez.store.api_rest.security.UserDetailsServiceImpl;
import com.chavez.store.api_rest.service.IAuthService;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autenticacion y gestion de usuarios.
 * Reglas R-A-01 a R-A-07 (LOGICA_NEGOCIO.md seccion 3.1).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final int MAX_INTENTOS = 5;
    private static final int MINUTOS_BLOQUEO = 15;

    /** R-A-03: 8 caracteres, 1 mayuscula, 1 minuscula, 1 digito. */
    private static final Pattern PASSWORD_FUERTE =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    private final IUserDAO userDAO;
    private final IRoleDAO roleDAO;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    @Transactional
    public JwtResponseDTO login(LoginRequestDTO request) {
        User user = userDAO.findByUsername(request.getUsername())
                .orElseThrow(InvalidCredentialsException::new);

        // R-A-06: cuenta bloqueada por intentos fallidos
        if (user.isLocked()) {
            throw new AccountLockedException(user.getLockedUntil());
        }

        // R-A-05: solo usuarios activos se autentican
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new AccountInactiveException();
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            registrarIntentoFallido(user);
            throw new InvalidCredentialsException();
        }

        // Login exitoso: se limpia el bloqueo
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userDAO.save(user);

        CustomUserDetails details = userDetailsService.toUserDetails(user);
        String token = tokenProvider.generateToken(details);

        return JwtResponseDTO.builder()
                .token(token)
                .expiresIn(tokenProvider.getExpiresInSeconds())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .roles(details.getAuthorities().stream()
                        .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                        .toList())
                .build();
    }

    private void registrarIntentoFallido(User user) {
        int intentos = user.getFailedAttempts() + 1;
        user.setFailedAttempts(intentos);
        if (intentos >= MAX_INTENTOS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
            log.warn("Cuenta bloqueada por {} intentos fallidos: {}",
                    intentos, user.getUsername());
        }
        userDAO.save(user);
    }

    @Override
    @Transactional
    public UsuarioResponseDTO registrar(RegistroUsuarioRequestDTO request) {
        // R-A-01: username y email unicos
        if (userDAO.existsByUsername(request.getUsername())) {
            throw new ReglaNegocioException("El usuario ya existe: " + request.getUsername());
        }
        if (userDAO.existsByEmail(request.getEmail())) {
            throw new ReglaNegocioException("El email ya esta registrado: " + request.getEmail());
        }

        // R-A-03: formato de password
        if (!PASSWORD_FUERTE.matcher(request.getPassword()).matches()) {
            throw new ReglaNegocioException(
                    "El password debe tener al menos 8 caracteres, 1 mayuscula, "
                            + "1 minuscula y 1 digito (R-A-03)");
        }

        // R-A-02: nunca se guarda el password en texto plano
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .active(true)
                .failedAttempts(0)
                .build();

        // Sin roles explicitos: CAJERO es el rol por defecto mas restrictivo
        Set<String> solicitados = request.getRoles() == null || request.getRoles().isEmpty()
                ? Set.of("CAJERO")
                : new HashSet<>(request.getRoles());

        user.getRoles().addAll(resolverRoles(solicitados));
        userDAO.save(user);

        return toResponse(user);
    }

    private Set<Role> resolverRoles(Set<String> nombres) {
        Set<Role> roles = new HashSet<>();
        for (String nombre : nombres) {
            roles.add(roleDAO.findByName(nombre)
                    .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + nombre)));
        }
        return roles;
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO usuarioActual(String username) {
        User user = userDAO.findByUsername(username)
                .orElseThrow(() -> ResourceNotFoundException.porNombre("Usuario", username));
        return toResponse(user);
    }

    @Override
    @Transactional
    public UsuarioResponseDTO cambiarPassword(String username, CambiarPasswordRequestDTO request) {
        User user = userDAO.findByUsername(username)
                .orElseThrow(() -> ResourceNotFoundException.porNombre("Usuario", username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (!PASSWORD_FUERTE.matcher(request.getNewPassword()).matches()) {
            throw new ReglaNegocioException(
                    "El nuevo password debe tener al menos 8 caracteres, 1 mayuscula, "
                            + "1 minuscula y 1 digito (R-A-03)");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userDAO.save(user);
        return toResponse(user);
    }

    private UsuarioResponseDTO toResponse(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();
        return UsuarioResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .active(user.getActive())
                .roles(roles)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}