package com.chavez.store.api_rest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AjusteStockRequestDTO {

    @NotNull(message = "El producto es obligatorio")
    private Long productoId;

    /** Stock final deseado en unidad base. */
    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;

    /** Regla R-I-06: motivo obligatorio y rol ADMIN o SUPERVISOR. */
    @NotBlank(message = "El motivo del ajuste es obligatorio")
    @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
    private String reason;
}