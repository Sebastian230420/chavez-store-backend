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

/** Carga de inventario de apertura. Crea stock y sus lotes iniciales. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockInicialRequestDTO {

    @NotNull(message = "El stock en unidad base es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer unitsBase;

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
    private String reason;

    @Valid
    private List<LoteInicialDTO> lotes;
}