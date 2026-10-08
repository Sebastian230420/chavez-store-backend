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
public class ProveedorRequestDTO {

    /** NULL o vacio: se autogenera PROV-{correlativo} (regla R-CO-06). */
    @Size(max = 20, message = "El documento no puede superar 20 caracteres")
    private String document;

    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    private String name;

    @Size(max = 30, message = "El telefono no puede superar 30 caracteres")
    private String phone;

    @Size(max = 100, message = "El email no puede superar 100 caracteres")
    private String email;

    @Size(max = 200, message = "La direccion no puede superar 200 caracteres")
    private String address;
}