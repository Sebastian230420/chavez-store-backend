package com.chavez.store.api_rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompraRequestDTO {

    @NotNull(message = "El proveedor es obligatorio")
    private Long proveedorId;

    /** NULL o vacio: se autogenera COM-{anio}-{correlativo}. */
    @Size(max = 20, message = "El documento no puede superar 20 caracteres")
    private String document;

    @NotNull(message = "La fecha de emision es obligatoria")
    private LocalDate issueDate;

    private String notes;

    /** Regla R-CO-01: al menos un item. */
    @NotEmpty(message = "La compra debe tener al menos un item")
    @Valid
    private List<DetalleCompraRequestDTO> detalles;
}