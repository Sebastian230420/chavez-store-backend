package com.chavez.store.api_rest.service;

import com.chavez.store.api_rest.dto.request.CambiarPasswordRequestDTO;
import com.chavez.store.api_rest.dto.request.LoginRequestDTO;
import com.chavez.store.api_rest.dto.request.RegistroUsuarioRequestDTO;
import com.chavez.store.api_rest.dto.response.JwtResponseDTO;
import com.chavez.store.api_rest.dto.response.UsuarioResponseDTO;

public interface IAuthService {

    /** Reglas R-A-01 a R-A-07. */
    JwtResponseDTO login(LoginRequestDTO request);

    UsuarioResponseDTO registrar(RegistroUsuarioRequestDTO request);

    UsuarioResponseDTO usuarioActual(String username);

    UsuarioResponseDTO cambiarPassword(String username, CambiarPasswordRequestDTO request);
}