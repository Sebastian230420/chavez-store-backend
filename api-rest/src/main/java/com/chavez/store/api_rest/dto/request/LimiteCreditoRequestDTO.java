package com.chavez.store.api_rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Regla R-CL-04: solo ADMIN puede modificar el cupo de credito. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimiteCreditoRequestDTO {

    @NotNull(message = "El limite de credito es obligatorio")
    @DecimalMin(value = "0.00", message = "El limite no puede ser negativo")
    private BigDecimal creditLimit;

    @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
    private String reason;
}