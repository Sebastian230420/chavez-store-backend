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
public class AnularVentaRequestDTO {

    @NotBlank(message = "El motivo de anulacion es obligatorio")
    @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
    private String reason;
}