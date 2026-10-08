package com.chavez.store.api_rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CambiarPasswordRequestDTO {

    @NotBlank(message = "El password actual es obligatorio")
    private String currentPassword;

    @NotBlank(message = "El nuevo password es obligatorio")
    @Size(min = 8, max = 100, message = "El password debe tener entre 8 y 100 caracteres")
    private String newPassword;
}