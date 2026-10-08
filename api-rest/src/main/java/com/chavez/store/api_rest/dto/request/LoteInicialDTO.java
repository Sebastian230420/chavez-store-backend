package com.chavez.store.api_rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteInicialDTO {

    @NotBlank(message = "El codigo de lote es obligatorio")
    @Size(max = 60, message = "El codigo de lote no puede superar 60 caracteres")
    private String lotCode;

    private LocalDate entryDate;

    private LocalDate expiryDate;

    @NotNull(message = "La cantidad del lote es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer qty;

    @NotNull(message = "El costo unitario es obligatorio")
    @DecimalMin(value = "0.0000", message = "El costo no puede ser negativo")
    private BigDecimal costUnit;
}