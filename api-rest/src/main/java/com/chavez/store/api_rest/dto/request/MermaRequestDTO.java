package com.chavez.store.api_rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
public class MermaRequestDTO {

    @NotNull(message = "El producto es obligatorio")
    private Long productoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer qty;

    /** NULL: consume lotes por FEFO. */
    private Long loteId;

    /** Regla R-I-07: motivo obligatorio (VENCIDO, QUIEBRE, ERROR_TOMA, ROBO, OTRO). */
    @NotBlank(message = "El motivo de la merma es obligatorio")
    @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
    private String reason;
}